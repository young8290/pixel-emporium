package com.example.business.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductSkuGenerateResponse {
    private int generatedCount;
    private int reusedCount;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> skuIds = new ArrayList<>();
}
