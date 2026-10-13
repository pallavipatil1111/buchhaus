package com.buchhaus.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void happyPathMovesForwardOnly() {
        assertThat(OrderStatus.PLACED.canMoveTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canMoveTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canMoveTo(OrderStatus.PLACED)).isFalse();
        assertThat(OrderStatus.PLACED.canMoveTo(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void finishedOrdersAreFinal() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(OrderStatus.DELIVERED.canMoveTo(s)).isFalse();
            assertThat(OrderStatus.CANCELLED.canMoveTo(s)).isFalse();
        }
        assertThat(OrderStatus.DELIVERED.isFinal()).isTrue();
        assertThat(OrderStatus.PLACED.isFinal()).isFalse();
    }

    @Test
    void customersCanOnlyCancelBeforeShipping() {
        assertThat(OrderStatus.PLACED.customerCanCancel()).isTrue();
        assertThat(OrderStatus.SHIPPED.customerCanCancel()).isFalse();
        assertThat(OrderStatus.SHIPPED.canMoveTo(OrderStatus.CANCELLED)).isTrue(); // admins still can
    }
}
