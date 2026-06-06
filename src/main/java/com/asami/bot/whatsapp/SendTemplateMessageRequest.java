package com.asami.bot.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendTemplateMessageRequest(
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{7,14}$", message = "Use the international number without +")
        String recipientPhone
) {
}
