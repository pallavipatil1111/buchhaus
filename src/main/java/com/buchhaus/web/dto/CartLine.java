package com.buchhaus.web.dto;

import java.math.BigDecimal;

public record CartLine(BookView book, int quantity, BigDecimal lineTotal, boolean inStock) {
}
