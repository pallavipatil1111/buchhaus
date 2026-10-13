package com.buchhaus.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** {@code freeShippingGap}: how much more to add for free shipping (0 once qualified). */
public record CartView(List<CartLine> items, int itemCount, BigDecimal subtotal, BigDecimal shipping, BigDecimal total,
                       BigDecimal freeShippingGap, boolean canCheckout) {
}
