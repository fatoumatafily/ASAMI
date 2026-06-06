package com.asami.bot.whatsapp;

import com.asami.bot.config.WhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class WhatsAppClient {

    private final WhatsAppProperties properties;
    private final RestClient restClient;
    private final WhatsAppCredentialsService credentials;

    @Autowired
    public WhatsAppClient(
            WhatsAppProperties properties,
            RestClient.Builder restClientBuilder,
            WhatsAppCredentialsService credentials
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.credentials = credentials;
    }

    WhatsAppClient(WhatsAppProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.credentials = null;
    }

    public String sendText(String recipientPhone, String text) {
        return sendText(properties.phoneNumberId(), recipientPhone, text);
    }

    public String sendText(String phoneNumberId, String recipientPhone, String text) {
        validateConfiguration(phoneNumberId);

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipientPhone,
                "type", "text",
                "text", Map.of(
                        "preview_url", false,
                        "body", text
                )
        );

        return sendMessage(phoneNumberId, payload);
    }

    public String sendTemplate(String recipientPhone, String templateName, String languageCode) {
        validateConfiguration(properties.phoneNumberId());

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", recipientPhone,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", languageCode)
                )
        );

        return sendMessage(properties.phoneNumberId(), payload);
    }

    public WhatsAppMedia downloadMedia(String mediaId) {
        return downloadMedia(properties.phoneNumberId(), mediaId);
    }

    public WhatsAppMedia downloadMedia(String phoneNumberId, String mediaId) {
        String accessToken = validateConfiguration(phoneNumberId);
        try {
            JsonNode metadata = restClient.get()
                    .uri("/{version}/{mediaId}", properties.apiVersion(), mediaId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(JsonNode.class);
            String mediaUrl = metadata == null ? null : metadata.path("url").asText(null);
            if (mediaUrl == null) {
                throw new WhatsAppApiException("Meta did not return a media URL");
            }

            ResponseEntity<byte[]> response = restClient.get()
                    .uri(mediaUrl)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(byte[].class);
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new WhatsAppApiException("Meta returned an empty media file");
            }
            MediaType contentType = response.getHeaders().getContentType();
            return new WhatsAppMedia(
                    response.getBody(),
                    contentType == null ? "audio/ogg" : contentType.toString()
            );
        } catch (RestClientResponseException exception) {
            throw new WhatsAppApiException(
                    "Unable to download WhatsApp media: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    private String sendMessage(String phoneNumberId, Map<String, Object> payload) {
        String accessToken = validateConfiguration(phoneNumberId);
        try {
            JsonNode response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            properties.apiVersion(),
                            phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String messageId = response == null
                    ? null
                    : response.path("messages").path(0).path("id").asText(null);
            if (messageId == null) {
                throw new WhatsAppApiException("Meta did not return a WhatsApp message id");
            }
            return messageId;
        } catch (RestClientResponseException exception) {
            throw new WhatsAppApiException(
                    "Meta WhatsApp API rejected the message: " + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    private String validateConfiguration(String phoneNumberId) {
        String accessToken = credentials == null
                ? properties.accessToken()
                : credentials.accessToken(phoneNumberId);
        if (accessToken == null || accessToken.isBlank()) {
            throw new WhatsAppApiException("WHATSAPP_ACCESS_TOKEN is not configured");
        }
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new WhatsAppApiException("WHATSAPP_PHONE_NUMBER_ID is not configured");
        }
        if (properties.apiVersion() == null || properties.apiVersion().isBlank()) {
            throw new WhatsAppApiException("WHATSAPP_API_VERSION is not configured");
        }
        return accessToken;
    }
}
