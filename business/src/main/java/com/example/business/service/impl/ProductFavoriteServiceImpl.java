package com.example.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Product;
import com.example.business.entity.ProductFavorite;
import com.example.business.mapper.ProductFavoriteMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.response.ProductResponse;
import com.example.business.service.ProductFavoriteService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductFavoriteServiceImpl implements ProductFavoriteService {
    private final ProductFavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    public ProductFavoriteServiceImpl(ProductFavoriteMapper favoriteMapper,
                                      ProductMapper productMapper) {
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResponse<ProductResponse> list(PageRequest request) {
        Long userId = requireUserId();
        PageRequest pageRequest = request == null ? new PageRequest() : request;
        Page<ProductResponse> page = favoriteMapper.selectProductPage(
                Page.of(pageRequest.getPage(), pageRequest.getSize()), userId);
        PageResponse<ProductResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords());
        return response;
    }

    @Override
    @Transactional
    public boolean add(Long productId) {
        Long userId = requireUserId();
        requirePublicProduct(productId);
        if (favoriteMapper.existsActive(userId, productId)) {
            return true;
        }
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            if (favoriteMapper.insert(favorite) <= 0) {
                throw new CustomBusinessException("收藏失败");
            }
        } catch (DuplicateKeyException ignored) {
            return true;
        }
        return true;
    }

    @Override
    @Transactional
    public boolean remove(Long productId) {
        validateProductId(productId);
        favoriteMapper.logicalDeleteActive(requireUserId(), productId);
        return true;
    }

    @Override
    public boolean isFavorite(Long productId) {
        validateProductId(productId);
        return favoriteMapper.existsActive(requireUserId(), productId);
    }

    private void requirePublicProduct(Long productId) {
        validateProductId(productId);
        Product product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getOnSale())) {
            throw new CustomBusinessException("商品不存在或已下架");
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomValidationException("请先登录");
        }
        return userId;
    }
}
