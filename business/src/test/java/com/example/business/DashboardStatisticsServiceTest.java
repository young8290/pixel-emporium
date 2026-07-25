package com.example.business;

import com.example.business.mapper.DashboardStatisticsMapper;
import com.example.business.response.DailyStatisticsResponse;
import com.example.business.response.DashboardStatisticsResponse;
import com.example.business.response.HotProductResponse;
import com.example.business.service.impl.DashboardStatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatisticsServiceTest {
    @Mock
    private DashboardStatisticsMapper statisticsMapper;

    @Test
    void dashboardUsesDatabaseAggregatesAndReturnsAllViews() {
        DailyStatisticsResponse trend = new DailyStatisticsResponse();
        trend.setDay("2026-07-25");
        trend.setOrderCount(3);
        HotProductResponse product = new HotProductResponse();
        product.setProductId(7L);
        product.setSoldQuantity(12);
        when(statisticsMapper.selectTodayOrderCount()).thenReturn(4L);
        when(statisticsMapper.selectTodayPaidSales()).thenReturn(new BigDecimal("88.80"));
        when(statisticsMapper.selectTotalOrderCount()).thenReturn(40L);
        when(statisticsMapper.selectOnSaleProductCount()).thenReturn(9L);
        when(statisticsMapper.selectSevenDayTrend()).thenReturn(List.of(trend));
        when(statisticsMapper.selectHotProducts()).thenReturn(List.of(product));

        DashboardStatisticsResponse response = new DashboardStatisticsServiceImpl(statisticsMapper).dashboard();

        assertEquals(4L, response.getTodayOrderCount());
        assertEquals(new BigDecimal("88.80"), response.getTodayPaidSales());
        assertEquals(40L, response.getTotalOrderCount());
        assertEquals(9L, response.getOnSaleProductCount());
        assertEquals("2026-07-25", response.getSevenDayTrend().getFirst().getDay());
        assertEquals(7L, response.getHotProducts().getFirst().getProductId());
        assertEquals(List.of(), response.getRecentOrders());
    }
}
