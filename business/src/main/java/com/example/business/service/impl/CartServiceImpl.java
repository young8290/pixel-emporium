package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.Cart;
import com.example.business.entity.Product;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.CartAddRequest;
import com.example.business.request.CartUpdateRequest;
import com.example.business.response.CartItemResponse;
import com.example.business.response.CartViewResponse;
import com.example.business.service.CartService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    public CartServiceImpl(CartMapper cartMapper,
                           ProductSkuMapper productSkuMapper,
                           ProductMapper productMapper) {
        this.baseMapper = cartMapper;
        this.productSkuMapper = productSkuMapper;
        this.productMapper = productMapper;
    }

    @Override
    public CartViewResponse getCart() {
        Long userId = requireUserId();
        List<Cart> carts = baseMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByAsc(Cart::getCreatedAt)
                .orderByAsc(Cart::getId));
        CartViewResponse view = new CartViewResponse();
        if (carts.isEmpty()) {
            return view;
        }

        Set<Long> skuIds = carts.stream()
                .map(Cart::getProductSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ProductSku> skuById = skuIds.isEmpty()
                ? Map.of()
                : productSkuMapper.selectByIds(skuIds).stream()
                        .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<Long> productIds = skuById.values().stream()
                .map(ProductSku::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Product> productById = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal checkedAmount = BigDecimal.ZERO;
        int checkedQuantity = 0;
        for (Cart cart : carts) {
            ProductSku sku = skuById.get(cart.getProductSkuId());
            Product product = sku == null ? null : productById.get(sku.getProductId());
            CartItemResponse item = toResponse(cart, sku, product);
            view.getItems().add(item);
            if (Boolean.TRUE.equals(item.getChecked()) && !Boolean.TRUE.equals(item.getInvalid())) {
                checkedAmount = checkedAmount.add(item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())));
                checkedQuantity += item.getQuantity();
            }
        }
        view.setCheckedAmount(checkedAmount);
        view.setCheckedQuantity(checkedQuantity);
        return view;
    }

    @Override
    @Transactional
    public boolean add(CartAddRequest request) {
        Long userId = requireUserId();
        validateAddRequest(request);
        ProductSku sku = loadAvailableSku(request.getProductSkuId());
        Cart existing = baseMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductSkuId, request.getProductSkuId()));
        int existingQuantity = existing == null || existing.getQuantity() == null
                ? 0 : existing.getQuantity();
        long total = (long) existingQuantity + request.getQuantity();
        if (total > sku.getStock()) {
            throw new CustomValidationException("购物车数量不能超过库存");
        }

        if (existing == null) {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductSkuId(request.getProductSkuId());
            cart.setQuantity(request.getQuantity());
            cart.setChecked(Boolean.TRUE);
            if (baseMapper.insert(cart) <= 0) {
                throw new CustomBusinessException("加入购物车失败");
            }
            return true;
        }
        existing.setQuantity((int) total);
        if (baseMapper.updateById(existing) <= 0) {
            throw new CustomBusinessException("购物车更新失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean update(CartUpdateRequest request) {
        Long userId = requireUserId();
        if (request == null || request.getId() == null) {
            throw new CustomValidationException("购物车项ID不能为空");
        }
        if (request.getQuantity() == null && request.getChecked() == null) {
            throw new CustomValidationException("至少提供数量或勾选状态");
        }
        if (request.getQuantity() != null && request.getQuantity() <= 0) {
            throw new CustomValidationException("数量必须大于0");
        }
        Cart cart = ownedCart(userId, request.getId());
        if (request.getQuantity() != null) {
            ProductSku sku = loadAvailableSku(cart.getProductSkuId());
            if (request.getQuantity() > sku.getStock()) {
                throw new CustomValidationException("购物车数量不能超过库存");
            }
            cart.setQuantity(request.getQuantity());
        }
        if (request.getChecked() != null) {
            cart.setChecked(request.getChecked());
        }
        if (baseMapper.updateById(cart) <= 0) {
            throw new CustomBusinessException("购物车更新失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        Long userId = requireUserId();
        ownedCart(userId, id);
        if (baseMapper.logicalDeleteOwned(userId, id) != 1) {
            throw new CustomBusinessException("购物车删除失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteBatch(List<Long> ids) {
        Long userId = requireUserId();
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new CustomValidationException("购物车项ID列表不能为空");
        }
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<Cart> owned = baseMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, uniqueIds));
        Set<Long> ownedIds = owned.stream()
                .map(Cart::getId)
                .collect(Collectors.toSet());
        if (ownedIds.size() != uniqueIds.size() || !ownedIds.containsAll(uniqueIds)) {
            throw new CustomValidationException("购物车项不存在或无权操作");
        }
        if (baseMapper.logicalDeleteBatch(userId, uniqueIds) != uniqueIds.size()) {
            throw new CustomBusinessException("购物车批量删除失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean clear() {
        baseMapper.logicalDeleteAll(requireUserId());
        return true;
    }

    private void validateAddRequest(CartAddRequest request) {
        if (request == null || request.getProductSkuId() == null) {
            throw new CustomValidationException("SKU ID不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new CustomValidationException("数量必须大于0");
        }
    }

    private ProductSku loadAvailableSku(Long productSkuId) {
        ProductSku sku = productSkuMapper.selectById(productSkuId);
        if (sku == null || sku.getProductId() == null) {
            throw new CustomBusinessException("商品SKU不存在");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null) {
            throw new CustomBusinessException("商品不存在");
        }
        if (!Integer.valueOf(1).equals(product.getOnSale())) {
            throw new CustomBusinessException("商品已下架");
        }
        if (sku.getStock() == null || sku.getStock() < 0) {
            throw new CustomBusinessException("商品库存不可用");
        }
        return sku;
    }

    private Cart ownedCart(Long userId, Long id) {
        if (id == null) {
            throw new CustomValidationException("购物车项ID不能为空");
        }
        Cart cart = baseMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getId, id)
                .eq(Cart::getUserId, userId));
        if (cart == null) {
            throw new CustomValidationException("购物车项不存在");
        }
        return cart;
    }

    private CartItemResponse toResponse(Cart cart, ProductSku sku, Product product) {
        CartItemResponse response = new CartItemResponse();
        response.setId(cart.getId());
        response.setProductSkuId(cart.getProductSkuId());
        response.setQuantity(cart.getQuantity());
        response.setChecked(Boolean.TRUE.equals(cart.getChecked()));
        if (sku != null) {
            response.setProductId(sku.getProductId());
            response.setSkuTitle(sku.getTitle());
            response.setPrice(sku.getPrice());
            response.setStock(sku.getStock());
        }
        if (product != null) {
            response.setProductTitle(product.getTitle());
            response.setProductImage(product.getImage());
        }
        boolean invalid = sku == null
                || product == null
                || !Integer.valueOf(1).equals(product.getOnSale())
                || cart.getQuantity() == null
                || cart.getQuantity() <= 0
                || sku.getStock() == null
                || sku.getStock() < cart.getQuantity()
                || sku.getPrice() == null;
        response.setInvalid(invalid);
        return response;
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomValidationException("请先登录");
        }
        return userId;
    }
}
