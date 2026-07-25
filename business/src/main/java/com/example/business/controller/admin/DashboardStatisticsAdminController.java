package com.example.business.controller.admin;

import com.example.business.response.DashboardStatisticsResponse;
import com.example.business.service.DashboardStatisticsService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/dashboard")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class DashboardStatisticsAdminController {
    private final DashboardStatisticsService dashboardStatisticsService;

    public DashboardStatisticsAdminController(DashboardStatisticsService dashboardStatisticsService) {
        this.dashboardStatisticsService = dashboardStatisticsService;
    }

    @Operation(summary = "管理端经营仪表盘统计")
    @GetMapping("/statistics")
    public R<DashboardStatisticsResponse> statistics() {
        return R.ok(dashboardStatisticsService.dashboard());
    }
}
