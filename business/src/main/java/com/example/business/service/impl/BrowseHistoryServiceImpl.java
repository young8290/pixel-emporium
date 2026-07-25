package com.example.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.BrowseHistory;
import com.example.business.entity.Product;
import com.example.business.mapper.BrowseHistoryMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.response.BrowseHistoryResponse;
import com.example.business.service.BrowseHistoryService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BrowseHistoryServiceImpl implements BrowseHistoryService {
    private final BrowseHistoryMapper historyMapper;
    private final ProductMapper productMapper;

    public BrowseHistoryServiceImpl(BrowseHistoryMapper historyMapper,
                                    ProductMapper productMapper) {
        this.historyMapper = historyMapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResponse<BrowseHistoryResponse> list(PageRequest request) {
        Long userId = requireUserId();
        PageRequest pageRequest = request == null ? new PageRequest() : request;
        Page<BrowseHistoryResponse> page = historyMapper.selectProductPage(
                Page.of(pageRequest.getPage(), pageRequest.getSize()), userId);
        PageResponse<BrowseHistoryResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords());
        return response;
    }

    @Override
    @Transactional
    public boolean record(Long productId) {
        Long userId = requireUserId();
        requirePublicProduct(productId);
        BrowseHistory existing = historyMapper.selectActive(userId, productId);
        if (existing != null) {
            return increment(userId, productId);
        }

        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setProductId(productId);
        history.setViewCount(1);
        history.setLastViewedAt(LocalDateTime.now());
        try {
            if (historyMapper.insert(history) <= 0) {
                throw new CustomBusinessException("浏览记录保存失败");
            }
            return true;
        } catch (DuplicateKeyException ignored) {
            return increment(userId, productId);
        }
    }

    @Override
    @Transactional
    public boolean remove(Long productId) {
        validateProductId(productId);
        historyMapper.logicalDeleteActive(requireUserId(), productId);
        return true;
    }

    @Override
    @Transactional
    public boolean clear() {
        historyMapper.logicalDeleteAll(requireUserId());
        return true;
    }

    private boolean increment(Long userId, Long productId) {
        if (historyMapper.incrementActive(userId, productId) <= 0) {
            throw new CustomBusinessException("浏览记录更新失败");
        }
        return true;
    }

    private void requirePublicProduct(Long productId) {
        validateProductId(productId);
        Product product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getOnSale())) {
            throw new CustomBusinessException("商品不存在或已下架");
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomValidationException("请先登录");
        }
        return userId;
    }
}
