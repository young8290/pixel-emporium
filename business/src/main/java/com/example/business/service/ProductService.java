package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.Product;
import com.example.business.request.ProductListRequest;
import com.example.business.request.ProductPublicListRequest;
import com.example.business.request.ProductRequest;
import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductResponse;
import com.example.common.response.PageResponse;

import java.util.List;

public interface ProductService extends IService<Product> {
    PageResponse<ProductResponse> getProductPage(ProductListRequest request);

    PageResponse<ProductResponse> getPublicProductPage(ProductPublicListRequest request);

    ProductDetailResponse getPublicProductDetail(Long id);

    List<String> getSearchSuggestions(String keyword, Integer limit);

    List<ProductResponse> getRecommendations(Long productId, Integer limit);

    boolean saveOrUpdateProduct(ProductRequest request);

    boolean deleteProduct(Long id);
}
