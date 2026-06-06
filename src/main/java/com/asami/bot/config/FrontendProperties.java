package com.asami.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asami.frontend")
public record FrontendProperties(String allowedOrigin) {
}
