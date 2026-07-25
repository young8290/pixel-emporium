package com.example.business.service;

import com.example.business.response.ProductResponse;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;

public interface ProductFavoriteService {
    PageResponse<ProductResponse> list(PageRequest request);

    boolean add(Long productId);

    boolean remove(Long productId);

    boolean isFavorite(Long productId);
}
