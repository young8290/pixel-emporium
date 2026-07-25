package com.example.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.service.SkuAttributeValueService;
import org.springframework.stereotype.Service;

@Service
public class SkuAttributeValueServiceImpl
        extends ServiceImpl<SkuAttributeValueMapper, SkuAttributeValue>
        implements SkuAttributeValueService {
}
