package com.example.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.service.ProductAttributeValueService;
import org.springframework.stereotype.Service;

@Service
public class ProductAttributeValueServiceImpl
        extends ServiceImpl<ProductAttributeValueMapper, ProductAttributeValue>
        implements ProductAttributeValueService {
}
