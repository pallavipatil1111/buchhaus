package com.buchhaus.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 0 removes the book from the cart. */
public record QuantityRequest(@NotNull @Min(0) @Max(10) Integer quantity) {
}
