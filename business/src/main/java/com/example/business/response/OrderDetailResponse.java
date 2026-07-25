package com.example.business.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDetailResponse extends OrderResponse {
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverDetail;
    private String remark;
}
