package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.business.entity.Product;
import com.example.business.entity.ProductImage;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.ProductImageMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.response.ProductImageResponse;
import com.example.business.service.ProductImageService;
import com.example.business.service.ValidatedOssStorage;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageMapper imageMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ValidatedOssStorage storage;

    public ProductImageServiceImpl(ProductImageMapper imageMapper, ProductMapper productMapper,
                                   ProductSkuMapper skuMapper, ValidatedOssStorage storage) {
        this.imageMapper = imageMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.storage = storage;
    }

    @Override
    @Transactional
    public ProductImage upload(Long productId, Long skuId, Integer sort, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomValidationException("文件不能为空");
        }
        Product product = requireProduct(productId);
        validateSku(productId, skuId);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new CustomBusinessException("商品图片读取失败");
        }
        String url = storage.storeProductImage(UserContext.get(), file.getOriginalFilename(),
                file.getContentType(), bytes);
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setSkuId(skuId);
        image.setOriginalUrl(url);
        image.setStyleStatus(ProductImage.StyleStatus.NONE);
        image.setSort(sort == null ? 99 : Math.max(0, sort));
        image.setIsMain(false);
        if (imageMapper.insert(image) <= 0) {
            storage.deleteOwnObject(url);
            throw new CustomBusinessException("商品图片保存失败");
        }
        return image;
    }

    @Override
    public List<ProductImageResponse> list(Long productId, Long skuId) {
        requireProduct(productId);
        return imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .eq(skuId != null, ProductImage::getSkuId, skuId)
                        .orderByDesc(ProductImage::getIsMain)
                        .orderByAsc(ProductImage::getSort)
                        .orderByAsc(ProductImage::getId))
                .stream().map(ProductImageResponse::from).toList();
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        ProductImage image = requireImage(id);
        boolean deleted = imageMapper.deleteById(id) > 0;
        if (!deleted) {
            return false;
        }
        storage.deleteOwnObject(image.getOriginalUrl());
        storage.deleteOwnObject(image.getStyledUrl());
        if (Boolean.TRUE.equals(image.getIsMain())) {
            if (image.getSkuId() != null) {
                ProductSku sku = skuMapper.selectById(image.getSkuId());
                if (sku != null && id.equals(sku.getMainImageId())) {
                    sku.setMainImageId(null);
                    skuMapper.updateById(sku);
                }
            } else {
                Product product = requireProduct(image.getProductId());
                if (ProductImageResponse.preferredUrl(image).equals(product.getImage())) {
                    product.setImage(null);
                    productMapper.updateById(product);
                }
            }
        }
        return true;
    }

    @Override
    @Transactional
    public ProductImageResponse setMain(Long id) {
        ProductImage image = requireImage(id);
        LambdaUpdateWrapper<ProductImage> clear = new LambdaUpdateWrapper<ProductImage>()
                .set(ProductImage::getIsMain, false)
                .eq(ProductImage::getProductId, image.getProductId());
        if (image.getSkuId() == null) {
            clear.isNull(ProductImage::getSkuId);
        } else {
            clear.eq(ProductImage::getSkuId, image.getSkuId());
        }
        imageMapper.update(null, clear);
        image.setIsMain(true);
        imageMapper.updateById(image);
        String preferred = ProductImageResponse.preferredUrl(image);
        if (image.getSkuId() != null) {
            ProductSku sku = validateSku(image.getProductId(), image.getSkuId());
            sku.setMainImageId(image.getId());
            skuMapper.updateById(sku);
        } else {
            Product product = requireProduct(image.getProductId());
            product.setImage(preferred);
            productMapper.updateById(product);
        }
        return ProductImageResponse.from(image);
    }

    @Override
    public ProductImageResponse status(Long id) {
        return ProductImageResponse.from(requireImage(id));
    }

    private Product requireProduct(Long productId) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NoSuchElementException("商品不存在");
        }
        return product;
    }

    private ProductSku validateSku(Long productId, Long skuId) {
        if (skuId == null) {
            return null;
        }
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || !productId.equals(sku.getProductId())) {
            throw new CustomValidationException("SKU不属于当前商品");
        }
        return sku;
    }

    private ProductImage requireImage(Long id) {
        ProductImage image = id == null ? null : imageMapper.selectById(id);
        if (image == null) {
            throw new NoSuchElementException("商品图片不存在");
        }
        return image;
    }
}
