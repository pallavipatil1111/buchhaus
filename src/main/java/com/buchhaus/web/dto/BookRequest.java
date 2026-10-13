package com.buchhaus.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Admin create/update. {@code version} must echo the value read, so concurrent edits are detected. */
public record BookRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "use 3-20 capital letters, digits or dashes") String sku,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 120) String author,
        @NotBlank @Size(max = 40) String genre,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.00") @DecimalMax("99999.99") BigDecimal price,
        @NotNull @Min(0) @Max(100000) Integer stock,
        @Min(1450) @Max(2100) Integer publishedYear,
        @Min(1) @Max(10000) Integer pages,
        Long version) {
}
