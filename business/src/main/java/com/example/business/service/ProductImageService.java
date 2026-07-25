package com.example.business.service;

import com.example.business.entity.ProductImage;
import com.example.business.response.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductImageService {
    ProductImage upload(Long productId, Long skuId, Integer sort, MultipartFile file);

    List<ProductImageResponse> list(Long productId, Long skuId);

    boolean delete(Long id);

    ProductImageResponse setMain(Long id);

    ProductImageResponse status(Long id);
}
