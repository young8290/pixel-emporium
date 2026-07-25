package com.example.user.controller.admin;

import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import com.example.user.request.OperationLogListRequest;
import com.example.user.response.OperationLogResponse;
import com.example.user.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/operation-log")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class OperationLogAdminController {
    private final OperationLogService operationLogService;

    public OperationLogAdminController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/list")
    public R<PageResponse<OperationLogResponse>> list(@Valid OperationLogListRequest request) {
        return R.ok(operationLogService.adminList(request));
    }
}
