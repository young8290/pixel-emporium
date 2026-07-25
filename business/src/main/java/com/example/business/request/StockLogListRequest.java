package com.example.business.request;

import com.example.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockLogListRequest extends PageRequest {
    private Long productSkuId;
    private String type;
    private String bizNo;
}
