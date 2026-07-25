package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.Product;
import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.ProductSku;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductSkuAttributeRequest;
import com.example.business.request.ProductSkuGenerateRequest;
import com.example.business.request.ProductSkuListRequest;
import com.example.business.request.ProductSkuRequest;
import com.example.business.response.ProductSkuGenerateResponse;
import com.example.business.response.ProductSkuResponse;
import com.example.business.service.ProductSkuService;
import com.example.business.service.StockLogService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku>
        implements ProductSkuService {
    private static final int MAX_GENERATED_COMBINATIONS = 200;

    private final ProductAttributeMapper attributeMapper;
    private final ProductAttributeValueMapper valueMapper;
    private final SkuAttributeValueMapper mappingMapper;
    private final ProductMapper productMapper;
    private final StockLogService stockLogService;

    public ProductSkuServiceImpl(ProductSkuMapper skuMapper,
                                 ProductAttributeMapper attributeMapper,
                                 ProductAttributeValueMapper valueMapper,
                                 SkuAttributeValueMapper mappingMapper,
                                 ProductMapper productMapper,
                                 StockLogService stockLogService) {
        this.baseMapper = skuMapper;
        this.attributeMapper = attributeMapper;
        this.valueMapper = valueMapper;
        this.mappingMapper = mappingMapper;
        this.productMapper = productMapper;
        this.stockLogService = stockLogService;
    }

    @Override
    @Transactional
    public void deductStock(Long id, Integer qty) {
        deductStock(id, qty, null, null);
    }

    @Override
    @Transactional
    public void deductStock(Long id, Integer qty, String bizNo, Long operatorId) {
        validatePositiveQty(qty);
        // 幂等：同一 bizNo+SKU+DEDUCT 已扣减过则跳过（防重复提交/重复回调）
        if (stockLogService.exists(bizNo, id, "DEDUCT")) {
            return;
        }
        ProductSku before = baseMapper.selectById(id);
        if (before == null) {
            throw new CustomBusinessException("库存不足或商品不存在");
        }
        int beforeStock = before.getStock();
        if (baseMapper.deductStock(id, qty) == 0) {
            throw new CustomBusinessException("库存不足或商品不存在");
        }
        stockLogService.record(id, -qty, beforeStock, beforeStock - qty,
                "DEDUCT", bizNo, operatorId, null);
    }

    @Override
    @Transactional
    public void restoreStock(Long id, Integer qty) {
        restoreStock(id, qty, null, null);
    }

    @Override
    @Transactional
    public void restoreStock(Long id, Integer qty, String bizNo, Long operatorId) {
        validatePositiveQty(qty);
        ProductSku before = baseMapper.selectById(id);
        if (before == null) {
            throw new CustomBusinessException("商品不存在");
        }
        int beforeStock = before.getStock();
        if (baseMapper.restoreStock(id, qty) == 0) {
            throw new CustomBusinessException("商品不存在");
        }
        stockLogService.record(id, qty, beforeStock, beforeStock + qty,
                "RESTORE", bizNo, operatorId, null);
    }

    @Override
    @Transactional
    public boolean saveSku(ProductSkuRequest request) {
        normalizePrices(request);
        validateSkuRequest(request);
        ensureProductExists(request.getProductId());
        ProductSku sku = request.getId() == null
                ? new ProductSku()
                : ownedSku(request.getProductId(), request.getId());

        validateSkuCode(request.getProductId(), request.getSkuCode(), request.getId());
        List<SkuAttributeValue> newMappings = validateCombination(
                request.getProductId(), request.getAttributeValueIds());
        rejectDuplicateCombination(request.getProductId(), request.getId(), newMappings);
        // 记录旧库存，更新后若发生变化则写 ADMIN_ADJUST 流水（BE-P2-03）
        boolean isUpdate = sku.getId() != null;
        Integer oldStock = isUpdate ? sku.getStock() : null;
        BeanUtil.copyProperties(request, sku);
        sku.setProductId(request.getProductId());
        if (sku.getId() == null) {
            if (baseMapper.insert(sku) <= 0) {
                throw new CustomBusinessException("SKU保存失败");
            }
        } else if (baseMapper.updateById(sku) <= 0) {
            throw new NoSuchElementException("商品SKU不存在");
        }
        if (isUpdate && sku.getStock() != null && oldStock != null
                && !oldStock.equals(sku.getStock())) {
            String adjustReason = request.getStockAdjustReason();
            if (adjustReason == null || adjustReason.isBlank()) {
                adjustReason = "管理员编辑SKU调整库存";
            }
            stockLogService.record(sku.getId(), sku.getStock() - oldStock,
                    oldStock, sku.getStock(), "ADMIN_ADJUST", null,
                    UserContext.get(), adjustReason);
        }
        syncMappings(request.getProductId(), sku.getId(), newMappings);
        return true;
    }

    @Override
    public PageResponse<ProductSkuResponse> getProductSkuPage(ProductSkuListRequest request) {
        return executePageQuery(request, buildQuery(request));
    }

    @Override
    public PageResponse<ProductSkuResponse> getAdminProductSkuPage(ProductSkuListRequest request) {
        return executePageQuery(request, buildQuery(request));
    }

    @Override
    @Transactional
    public boolean adminAdd(ProductSkuRequest request) {
        return saveSku(request);
    }

    @Override
    @Transactional
    public boolean adminUpdate(ProductSkuRequest request) {
        if (request.getId() == null) {
            throw new CustomValidationException("SKU ID不能为空");
        }
        return saveSku(request);
    }

    @Override
    @Transactional
    public boolean deleteSku(Long productId, Long id) {
        if (productId == null || id == null) {
            throw new CustomValidationException("商品ID和SKU ID不能为空");
        }
        ProductSku sku = ownedSku(productId, id);
        mappingMapper.delete(new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId)
                .eq(SkuAttributeValue::getSkuId, id));
        return baseMapper.deleteById(sku.getId()) > 0;
    }

    @Override
    @Transactional
    public boolean adminDeleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        mappingMapper.delete(new LambdaQueryWrapper<SkuAttributeValue>()
                .in(SkuAttributeValue::getSkuId, ids));
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional
    public boolean deleteSkuBatch(Long productId, List<Long> ids) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            ownedSku(productId, id);
        }
        mappingMapper.delete(new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId)
                .in(SkuAttributeValue::getSkuId, ids));
        return baseMapper.delete(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .in(ProductSku::getId, ids)) > 0;
    }

    @Override
    @Transactional
    public ProductSkuGenerateResponse generate(ProductSkuGenerateRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        if (request.getDefaultPrice() == null || request.getDefaultPrice().signum() < 0) {
            throw new CustomValidationException("默认价格不能小于0");
        }
        if (request.getDefaultStock() == null || request.getDefaultStock() < 0) {
            throw new CustomValidationException("默认库存不能小于0");
        }
        ensureProductExists(request.getProductId());

        List<ProductAttribute> attributes = loadAttributes(request.getProductId());
        ProductSkuGenerateResponse response = new ProductSkuGenerateResponse();
        if (attributes.isEmpty()) {
            if (request.getSelections() != null && !request.getSelections().isEmpty()) {
                throw new CustomValidationException("无规格商品不能提交规格组合");
            }
            List<ProductSku> existing = baseMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, request.getProductId()));
            if (!existing.isEmpty()) {
                response.setReusedCount(1);
                response.getSkuIds().add(existing.getFirst().getId());
                return response;
            }
            ProductSku sku = createGeneratedSku(request.getProductId(), request.getDefaultPrice(),
                    request.getDefaultStock(), List.of());
            response.setGeneratedCount(1);
            response.getSkuIds().add(sku.getId());
            return response;
        }

        List<List<ProductAttributeValue>> dimensions = validateSelections(request.getProductId(), attributes,
                request.getSelections());
        long combinationCount = 1;
        for (List<ProductAttributeValue> dimension : dimensions) {
            combinationCount *= dimension.size();
            if (combinationCount > MAX_GENERATED_COMBINATIONS) {
                throw new CustomValidationException("SKU组合数量不能超过" + MAX_GENERATED_COMBINATIONS);
            }
        }

        Map<String, Long> existing = existingCombinationIds(request.getProductId());
        List<List<ProductAttributeValue>> combinations = new ArrayList<>();
        cartesian(dimensions, 0, new ArrayList<>(), combinations);
        for (List<ProductAttributeValue> combination : combinations) {
            List<SkuAttributeValue> mappings = toMappings(request.getProductId(), null, combination);
            String key = ProductSkuCombinationSupport.key(mappings);
            Long existingId = existing.get(key);
            if (existingId != null) {
                response.setReusedCount(response.getReusedCount() + 1);
                response.getSkuIds().add(existingId);
                continue;
            }
            ProductSku sku = createGeneratedSku(request.getProductId(), request.getDefaultPrice(),
                    request.getDefaultStock(), mappings);
            existing.put(key, sku.getId());
            response.setGeneratedCount(response.getGeneratedCount() + 1);
            response.getSkuIds().add(sku.getId());
        }
        return response;
    }

    private ProductSku createGeneratedSku(Long productId, BigDecimal price, Integer stock,
                                           List<SkuAttributeValue> mappings) {
        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setTitle(mappings.isEmpty() ? "默认规格" : "自动生成SKU");
        if (baseMapper.insert(sku) <= 0) {
            throw new CustomBusinessException("SKU生成失败");
        }
        for (SkuAttributeValue mapping : mappings) {
            mapping.setSkuId(sku.getId());
            if (mappingMapper.insert(mapping) <= 0) {
                throw new CustomBusinessException("SKU规格组合生成失败");
            }
        }
        return sku;
    }

    private List<List<ProductAttributeValue>> validateSelections(Long productId,
                                                                   List<ProductAttribute> attributes,
                                                                   List<ProductSkuAttributeRequest> selections) {
        if (selections == null || selections.size() != attributes.size()) {
            throw new CustomValidationException("每个规格属性必须恰好选择一组规格值");
        }
        Map<Long, ProductSkuAttributeRequest> requestByAttribute = new HashMap<>();
        for (ProductSkuAttributeRequest selection : selections) {
            if (selection == null || selection.getAttributeId() == null
                    || requestByAttribute.put(selection.getAttributeId(), selection) != null) {
                throw new CustomValidationException("规格属性选择不能重复或为空");
            }
        }
        Map<Long, ProductAttribute> attributeById = attributes.stream()
                .collect(Collectors.toMap(ProductAttribute::getId, attribute -> attribute));
        if (!requestByAttribute.keySet().equals(attributeById.keySet())) {
            throw new CustomValidationException("规格属性选择必须覆盖商品全部规格");
        }
        long requestedCombinationCount = 1;
        for (ProductSkuAttributeRequest selection : selections) {
            List<Long> requestedIds = selection.getAttributeValueIds();
            if (requestedIds == null || requestedIds.isEmpty()) {
                throw new CustomValidationException("规格属性必须至少选择一个值");
            }
            requestedCombinationCount *= new LinkedHashSet<>(requestedIds).size();
            if (requestedCombinationCount > MAX_GENERATED_COMBINATIONS) {
                throw new CustomValidationException("SKU组合数量不能超过" + MAX_GENERATED_COMBINATIONS);
            }
        }
        Map<Long, ProductAttributeValue> values = loadValues(productId).stream()
                .collect(Collectors.toMap(ProductAttributeValue::getId, value -> value));
        List<List<ProductAttributeValue>> dimensions = new ArrayList<>();
        for (ProductAttribute attribute : attributes) {
            List<Long> requestedIds = requestByAttribute.get(attribute.getId()).getAttributeValueIds();
            if (requestedIds == null || requestedIds.isEmpty()) {
                throw new CustomValidationException("规格属性必须至少选择一个值");
            }
            Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
            List<ProductAttributeValue> dimension = new ArrayList<>();
            for (Long valueId : uniqueIds) {
                ProductAttributeValue value = values.get(valueId);
                if (value == null || !productId.equals(value.getProductId())
                        || !attribute.getId().equals(value.getAttributeId())) {
                    throw new CustomValidationException("规格值不属于当前商品或规格属性");
                }
                dimension.add(value);
            }
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private List<SkuAttributeValue> validateCombination(Long productId, List<Long> valueIds) {
        List<ProductAttribute> attributes = loadAttributes(productId);
        List<ProductAttributeValue> values = loadValues(productId);
        Map<Long, ProductAttributeValue> valueById = values.stream()
                .collect(Collectors.toMap(ProductAttributeValue::getId, value -> value));
        List<Long> requested = valueIds == null ? List.of() : valueIds;
        if (new HashSet<>(requested).size() != requested.size()) {
            throw new CustomValidationException("SKU规格值不能重复");
        }
        Map<Long, ProductAttributeValue> selectedByAttribute = new LinkedHashMap<>();
        for (Long valueId : requested) {
            ProductAttributeValue value = valueById.get(valueId);
            if (value == null || !productId.equals(value.getProductId())) {
                throw new CustomValidationException("规格值不属于当前商品");
            }
            if (selectedByAttribute.put(value.getAttributeId(), value) != null) {
                throw new CustomValidationException("同一规格属性只能选择一个值");
            }
        }
        if (selectedByAttribute.size() != attributes.size()
                || !selectedByAttribute.keySet().containsAll(attributes.stream()
                .map(ProductAttribute::getId).toList())) {
            if (!attributes.isEmpty()) {
                throw new CustomValidationException("SKU组合必须覆盖商品全部规格属性");
            }
            if (!requested.isEmpty()) {
                throw new CustomValidationException("无规格商品不能绑定规格值");
            }
        }
        return toMappings(productId, null, new ArrayList<>(selectedByAttribute.values()));
    }

    private void rejectDuplicateCombination(Long productId, Long ignoredSkuId,
                                            List<SkuAttributeValue> mappings) {
        if (mappings.isEmpty()) {
            boolean existingDefaultSku = baseMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                            .eq(ProductSku::getProductId, productId)
                            .ne(ignoredSkuId != null, ProductSku::getId, ignoredSkuId))
                    .stream()
                    .anyMatch(sku -> !Objects.equals(sku.getId(), ignoredSkuId));
            if (existingDefaultSku) {
                throw new CustomValidationException("无规格商品只能有一个默认SKU");
            }
            return;
        }
        String newKey = ProductSkuCombinationSupport.key(mappings);
        Map<Long, List<SkuAttributeValue>> grouped = ProductSkuCombinationSupport.bySku(
                activeMappings(productId));
        for (Map.Entry<Long, List<SkuAttributeValue>> entry : grouped.entrySet()) {
            if (ignoredSkuId != null && ignoredSkuId.equals(entry.getKey())) {
                continue;
            }
            if (newKey.equals(ProductSkuCombinationSupport.key(entry.getValue()))) {
                throw new CustomValidationException("同一商品内SKU规格组合不能重复");
            }
        }
    }

    private Map<String, Long> existingCombinationIds(Long productId) {
        Map<String, Long> result = new HashMap<>();
        ProductSkuCombinationSupport.bySku(activeMappings(productId)).forEach((skuId, mappings) ->
                result.put(ProductSkuCombinationSupport.key(mappings), skuId));
        return result;
    }

    private List<SkuAttributeValue> activeMappings(Long productId) {
        return mappingMapper.selectList(new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId));
    }

    private List<SkuAttributeValue> toMappings(Long productId, Long skuId,
                                               List<ProductAttributeValue> values) {
        return values.stream().map(value -> {
            SkuAttributeValue mapping = new SkuAttributeValue();
            mapping.setProductId(productId);
            mapping.setSkuId(skuId);
            mapping.setAttributeId(value.getAttributeId());
            mapping.setAttributeValueId(value.getId());
            return mapping;
        }).toList();
    }

    private void cartesian(List<List<ProductAttributeValue>> dimensions, int index,
                           List<ProductAttributeValue> current,
                           List<List<ProductAttributeValue>> result) {
        if (index == dimensions.size()) {
            result.add(List.copyOf(current));
            return;
        }
        for (ProductAttributeValue value : dimensions.get(index)) {
            current.add(value);
            cartesian(dimensions, index + 1, current, result);
            current.removeLast();
        }
    }

    private List<ProductAttribute> loadAttributes(Long productId) {
        return attributeMapper.selectList(new LambdaQueryWrapper<ProductAttribute>()
                        .eq(ProductAttribute::getProductId, productId)
                        .orderByAsc(ProductAttribute::getSort)
                        .orderByAsc(ProductAttribute::getId))
                .stream()
                .sorted(Comparator.comparing(ProductAttribute::getSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductAttribute::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private void syncMappings(Long productId, Long skuId, List<SkuAttributeValue> requestedMappings) {
        List<SkuAttributeValue> existingMappings = mappingMapper.selectList(
                new LambdaQueryWrapper<SkuAttributeValue>()
                        .eq(SkuAttributeValue::getProductId, productId)
                        .eq(SkuAttributeValue::getSkuId, skuId))
                .stream()
                .filter(mapping -> Objects.equals(mapping.getSkuId(), skuId))
                .toList();
        Map<Long, SkuAttributeValue> existingByAttribute = existingMappings.stream()
                .collect(Collectors.toMap(SkuAttributeValue::getAttributeId, mapping -> mapping,
                        (left, right) -> left));
        Set<Long> requestedAttributeIds = new HashSet<>();
        for (SkuAttributeValue requested : requestedMappings) {
            requested.setProductId(productId);
            requested.setSkuId(skuId);
            requestedAttributeIds.add(requested.getAttributeId());
            SkuAttributeValue existing = existingByAttribute.get(requested.getAttributeId());
            if (existing == null) {
                if (mappingMapper.insert(requested) <= 0) {
                    throw new CustomBusinessException("SKU规格组合保存失败");
                }
                continue;
            }
            existing.setAttributeValueId(requested.getAttributeValueId());
            if (mappingMapper.updateById(existing) <= 0) {
                throw new CustomBusinessException("SKU规格组合更新失败");
            }
        }
        for (SkuAttributeValue existing : existingMappings) {
            if (!requestedAttributeIds.contains(existing.getAttributeId())
                    && mappingMapper.deleteById(existing.getId()) <= 0) {
                throw new CustomBusinessException("SKU规格组合删除失败");
            }
        }
    }

    private List<ProductAttributeValue> loadValues(Long productId) {
        return valueMapper.selectList(new LambdaQueryWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId));
    }

    private void validateSkuRequest(ProductSkuRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw new CustomValidationException("价格不能小于0");
        }
        if (request.getOriginalPrice() != null
                && request.getOriginalPrice().compareTo(request.getPrice()) <= 0) {
            throw new CustomValidationException("原价必须高于现价");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new CustomValidationException("库存不能小于0");
        }
    }

    private void normalizePrices(ProductSkuRequest request) {
        if (request == null) {
            return;
        }
        if (request.getPrice() != null) {
            request.setPrice(request.getPrice().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getOriginalPrice() != null) {
            request.setOriginalPrice(request.getOriginalPrice().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void validateSkuCode(Long productId, String skuCode, Long id) {
        if (!StringUtils.hasText(skuCode)) {
            return;
        }
        long duplicate = baseMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getSkuCode, skuCode.trim())
                .ne(id != null, ProductSku::getId, id));
        if (duplicate > 0) {
            throw new CustomValidationException("同一商品内SKU编码不能重复");
        }
    }

    private ProductSku ownedSku(Long productId, Long id) {
        ProductSku sku = baseMapper.selectById(id);
        if (sku == null) {
            throw new NoSuchElementException("商品SKU不存在");
        }
        if (!productId.equals(sku.getProductId())) {
            throw new CustomValidationException("SKU不属于当前商品");
        }
        return sku;
    }

    private void ensureProductExists(Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new NoSuchElementException("商品不存在");
        }
    }

    private LambdaQueryWrapper<ProductSku> buildQuery(ProductSkuListRequest request) {
        LambdaQueryWrapper<ProductSku> query = new LambdaQueryWrapper<>();
        query.eq(request.getProductId() != null, ProductSku::getProductId, request.getProductId());
        if (StringUtils.hasText(request.getKeyword())) {
            query.like(ProductSku::getTitle, request.getKeyword().trim());
        }
        return query.orderByDesc(ProductSku::getCreatedAt);
    }

    private PageResponse<ProductSkuResponse> executePageQuery(ProductSkuListRequest request,
                                                               LambdaQueryWrapper<ProductSku> query) {
        Page<ProductSku> page = this.page(Page.of(request.getPage(), request.getSize()), query);
        List<ProductSkuResponse> list = BeanUtil.copyToList(page.getRecords(), ProductSkuResponse.class);
        if (!list.isEmpty()) {
            Map<Long, ProductSkuResponse> responseById = list.stream()
                    .collect(Collectors.toMap(ProductSkuResponse::getId, response -> response));
            List<SkuAttributeValue> mappings = mappingMapper.selectList(new LambdaQueryWrapper<SkuAttributeValue>()
                    .in(SkuAttributeValue::getSkuId, responseById.keySet()));
            Map<Long, List<Long>> idsBySku = mappings.stream().collect(Collectors.groupingBy(
                    SkuAttributeValue::getSkuId,
                    Collectors.mapping(SkuAttributeValue::getAttributeValueId, Collectors.toList())));
            responseById.forEach((id, response) -> response.setAttributeValueIds(
                    idsBySku.getOrDefault(id, List.of())));

            List<Long> productIds = list.stream().map(ProductSkuResponse::getProductId).distinct().toList();
            Map<Long, String> titles = productMapper.selectByIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Product::getTitle, (left, right) -> left));
            list.forEach(response -> response.setProductTitle(titles.get(response.getProductId())));
        }
        PageResponse<ProductSkuResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(list);
        return response;
    }

    private void validatePositiveQty(Integer qty) {
        if (qty == null || qty <= 0) {
            throw new CustomValidationException("库存数量必须大于0");
        }
    }
}
