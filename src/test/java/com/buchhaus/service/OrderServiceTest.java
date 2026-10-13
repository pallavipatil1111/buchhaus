package com.buchhaus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.domain.CustomerOrder;
import com.buchhaus.web.dto.CheckoutRequest;
import com.buchhaus.web.dto.OrderView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderServiceTest {

    private static final CheckoutRequest ADDRESS =
            new CheckoutRequest("Pallavi Patil", "9876543210", " Flat 12, Baner Road ", "Pune", "411045");

    private Fixture f;
    private OrderService service;

    @BeforeEach
    void setUp() {
        f = new Fixture();
        service = new OrderService(f.orders, f.cartItems, f.books, f.users, Fixture.CLOCK);
    }

    @Test
    void checkoutTakesStockSnapshotsPricesAndEmptiesTheCart() {
        f.inCart(f.cleanCode, 2);
        f.inCart(f.ikigai, 1);

        OrderView order = service.checkout(1L, ADDRESS);

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.subtotal()).isEqualByComparingTo(new BigDecimal("1948"));
        assertThat(order.shipping()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.total()).isEqualByComparingTo(new BigDecimal("1948"));
        assertThat(order.items()).extracting(OrderView.Line::title).containsExactly("Clean Code", "Ikigai");
        assertThat(order.shipTo().address()).isEqualTo("Flat 12, Baner Road");
        assertThat(order.canCancel()).isTrue();
        assertThat(f.cleanCode.getStock()).isEqualTo(10);
        assertThat(f.ikigai.getStock()).isEqualTo(1);
        verify(f.cartItems).deleteAllForUser(1L);

        // later price changes don't rewrite history
        f.cleanCode.setPricePaise(999_00);
        ArgumentCaptor<CustomerOrder> saved = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(f.orders).save(saved.capture());
        assertThat(saved.getValue().getItems().get(0).getUnitPricePaise()).isEqualTo(799_00L);
    }

    @Test
    void smallOrdersPayShipping() {
        f.inCart(f.ikigai, 1);
        OrderView order = service.checkout(1L, ADDRESS);
        assertThat(order.shipping()).isEqualByComparingTo(new BigDecimal("49"));
        assertThat(order.total()).isEqualByComparingTo(new BigDecimal("399"));
    }

    @Test
    void stockIsRecheckedUnderTheLockAndNothingIsTakenOnFailure() {
        f.inCart(f.cleanCode, 1);
        f.inCart(f.ikigai, 2);
        f.ikigai.setStock(1);                        // sold while it sat in the cart

        assertThatThrownBy(() -> service.checkout(1L, ADDRESS))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only 1 copies of \"Ikigai\"");
        verify(f.orders, never()).save(any());
        verify(f.cartItems, never()).deleteAllForUser(any());
        // In the real app the transaction rolls back the Clean Code decrement as well.
    }

    @Test
    void retiredBooksBlockCheckout() {
        f.inCart(f.cleanCode, 1);
        f.cleanCode.setActive(false);
        assertThatThrownBy(() -> service.checkout(1L, ADDRESS)).hasMessageContaining("no longer sold");
    }

    @Test
    void emptyCartCannotCheckout() {
        assertThatThrownBy(() -> service.checkout(1L, ADDRESS)).hasMessage("Your cart is empty");
    }

    @Test
    void cancellingRestoresStock() {
        f.inCart(f.cleanCode, 3);
        service.checkout(1L, ADDRESS);
        ArgumentCaptor<CustomerOrder> saved = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(f.orders).save(saved.capture());
        CustomerOrder order = saved.getValue();
        order.setId(50L);
        when(f.orders.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(order));
        assertThat(f.cleanCode.getStock()).isEqualTo(9);

        OrderView cancelled = service.cancel(1L, 50L);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(f.cleanCode.getStock()).isEqualTo(12);
        assertThat(cancelled.canCancel()).isFalse();
    }

    @Test
    void shippedOrdersCannotBeCancelledByTheCustomerButAdminsMoveThemOn() {
        f.inCart(f.ikigai, 1);
        service.checkout(1L, ADDRESS);
        ArgumentCaptor<CustomerOrder> saved = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(f.orders).save(saved.capture());
        CustomerOrder order = saved.getValue();
        order.setId(7L);
        when(f.orders.findById(7L)).thenReturn(Optional.of(order));
        when(f.orders.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(order));

        assertThat(service.updateStatus(7L, OrderStatus.SHIPPED).nextStatuses())
                .containsExactly(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(1L, 7L)).hasMessageContaining("already shipped");
        assertThatThrownBy(() -> service.updateStatus(7L, OrderStatus.PLACED)).hasMessageContaining("Can't move");
        assertThat(service.updateStatus(7L, OrderStatus.DELIVERED).status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void someoneElsesOrderIsNotFound() {
        when(f.orders.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(1L, 5L)).hasMessage("Order not found");
        assertThatThrownBy(() -> service.cancel(1L, 5L)).hasMessage("Order not found");
    }

    @Test
    void adminCanFilterByStatus() {
        when(f.orders.findByStatusOrderByCreatedAtDescIdDesc(OrderStatus.SHIPPED)).thenReturn(List.of());
        assertThat(service.all(OrderStatus.SHIPPED)).isEmpty();
        verify(f.orders).findByStatusOrderByCreatedAtDescIdDesc(OrderStatus.SHIPPED);
    }
}
