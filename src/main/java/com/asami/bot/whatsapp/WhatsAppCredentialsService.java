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
                .map(seller -> {
                    if ("META_TEST_ACCOUNT".equals(seller.getWhatsappBusinessAccountId())) {
                        return fallback.accessToken();
                    }
                    String encryptedToken = seller.getWhatsappAccessTokenEncrypted();
                    return encryptedToken == null || encryptedToken.isBlank()
                            ? fallback.accessToken()
                            : cipher.decrypt(encryptedToken);
                })
                .orElse(fallback.accessToken());
    }
}
