package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.Cart;
import com.example.business.request.CartAddRequest;
import com.example.business.request.CartUpdateRequest;
import com.example.business.response.CartViewResponse;

import java.util.List;

public interface CartService extends IService<Cart> {
    CartViewResponse getCart();

    boolean add(CartAddRequest request);

    boolean update(CartUpdateRequest request);

    boolean delete(Long id);

    boolean deleteBatch(List<Long> ids);

    boolean clear();
}
