package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.Category;
import com.example.business.entity.Product;
import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.ProductImage;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductImageMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductListRequest;
import com.example.business.request.ProductPublicListRequest;
import com.example.business.request.ProductRequest;
import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductResponse;
import com.example.business.response.ProductImageResponse;
import com.example.business.service.ProductService;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    private static final String MIN_ACTIVE_SKU_PRICE_SQL =
            "(SELECT MIN(ps.price) FROM product_sku ps WHERE ps.product_id = product.id AND ps.deleted = 0)";

    private final CategoryMapper categoryMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductAttributeMapper attributeMapper;
    private final ProductAttributeValueMapper valueMapper;
    private final SkuAttributeValueMapper mappingMapper;
    private final ProductImageMapper imageMapper;

    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper,
                              ProductSkuMapper productSkuMapper) {
        this(productMapper, categoryMapper, productSkuMapper, null, null, null, null);
    }

    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper,
                              ProductSkuMapper productSkuMapper,
                              ProductAttributeMapper attributeMapper,
                              ProductAttributeValueMapper valueMapper,
                              SkuAttributeValueMapper mappingMapper) {
        this(productMapper, categoryMapper, productSkuMapper, attributeMapper, valueMapper, mappingMapper, null);
    }

    @Autowired
    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper,
                              ProductSkuMapper productSkuMapper,
                              ProductAttributeMapper attributeMapper,
                              ProductAttributeValueMapper valueMapper,
                              SkuAttributeValueMapper mappingMapper,
                              ProductImageMapper imageMapper) {
        this.baseMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.productSkuMapper = productSkuMapper;
        this.attributeMapper = attributeMapper;
        this.valueMapper = valueMapper;
        this.mappingMapper = mappingMapper;
        this.imageMapper = imageMapper;
    }

    @Override
    public PageResponse<ProductResponse> getProductPage(ProductListRequest request) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Product::getCreatedAt);

        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.like(Product::getTitle, request.getKeyword().trim());
        }
        if (request.getOnSale() != null) {
            queryWrapper.eq(Product::getOnSale, request.getOnSale());
        }
        if (request.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, request.getCategoryId());
        }

        Page<Product> page = this.page(Page.of(request.getPage(), request.getSize()), queryWrapper);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<ProductResponse> getPublicProductPage(ProductPublicListRequest request) {
        validatePublicPriceRange(request);
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        query.eq(Product::getOnSale, 1);
        if (StringUtils.hasText(request.getKeyword())) {
            query.like(Product::getTitle, request.getKeyword().trim());
        }
        query.eq(request.getCategoryId() != null, Product::getCategoryId, request.getCategoryId());
        query.apply(request.getMinPrice() != null,
                MIN_ACTIVE_SKU_PRICE_SQL + " >= {0}",
                request.getMinPrice());
        query.apply(request.getMaxPrice() != null,
                MIN_ACTIVE_SKU_PRICE_SQL + " <= {0}",
                request.getMaxPrice());
        query.apply(Boolean.TRUE.equals(request.getInStock()),
                "EXISTS (SELECT 1 FROM product_sku ps WHERE ps.product_id = product.id AND ps.deleted = 0 AND ps.stock > 0)");
        applyPublicSort(query, request.getSort());

        Page<Product> page = this.page(Page.of(request.getPage(), request.getSize()), query);
        return toPageResponse(page);
    }

    @Override
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit == null ? 10 : limit, 10));
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        List<Product> products = this.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getOnSale, 1)
                .like(Product::getTitle, normalized)
                .orderByDesc(Product::getSoldCount)
                .orderByDesc(Product::getId)
                .last("LIMIT " + safeLimit));
        products.stream().map(Product::getTitle).filter(StringUtils::hasText)
                .forEach(suggestions::add);

        if (suggestions.size() < safeLimit) {
            int remaining = safeLimit - suggestions.size();
            categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                            .eq(Category::getEnabled, 1)
                            .like(Category::getName, normalized)
                            .orderByAsc(Category::getSortOrder)
                            .orderByAsc(Category::getId)
                            .last("LIMIT " + remaining))
                    .stream().map(Category::getName).filter(StringUtils::hasText)
                    .forEach(suggestions::add);
        }
        return suggestions.stream().limit(safeLimit).toList();
    }

    @Override
    public List<ProductResponse> getRecommendations(Long productId, Integer limit) {
        Product current = this.getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getOnSale, 1));
        if (current == null) {
            throw new NoSuchElementException("商品不存在");
        }

        int safeLimit = Math.max(1, Math.min(limit == null ? 8 : limit, 20));
        Map<Long, Product> recommendations = new LinkedHashMap<>();
        if (current.getCategoryId() != null) {
            addRecommendations(recommendations,
                    this.list(recommendationQuery(productId, current.getCategoryId(), Set.of(), safeLimit)),
                    productId, safeLimit);
        }
        if (recommendations.size() < safeLimit) {
            Set<Long> excluded = new java.util.HashSet<>(recommendations.keySet());
            excluded.add(productId);
            addRecommendations(recommendations,
                    this.list(recommendationQuery(productId, null, excluded, safeLimit - recommendations.size())),
                    productId, safeLimit);
        }

        Page<Product> page = Page.of(1, safeLimit);
        page.setRecords(new ArrayList<>(recommendations.values()));
        page.setTotal(recommendations.size());
        return toPageResponse(page).getList();
    }

    @Override
    public ProductDetailResponse getPublicProductDetail(Long id) {
        Product product = this.getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getOnSale, 1));
        if (product == null) {
            throw new NoSuchElementException("商品不存在");
        }

        ProductDetailResponse detail = new ProductDetailResponse();
        ProductResponse productResponse = BeanUtil.copyProperties(product, ProductResponse.class);
        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                productResponse.setCategoryName(category.getName());
            }
        }
        detail.setProduct(productResponse);

        List<ProductSku> productSkus = productSkuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, id)
                                .orderByAsc(ProductSku::getId));
        Map<Long, List<Long>> attributeValueIdsBySku = loadSkuAttributeValueIds(id);
        detail.setSkus(productSkus.stream()
                .map(sku -> toPublicSku(sku, attributeValueIdsBySku.getOrDefault(sku.getId(), List.of())))
                .toList());
        detail.setAttributes(loadAttributeTree(id));

        List<ProductImage> productImages = loadProductImages(id);
        if (!productImages.isEmpty()) {
            detail.setImages(productImages.stream().map(this::toImageResponse).toList());
        } else if (StringUtils.hasText(product.getImage())) {
            ProductDetailResponse.ImageResponse image = new ProductDetailResponse.ImageResponse();
            image.setOriginalUrl(product.getImage());
            image.setUrl(product.getImage());
            image.setStatus(ProductImage.StyleStatus.NONE.name());
            image.setIsMain(true);
            detail.setImages(List.of(image));
        }
        return detail;
    }

    @Override
    @Transactional
    public boolean saveOrUpdateProduct(ProductRequest request) {
        Product product;
        if (request.getId() == null) {
            product = BeanUtil.copyProperties(request, Product.class);
        } else {
            product = findProduct(request.getId());
            BeanUtil.copyProperties(request, product);
        }
        return this.saveOrUpdate(product);
    }

    @Override
    @Transactional
    public boolean deleteProduct(Long id) {
        findProduct(id);
        return this.removeById(id);
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        List<ProductResponse> list = BeanUtil.copyToList(page.getRecords(), ProductResponse.class);
        Set<Long> categoryIds = list.stream()
                .map(ProductResponse::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!categoryIds.isEmpty()) {
            Map<Long, String> categoryNames = categoryMapper.selectByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));
            list.forEach(product -> product.setCategoryName(categoryNames.get(product.getCategoryId())));
        }
        applyPreferredListImages(list);

        PageResponse<ProductResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(list);
        return response;
    }

    private void applyPublicSort(LambdaQueryWrapper<Product> query, String sort) {
        String normalized = StringUtils.hasText(sort) ? sort : "default";
        switch (normalized) {
            case "default", "newest" -> query.orderByDesc(Product::getCreatedAt)
                    .orderByDesc(Product::getId);
            case "priceAsc" -> query.last("ORDER BY (" + MIN_ACTIVE_SKU_PRICE_SQL
                    + " IS NULL) ASC, " + MIN_ACTIVE_SKU_PRICE_SQL + " ASC, id DESC");
            case "priceDesc" -> query.last("ORDER BY (" + MIN_ACTIVE_SKU_PRICE_SQL
                    + " IS NULL) ASC, " + MIN_ACTIVE_SKU_PRICE_SQL + " DESC, id DESC");
            case "sales" -> query.orderByDesc(Product::getSoldCount)
                    .orderByDesc(Product::getId);
            case "rating" -> query.last("ORDER BY (review_count > 0) DESC, rating DESC, review_count DESC, id DESC");
            default -> throw new CustomValidationException("不支持的商品排序方式: " + sort);
        }
    }

    private void validatePublicPriceRange(ProductPublicListRequest request) {
        if (request.getMinPrice() != null && request.getMinPrice().signum() < 0
                || request.getMaxPrice() != null && request.getMaxPrice().signum() < 0) {
            throw new CustomValidationException("价格区间不能小于0");
        }
        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new CustomValidationException("最低价不能高于最高价");
        }
    }

    private LambdaQueryWrapper<Product> recommendationQuery(Long productId, Long categoryId,
                                                              Set<Long> excludedIds, int limit) {
        return new LambdaQueryWrapper<Product>()
                .eq(Product::getOnSale, 1)
                .ne(Product::getId, productId)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .notIn(!excludedIds.isEmpty(), Product::getId, excludedIds)
                .apply("EXISTS (SELECT 1 FROM product_sku ps WHERE ps.product_id = product.id AND ps.deleted = 0 AND ps.stock > 0)")
                .orderByDesc(Product::getSoldCount)
                .orderByDesc(Product::getRating)
                .orderByDesc(Product::getReviewCount)
                .orderByDesc(Product::getId)
                .last("LIMIT " + limit);
    }

    private void addRecommendations(Map<Long, Product> target, List<Product> candidates,
                                    Long currentProductId, int limit) {
        for (Product candidate : candidates) {
            if (candidate.getId() == null || candidate.getId().equals(currentProductId)) {
                continue;
            }
            target.putIfAbsent(candidate.getId(), candidate);
            if (target.size() >= limit) {
                break;
            }
        }
    }

    private List<ProductDetailResponse.AttributeResponse> loadAttributeTree(Long productId) {
        if (attributeMapper == null || valueMapper == null) {
            return List.of();
        }
        List<ProductAttribute> attributes = attributeMapper.selectList(new LambdaQueryWrapper<ProductAttribute>()
                .eq(ProductAttribute::getProductId, productId)
                .orderByAsc(ProductAttribute::getSort)
                .orderByAsc(ProductAttribute::getId));
        List<ProductAttributeValue> values = valueMapper.selectList(new LambdaQueryWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId)
                .orderByAsc(ProductAttributeValue::getSort)
                .orderByAsc(ProductAttributeValue::getId));

        Map<Long, ProductDetailResponse.AttributeResponse> responseById = new LinkedHashMap<>();
        for (ProductAttribute attribute : attributes) {
            ProductDetailResponse.AttributeResponse response = new ProductDetailResponse.AttributeResponse();
            response.setId(attribute.getId());
            response.setName(attribute.getName());
            responseById.put(attribute.getId(), response);
        }
        for (ProductAttributeValue value : values) {
            ProductDetailResponse.AttributeResponse attribute = responseById.get(value.getAttributeId());
            if (attribute == null) {
                continue;
            }
            ProductDetailResponse.AttributeValueResponse valueResponse =
                    new ProductDetailResponse.AttributeValueResponse();
            valueResponse.setId(value.getId());
            valueResponse.setValue(value.getValue());
            attribute.getValues().add(valueResponse);
        }
        return new ArrayList<>(responseById.values());
    }

    private Map<Long, List<Long>> loadSkuAttributeValueIds(Long productId) {
        if (mappingMapper == null) {
            return Map.of();
        }
        List<SkuAttributeValue> mappings = mappingMapper.selectList(new LambdaQueryWrapper<SkuAttributeValue>()
                .eq(SkuAttributeValue::getProductId, productId));
        return mappings.stream()
                .sorted(Comparator.comparing(SkuAttributeValue::getAttributeId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(SkuAttributeValue::getAttributeValueId,
                                Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(SkuAttributeValue::getSkuId,
                        LinkedHashMap::new,
                        Collectors.mapping(SkuAttributeValue::getAttributeValueId, Collectors.toList())));
    }

    private ProductDetailResponse.SkuResponse toPublicSku(ProductSku sku, List<Long> attributeValueIds) {
        ProductDetailResponse.SkuResponse response = new ProductDetailResponse.SkuResponse();
        response.setId(sku.getId());
        response.setPrice(sku.getPrice());
        response.setOriginalPrice(sku.getOriginalPrice());
        response.setStock(sku.getStock());
        response.setSkuCode(sku.getSkuCode());
        response.setMainImageId(sku.getMainImageId());
        response.setAttributeValueIds(attributeValueIds);
        return response;
    }

    private List<ProductImage> loadProductImages(Long productId) {
        if (imageMapper == null) {
            return List.of();
        }
        return imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByDesc(ProductImage::getIsMain)
                .orderByAsc(ProductImage::getSort)
                .orderByAsc(ProductImage::getId));
    }

    private ProductDetailResponse.ImageResponse toImageResponse(ProductImage image) {
        ProductDetailResponse.ImageResponse response = new ProductDetailResponse.ImageResponse();
        response.setId(image.getId());
        response.setSkuId(image.getSkuId());
        response.setOriginalUrl(image.getOriginalUrl());
        response.setStyledUrl(image.getStyledUrl());
        response.setUrl(ProductImageResponse.preferredUrl(image));
        response.setStatus(image.getStyleStatus() == null
                ? ProductImage.StyleStatus.NONE.name() : image.getStyleStatus().name());
        response.setStylePreset(image.getStylePreset());
        response.setIsMain(image.getIsMain());
        return response;
    }

    private void applyPreferredListImages(List<ProductResponse> products) {
        if (imageMapper == null || products.isEmpty()) {
            return;
        }
        List<Long> productIds = products.stream().map(ProductResponse::getId).toList();
        Map<Long, ProductImage> preferredByProduct = imageMapper.selectList(
                        new LambdaQueryWrapper<ProductImage>()
                                .in(ProductImage::getProductId, productIds)
                                .orderByDesc(ProductImage::getIsMain)
                                .orderByAsc(ProductImage::getSort)
                                .orderByAsc(ProductImage::getId))
                .stream().collect(Collectors.toMap(ProductImage::getProductId, image -> image,
                        (first, ignored) -> first, LinkedHashMap::new));
        products.forEach(product -> {
            ProductImage preferred = preferredByProduct.get(product.getId());
            if (preferred != null) {
                product.setImage(ProductImageResponse.preferredUrl(preferred));
            }
        });
    }


    private Product findProduct(Long id) {
        return this.getOptById(id)
                .orElseThrow(() -> new NoSuchElementException("商品不存在"));
    }
}
