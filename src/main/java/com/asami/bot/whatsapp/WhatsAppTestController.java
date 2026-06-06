package com.asami.bot.whatsapp;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/whatsapp")
@ConditionalOnProperty(
        prefix = "asami.whatsapp",
        name = "test-endpoint-enabled",
        havingValue = "true"
)
public class WhatsAppTestController {

    private final WhatsAppClient whatsAppClient;

    public WhatsAppTestController(WhatsAppClient whatsAppClient) {
        this.whatsAppClient = whatsAppClient;
    }

    @PostMapping("/text")
    public ResponseEntity<SendTextMessageResponse> sendText(
            @Valid @RequestBody SendTextMessageRequest request
    ) {
        String messageId = whatsAppClient.sendText(request.recipientPhone(), request.text());
        return ResponseEntity.ok(new SendTextMessageResponse(messageId, "accepted"));
    }

    @PostMapping("/hello-world")
    public ResponseEntity<SendTextMessageResponse> sendHelloWorld(
            @Valid @RequestBody SendTemplateMessageRequest request
    ) {
        String messageId = whatsAppClient.sendTemplate(
                request.recipientPhone(),
                "hello_world",
                "en_US"
        );
        return ResponseEntity.ok(new SendTextMessageResponse(messageId, "accepted"));
    }
}
