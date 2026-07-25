package com.example.business.controller;

import com.example.business.response.BrowseHistoryResponse;
import com.example.business.service.BrowseHistoryService;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("browse-history")
public class BrowseHistoryController {
    private final BrowseHistoryService historyService;

    public BrowseHistoryController(BrowseHistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(summary = "查询我的浏览历史")
    @GetMapping("/list")
    public R<PageResponse<BrowseHistoryResponse>> list(@Valid PageRequest request) {
        return R.ok(historyService.list(request));
    }

    @Operation(summary = "记录商品浏览")
    @PostMapping("/{productId}")
    public R<Boolean> record(@PathVariable Long productId) {
        return R.ok(historyService.record(productId));
    }

    @Operation(summary = "删除单条浏览历史")
    @DeleteMapping("/{productId}")
    public R<Boolean> remove(@PathVariable Long productId) {
        return R.ok(historyService.remove(productId));
    }

    @Operation(summary = "清空浏览历史")
    @DeleteMapping("/clear")
    public R<Boolean> clear() {
        return R.ok(historyService.clear());
    }
}
