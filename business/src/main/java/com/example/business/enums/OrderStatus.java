package com.example.business.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED;

    public static boolean canTransfer(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case PENDING_PAYMENT -> to == PAID || to == CANCELED;
            case PAID -> to == SHIPPED;
            case SHIPPED -> to == COMPLETED;
            case COMPLETED, CANCELED -> false;
        };
    }
}
