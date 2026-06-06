package com.asami.bot.seller;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sellers")
public class Seller {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "whatsapp_phone_number_id", unique = true)
    private String whatsappPhoneNumberId;

    @Column(name = "whatsapp_business_account_id")
    private String whatsappBusinessAccountId;

    @Column(name = "whatsapp_display_phone", length = 30)
    private String whatsappDisplayPhone;

    @Column(name = "whatsapp_connection_status", nullable = false, length = 30)
    private String whatsappConnectionStatus = "DISCONNECTED";

    @Column(name = "whatsapp_access_token_encrypted", columnDefinition = "text")
    private String whatsappAccessTokenEncrypted;

    @Column(name = "whatsapp_connected_at")
    private Instant whatsappConnectedAt;

    @Column(name = "default_language", nullable = false)
    private String defaultLanguage = "fr";

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Seller() {
    }

    public Seller(String businessName, String whatsappPhoneNumberId) {
        this.businessName = businessName;
        this.whatsappPhoneNumberId = whatsappPhoneNumberId;
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getWhatsappPhoneNumberId() {
        return whatsappPhoneNumberId;
    }

    public String getWhatsappBusinessAccountId() {
        return whatsappBusinessAccountId;
    }

    public String getWhatsappDisplayPhone() {
        return whatsappDisplayPhone;
    }

    public String getWhatsappConnectionStatus() {
        return whatsappConnectionStatus;
    }

    public void connectWhatsApp(String phoneNumberId, String businessAccountId, String displayPhone) {
        connectWhatsApp(phoneNumberId, businessAccountId, displayPhone, null);
    }

    public void connectWhatsApp(
            String phoneNumberId,
            String businessAccountId,
            String displayPhone,
            String encryptedAccessToken
    ) {
        this.whatsappPhoneNumberId = phoneNumberId;
        this.whatsappBusinessAccountId = businessAccountId;
        this.whatsappDisplayPhone = displayPhone;
        if (encryptedAccessToken != null) {
            this.whatsappAccessTokenEncrypted = encryptedAccessToken;
        }
        this.whatsappConnectionStatus = "CONNECTED";
        this.whatsappConnectedAt = Instant.now();
    }

    public String getWhatsappAccessTokenEncrypted() {
        return whatsappAccessTokenEncrypted;
    }

    public void releaseWhatsAppNumber() {
        this.whatsappPhoneNumberId = null;
        this.whatsappBusinessAccountId = null;
        this.whatsappDisplayPhone = null;
        this.whatsappAccessTokenEncrypted = null;
        this.whatsappConnectionStatus = "DISCONNECTED";
        this.whatsappConnectedAt = null;
    }
}
