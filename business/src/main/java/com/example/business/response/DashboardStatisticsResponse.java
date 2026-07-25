package com.example.business.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DashboardStatisticsResponse {
    private long todayOrderCount;
    private BigDecimal todayPaidSales;
    private long totalOrderCount;
    private long onSaleProductCount;
    private List<DailyStatisticsResponse> sevenDayTrend = new ArrayList<>();
    private List<HotProductResponse> hotProducts = new ArrayList<>();
    private List<RecentOrderResponse> recentOrders = new ArrayList<>();
}
