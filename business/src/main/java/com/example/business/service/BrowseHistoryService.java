package com.example.business.service;

import com.example.business.response.BrowseHistoryResponse;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;

public interface BrowseHistoryService {
    PageResponse<BrowseHistoryResponse> list(PageRequest request);

    boolean record(Long productId);

    boolean remove(Long productId);

    boolean clear();
}
