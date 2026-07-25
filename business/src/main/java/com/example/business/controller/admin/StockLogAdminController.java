package com.example.business.controller.admin;

import com.example.business.request.StockLogListRequest;
import com.example.business.response.StockLogResponse;
import com.example.business.service.StockLogService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存变动流水查询接口（BE-P2-03，只读，ADMIN 权限）。
 */
@RestController
@RequestMapping("admin/stock-log")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class StockLogAdminController {
    private final StockLogService stockLogService;

    public StockLogAdminController(StockLogService stockLogService) {
        this.stockLogService = stockLogService;
    }

    @Operation(summary = "库存流水列表")
    @GetMapping("/list")
    public R<PageResponse<StockLogResponse>> list(@Valid StockLogListRequest request) {
        return R.ok(stockLogService.adminList(request));
    }
}
