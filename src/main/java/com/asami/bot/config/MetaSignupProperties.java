package com.asami.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asami.meta")
public record MetaSignupProperties(
        String appId,
        String appSecret,
        String configurationId,
        String tokenEncryptionKey,
        String apiVersion
) {
}
