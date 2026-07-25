package com.example.business.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderShipRequest {
    @NotBlank(message = "物流公司不能为空")
    @Size(max = 50, message = "物流公司长度不能超过 {max} 位")
    private String shippingCarrier;

    @NotBlank(message = "运单号不能为空")
    @Size(max = 100, message = "运单号长度不能超过 {max} 位")
    private String trackingNumber;

    @Size(max = 255, message = "发货备注长度不能超过 {max} 位")
    private String shippingNote;
}
