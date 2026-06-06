package com.asami.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asami.whatsapp")
public record WhatsAppProperties(
        String verifyToken,
        String appSecret,
        String accessToken,
        String phoneNumberId,
        String apiVersion,
        boolean testEndpointEnabled
) {
}
