package com.asami.bot.whatsapp;

import com.asami.bot.config.SecretCipher;
import com.asami.bot.config.WhatsAppProperties;
import com.asami.bot.seller.SellerRepository;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppCredentialsService {
    private final SellerRepository sellers;
    private final SecretCipher cipher;
    private final WhatsAppProperties fallback;

    public WhatsAppCredentialsService(
            SellerRepository sellers,
            SecretCipher cipher,
            WhatsAppProperties fallback
    ) {
        this.sellers = sellers;
        this.cipher = cipher;
        this.fallback = fallback;
    }

    public String accessToken(String phoneNumberId) {
        return sellers.findByWhatsappPhoneNumberId(phoneNumberId)
                .map(seller -> seller.getWhatsappAccessTokenEncrypted())
                .filter(value -> value != null && !value.isBlank())
                .map(cipher::decrypt)
                .orElse(fallback.accessToken());
    }
}
