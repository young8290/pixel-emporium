package com.example.business.response;

import com.example.business.enums.OrderStatus;
import com.example.business.enums.ProductReviewStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderReviewEligibilityResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    private OrderStatus orderStatus;
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long orderItemId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long productId;

        private String productTitle;
        private String skuTitle;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long reviewId;

        private Boolean eligible = false;
        private Boolean editable = false;
        private Boolean deletable = false;
        private ProductReviewStatus reviewStatus;
        private Integer rating;
        private String content;
        private String moderationReason;
    }
}
