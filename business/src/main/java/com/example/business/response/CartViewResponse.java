package com.example.business.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CartViewResponse {
    private List<CartItemResponse> items = new ArrayList<>();
    private BigDecimal checkedAmount = BigDecimal.ZERO;
    private Integer checkedQuantity = 0;
}
