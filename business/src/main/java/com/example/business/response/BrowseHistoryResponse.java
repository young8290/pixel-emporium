package com.example.business.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BrowseHistoryResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long historyId;

    private String title;
    private String description;
    private String image;
    private Boolean onSale;
    private BigDecimal rating;
    private Integer soldCount;
    private Integer reviewCount;
    private BigDecimal price;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    private String categoryName;
    private Integer viewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastViewedAt;
}
