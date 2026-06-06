package com.asami.bot.seller;

import com.asami.bot.config.MetaSignupProperties;
import com.asami.bot.config.SecretCipher;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MetaOnboardingService {
    private final MetaSignupProperties properties;
    private final SellerRepository sellers;
    private final SecretCipher cipher;
    private final RestClient meta;

    public MetaOnboardingService(
            MetaSignupProperties properties,
            SellerRepository sellers,
            SecretCipher cipher,
            RestClient.Builder builder
    ) {
        this.properties = properties;
        this.sellers = sellers;
        this.cipher = cipher;
        this.meta = builder.baseUrl("https://graph.facebook.com").build();
    }

    @Transactional
    public void connect(
            Seller seller,
            String code,
            String phoneNumberId,
            String businessAccountId
    ) {
        validateConfiguration();
        try {
            JsonNode tokenResponse = meta.get()
                    .uri(uri -> uri.path("/{version}/oauth/access_token")
                            .queryParam("client_id", properties.appId())
                            .queryParam("client_secret", properties.appSecret())
                            .queryParam("code", code)
                            .build(properties.apiVersion()))
                    .retrieve()
                    .body(JsonNode.class);
            String token = tokenResponse == null
                    ? null
                    : tokenResponse.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Meta n'a pas retourne de token"
                );
            }

            JsonNode phone = meta.get()
                    .uri("/{version}/{phoneNumberId}?fields=display_phone_number",
                            properties.apiVersion(), phoneNumberId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(JsonNode.class);
            String displayPhone = phone == null
                    ? phoneNumberId
                    : phone.path("display_phone_number").asText(phoneNumberId);
            seller.connectWhatsApp(
                    phoneNumberId,
                    businessAccountId,
                    displayPhone,
                    cipher.encrypt(token)
            );
            sellers.save(seller);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Meta a refuse la connexion WhatsApp: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.appId()) || isBlank(properties.appSecret())
                || isBlank(properties.configurationId())
                || isBlank(properties.tokenEncryptionKey())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Embedded Signup Meta n'est pas encore configure"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
