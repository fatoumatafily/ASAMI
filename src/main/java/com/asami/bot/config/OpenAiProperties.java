package com.asami.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asami.openai")
public record OpenAiProperties(
        String apiKey,
        String transcriptionModel
) {
}
