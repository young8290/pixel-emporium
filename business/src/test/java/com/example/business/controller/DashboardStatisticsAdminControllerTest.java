package com.example.business.controller;

import com.example.business.controller.admin.DashboardStatisticsAdminController;
import com.example.business.response.DashboardStatisticsResponse;
import com.example.business.service.DashboardStatisticsService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.annotation.CustomRequireRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardStatisticsAdminControllerTest {
    @Test
    void adminEndpointRequiresAdminRoleAndReturnsStatistics() throws Exception {
        DashboardStatisticsService service = mock(DashboardStatisticsService.class);
        DashboardStatisticsResponse response = new DashboardStatisticsResponse();
        response.setTodayOrderCount(5L);
        when(service.dashboard()).thenReturn(response);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardStatisticsAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        CustomRequireRole role = DashboardStatisticsAdminController.class.getAnnotation(CustomRequireRole.class);
        assertNotNull(role);
        assertEquals(Set.of("ADMIN", "SUPER_ADMIN"), Set.of(role.value()));
        mockMvc.perform(get("/admin/dashboard/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayOrderCount").value(5));
    }
}
