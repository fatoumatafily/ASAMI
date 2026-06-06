package com.asami.bot.whatsapp;

import com.asami.bot.config.WhatsAppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppProperties properties;
    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(
            WhatsAppProperties properties,
            WhatsAppSignatureVerifier signatureVerifier,
            WhatsAppWebhookService webhookService
    ) {
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode") String mode,
            @RequestParam(name = "hub.verify_token") String verifyToken,
            @RequestParam(name = "hub.challenge") String challenge
    ) {
        boolean valid = "subscribe".equals(mode)
                && properties.verifyToken().equals(verifyToken);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> receiveEvent(
            @RequestBody String payload,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (!signatureVerifier.isValid(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        webhookService.process(payload);
        return ResponseEntity.ok().build();
    }
}
