package com.asami.bot.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal price,

        @Size(min = 3, max = 3)
        String currency,

        @PositiveOrZero
        Integer stockQuantity
) {
}
