package com.example.business.service.impl;

import com.example.business.mapper.DashboardStatisticsMapper;
import com.example.business.response.DashboardStatisticsResponse;
import com.example.business.service.DashboardStatisticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardStatisticsServiceImpl implements DashboardStatisticsService {
    private final DashboardStatisticsMapper statisticsMapper;

    public DashboardStatisticsServiceImpl(DashboardStatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public DashboardStatisticsResponse dashboard() {
        DashboardStatisticsResponse response = new DashboardStatisticsResponse();
        response.setTodayOrderCount(statisticsMapper.selectTodayOrderCount());
        response.setTodayPaidSales(defaultAmount(statisticsMapper.selectTodayPaidSales()));
        response.setTotalOrderCount(statisticsMapper.selectTotalOrderCount());
        response.setOnSaleProductCount(statisticsMapper.selectOnSaleProductCount());
        response.setSevenDayTrend(defaultList(statisticsMapper.selectSevenDayTrend()));
        response.setHotProducts(defaultList(statisticsMapper.selectHotProducts()));
        response.setRecentOrders(defaultList(statisticsMapper.selectRecentOrders()));
        return response;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private <T> List<T> defaultList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
