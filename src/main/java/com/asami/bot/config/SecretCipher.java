package com.asami.bot.config;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(MetaSignupProperties properties) {
        String value = properties.tokenEncryptionKey();
        if (value == null || value.isBlank()) value = "asami-local-development-key";
        try {
            this.key = new SecretKeySpec(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)),
                    "AES"
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize token encryption", exception);
        }
    }

    public String encrypt(String clearText) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt Meta token", exception);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] value = Base64.getDecoder().decode(encoded);
            byte[] iv = java.util.Arrays.copyOfRange(value, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(value, 12, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decrypt Meta token", exception);
        }
    }
}
