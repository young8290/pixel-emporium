package com.example.user.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AddressResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String address;

    private String recipient;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private Boolean isDefault;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
