package com.example.user.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
@Getter @Setter public class OrderCreatedMailEvent {
    private String eventId; private String eventType; @JsonSerialize(using = ToStringSerializer.class) private Long userId; @JsonSerialize(using = ToStringSerializer.class) private Long orderId; private String orderNo; private OffsetDateTime createdAt; private BigDecimal totalAmount; private BigDecimal freightAmount; private BigDecimal payAmount; private Receiver receiver; private List<Item> items;
    @Getter @Setter public static class Receiver { private String name; private String phone; private String province; private String city; private String district; private String detail; }
    @Getter @Setter public static class Item { @JsonSerialize(using = ToStringSerializer.class) private Long productId; private String productTitle; private String skuTitle; private String image; private BigDecimal price; private Integer quantity; private BigDecimal subtotal; }
}
