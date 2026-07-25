package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductAttributeRequest;
import com.example.business.request.ProductAttributeValueRequest;
import com.example.business.response.ProductAttributeResponse;
import com.example.business.service.ProductAttributeService;
import com.example.common.exception.CustomValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ProductAttributeServiceImpl
        extends ServiceImpl<ProductAttributeMapper, ProductAttribute>
        implements ProductAttributeService {
    private final ProductAttributeValueMapper valueMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final SkuAttributeValueMapper mappingMapper;

    public ProductAttributeServiceImpl(ProductAttributeMapper attributeMapper,
                                       ProductAttributeValueMapper valueMapper,
                                       ProductMapper productMapper) {
        this(attributeMapper, valueMapper, productMapper, null, null);
    }

    @Autowired
    public ProductAttributeServiceImpl(ProductAttributeMapper attributeMapper,
                                       ProductAttributeValueMapper valueMapper,
                                       ProductMapper productMapper,
                                       ProductSkuMapper skuMapper,
                                       SkuAttributeValueMapper mappingMapper) {
        this.baseMapper = attributeMapper;
        this.valueMapper = valueMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.mappingMapper = mappingMapper;
    }

    @Override
    public List<ProductAttributeResponse> listTree(Long productId) {
        requireProductId(productId);
        List<ProductAttribute> attributes = baseMapper.selectList(new LambdaQueryWrapper<ProductAttribute>()
                        .eq(ProductAttribute::getProductId, productId)
                        .orderByAsc(ProductAttribute::getSort)
                        .orderByAsc(ProductAttribute::getId))
                .stream()
                .sorted(Comparator.comparing(ProductAttribute::getSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductAttribute::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        Map<Long, ProductAttributeResponse> byId = new LinkedHashMap<>();
        attributes.forEach(attribute -> byId.put(attribute.getId(), toResponse(attribute)));
        if (byId.isEmpty()) {
            return List.of();
        }

        List<ProductAttributeValue> values = valueMapper.selectList(
                        new LambdaQueryWrapper<ProductAttributeValue>()
                                .eq(ProductAttributeValue::getProductId, productId)
                                .in(ProductAttributeValue::getAttributeId, byId.keySet())
                                .orderByAsc(ProductAttributeValue::getSort)
                                .orderByAsc(ProductAttributeValue::getId))
                .stream()
                .sorted(Comparator.comparing(ProductAttributeValue::getSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductAttributeValue::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        values.forEach(value -> {
            ProductAttributeResponse parent = byId.get(value.getAttributeId());
            if (parent != null) {
                parent.getValues().add(toValueResponse(value));
            }
        });
        return new ArrayList<>(byId.values());
    }

    @Override
    @Transactional
    public boolean saveAttribute(ProductAttributeRequest request) {
        requireProductId(request.getProductId());
        if (!StringUtils.hasText(request.getName())) {
            throw new CustomValidationException("规格名称不能为空");
        }
        String name = request.getName().trim();
        long duplicates = baseMapper.selectCount(new LambdaQueryWrapper<ProductAttribute>()
                .eq(ProductAttribute::getProductId, request.getProductId())
                .eq(ProductAttribute::getName, name)
                .ne(request.getId() != null, ProductAttribute::getId, request.getId()));
        if (duplicates > 0) {
            throw new CustomValidationException("同一商品下规格名称不能重复");
        }
        ensureProductExists(request.getProductId());

        ProductAttribute attribute = request.getId() == null
                ? new ProductAttribute()
                : ownedAttribute(request.getProductId(), request.getId());
        attribute.setProductId(request.getProductId());
        attribute.setName(name);
        attribute.setSort(request.getSort() == null ? 99 : request.getSort());
        return request.getId() == null
                ? baseMapper.insert(attribute) > 0
                : baseMapper.updateById(attribute) > 0;
    }

    @Override
    @Transactional
    public boolean deleteAttribute(Long productId, Long id) {
        ProductAttribute attribute = ownedAttribute(productId, id);
        cascadeDeleteReferencedSkus(productId, new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId)
                .eq(SkuAttributeValue::getAttributeId, attribute.getId()));
        valueMapper.delete(new LambdaQueryWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId)
                .eq(ProductAttributeValue::getAttributeId, attribute.getId()));
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean saveValue(ProductAttributeValueRequest request) {
        requireProductId(request.getProductId());
        ProductAttribute attribute = ownedAttribute(request.getProductId(), request.getAttributeId());
        if (!StringUtils.hasText(request.getValue())) {
            throw new CustomValidationException("规格值不能为空");
        }
        String valueText = request.getValue().trim();
        long duplicates = valueMapper.selectCount(new LambdaQueryWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, request.getProductId())
                .eq(ProductAttributeValue::getAttributeId, attribute.getId())
                .eq(ProductAttributeValue::getValue, valueText)
                .ne(request.getId() != null, ProductAttributeValue::getId, request.getId()));
        if (duplicates > 0) {
            throw new CustomValidationException("同一规格下规格值不能重复");
        }

        ProductAttributeValue value = request.getId() == null
                ? new ProductAttributeValue()
                : ownedValue(request.getProductId(), request.getId());
        value.setProductId(request.getProductId());
        value.setAttributeId(attribute.getId());
        value.setValue(valueText);
        value.setSort(request.getSort() == null ? 99 : request.getSort());
        return request.getId() == null
                ? valueMapper.insert(value) > 0
                : valueMapper.updateById(value) > 0;
    }

    @Override
    @Transactional
    public boolean deleteValue(Long productId, Long id) {
        ProductAttributeValue value = ownedValue(productId, id);
        cascadeDeleteReferencedSkus(productId, new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId)
                .eq(SkuAttributeValue::getAttributeValueId, value.getId()));
        return valueMapper.deleteById(id) > 0;
    }

    /**
     * 删除规格后下架依赖它的 SKU，避免公开详情返回无法选择的孤立组合。
     */
    private void cascadeDeleteReferencedSkus(Long productId,
                                             LambdaQueryWrapper<SkuAttributeValue> query) {
        if (skuMapper == null || mappingMapper == null) {
            return;
        }
        List<Long> skuIds = mappingMapper.selectList(query).stream()
                .map(SkuAttributeValue::getSkuId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (skuIds.isEmpty()) {
            return;
        }
        mappingMapper.delete(new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId)
                .in(SkuAttributeValue::getSkuId, skuIds));
        skuMapper.deleteByIds(skuIds);
    }

    private ProductAttribute ownedAttribute(Long productId, Long id) {
        requireProductId(productId);
        ProductAttribute attribute = baseMapper.selectById(id);
        if (attribute == null) {
            throw new NoSuchElementException("商品规格不存在");
        }
        if (!productId.equals(attribute.getProductId())) {
            throw new CustomValidationException("规格不属于当前商品");
        }
        return attribute;
    }

    private ProductAttributeValue ownedValue(Long productId, Long id) {
        requireProductId(productId);
        ProductAttributeValue value = valueMapper.selectById(id);
        if (value == null) {
            throw new NoSuchElementException("商品规格值不存在");
        }
        if (!productId.equals(value.getProductId())) {
            throw new CustomValidationException("规格值不属于当前商品");
        }
        return value;
    }

    private void ensureProductExists(Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new NoSuchElementException("商品不存在");
        }
    }

    private void requireProductId(Long productId) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
    }

    private ProductAttributeResponse toResponse(ProductAttribute attribute) {
        return BeanUtil.copyProperties(attribute, ProductAttributeResponse.class);
    }

    private ProductAttributeResponse.ValueResponse toValueResponse(ProductAttributeValue value) {
        return BeanUtil.copyProperties(value, ProductAttributeResponse.ValueResponse.class);
    }
}
