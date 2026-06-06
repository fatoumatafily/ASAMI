package com.asami.bot.whatsapp;

import com.asami.bot.config.WhatsAppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WhatsAppSignatureVerifier {

    private static final String PREFIX = "sha256=";

    private final WhatsAppProperties properties;

    public WhatsAppSignatureVerifier(WhatsAppProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String body, String signatureHeader) {
        if (properties.appSecret() == null || properties.appSecret().isBlank()) {
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.appSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(signatureHeader.substring(PREFIX.length()));
            return MessageDigest.isEqual(expected, supplied);
        } catch (Exception exception) {
            return false;
        }
    }
}
