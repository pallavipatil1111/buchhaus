package com.buchhaus.web.dto;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.core.Pricing;
import com.buchhaus.domain.CustomerOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderView(Long id, OrderStatus status, List<Line> items, BigDecimal subtotal, BigDecimal shipping,
                        BigDecimal total, Shipping shipTo, String customerEmail, boolean canCancel,
                        List<OrderStatus> nextStatuses, Instant createdAt, Instant updatedAt) {

    public record Line(Long bookId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record Shipping(String name, String phone, String address, String city, String pincode) {
    }

    public static OrderView of(CustomerOrder o) {
        List<Line> lines = o.getItems().stream().map(i -> new Line(i.getBook().getId(), i.getTitleSnapshot(),
                Pricing.rupees(i.getUnitPricePaise()), i.getQuantity(), Pricing.rupees(i.getLineTotalPaise()))).toList();
        return new OrderView(o.getId(), o.getStatus(), lines, Pricing.rupees(o.getSubtotalPaise()),
                Pricing.rupees(o.getShippingPaise()), Pricing.rupees(o.getTotalPaise()),
                new Shipping(o.getShipName(), o.getShipPhone(), o.getShipAddress(), o.getShipCity(), o.getShipPincode()),
                o.getUser().getEmail(), o.getStatus().customerCanCancel(),
                o.getStatus().next().stream().sorted().toList(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
