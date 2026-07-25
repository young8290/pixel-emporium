package com.example.business.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderAddressSnapshot {
    private Long id;
    private Long userId;
    private String recipient;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
}
