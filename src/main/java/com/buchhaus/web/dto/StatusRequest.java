package com.buchhaus.web.dto;

import com.buchhaus.core.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull OrderStatus status) {
}
