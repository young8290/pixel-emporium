package com.example.business.request;

import com.example.business.enums.OrderStatus;
import com.example.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderListRequest extends PageRequest {
    private OrderStatus status;
}
