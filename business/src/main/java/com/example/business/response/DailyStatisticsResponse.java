package com.example.business.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DailyStatisticsResponse {
    private String day;
    private long orderCount;
    private BigDecimal paidSales;
}
