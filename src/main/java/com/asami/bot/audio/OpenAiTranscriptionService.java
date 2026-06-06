package com.asami.bot.audio;

import com.asami.bot.config.OpenAiProperties;
import com.asami.bot.whatsapp.WhatsAppMedia;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenAiTranscriptionService {

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiTranscriptionService(
            OpenAiProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com")
                .build();
    }

    public TranscriptionResult transcribe(WhatsAppMedia media) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AudioTranscriptionException("OPENAI_API_KEY is not configured");
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("model", properties.transcriptionModel());
        body.part("response_format", "json");
        body.part("prompt",
                "Conversation commerciale au Senegal. "
                        + "La langue peut etre le wolof ou le francais. "
                        + "Conserver les noms de produits, prix et expressions locales.");
        body.part("file", new ByteArrayResource(media.content()) {
            @Override
            public String getFilename() {
                return "whatsapp-audio.ogg";
            }
        }).contentType(MediaType.parseMediaType(media.contentType()));

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/audio/transcriptions")
                    .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(JsonNode.class);

            String text = response == null ? null : response.path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new AudioTranscriptionException(
                        "OpenAI returned an empty transcription"
                );
            }
            return new TranscriptionResult(text.trim(), detectLanguage(text));
        } catch (RestClientResponseException exception) {
            throw new AudioTranscriptionException(
                    "OpenAI transcription failed: " + exception.getResponseBodyAsString(),
                    exception
            );
        }
    }

    private String detectLanguage(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("nanga") || lower.contains("am nga")
                || lower.contains("jerejef") || lower.contains("waaw")) {
            return "wo";
        }
        return "fr";
    }
}
