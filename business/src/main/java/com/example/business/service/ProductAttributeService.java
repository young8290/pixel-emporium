package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.ProductAttribute;
import com.example.business.request.ProductAttributeRequest;
import com.example.business.request.ProductAttributeValueRequest;
import com.example.business.response.ProductAttributeResponse;

import java.util.List;

public interface ProductAttributeService extends IService<ProductAttribute> {
    List<ProductAttributeResponse> listTree(Long productId);

    boolean saveAttribute(ProductAttributeRequest request);

    boolean deleteAttribute(Long productId, Long id);

    boolean saveValue(ProductAttributeValueRequest request);

    boolean deleteValue(Long productId, Long id);
}
