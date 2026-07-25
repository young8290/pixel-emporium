package com.example.business;

import com.example.business.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderStateMachineTest {
    private static final Set<String> ALLOWED = Set.of(
            "PENDING_PAYMENT->PAID",
            "PENDING_PAYMENT->CANCELED",
            "PAID->SHIPPED",
            "SHIPPED->COMPLETED");

    @ParameterizedTest
    @MethodSource("allStatusPairs")
    void acceptsExactlyTheFourDocumentedTransitions(OrderStatus from, OrderStatus to) {
        assertEquals(ALLOWED.contains(from + "->" + to), OrderStatus.canTransfer(from, to));
    }

    @Test
    void nullEndpointsAreNeverTransferable() {
        assertFalse(OrderStatus.canTransfer(null, OrderStatus.PAID));
        assertFalse(OrderStatus.canTransfer(OrderStatus.PAID, null));
    }

    private static Stream<Arguments> allStatusPairs() {
        return Stream.of(OrderStatus.values())
                .flatMap(from -> Stream.of(OrderStatus.values())
                        .map(to -> Arguments.of(from, to)));
    }
}
