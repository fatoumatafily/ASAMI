package com.asami.bot.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "whatsapp_message_id", unique = true)
    private String whatsappMessageId;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "detected_language", length = 10)
    private String detectedLanguage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(
            Conversation conversation,
            String whatsappMessageId,
            String messageType,
            String textContent,
            String mediaId
    ) {
        this.conversation = conversation;
        this.whatsappMessageId = whatsappMessageId;
        this.direction = "INBOUND";
        this.messageType = messageType;
        this.textContent = textContent;
        this.mediaId = mediaId;
    }

    public static Message outboundText(
            Conversation conversation,
            String whatsappMessageId,
            String textContent
    ) {
        Message message = new Message();
        message.conversation = conversation;
        message.whatsappMessageId = whatsappMessageId;
        message.direction = "OUTBOUND";
        message.messageType = "text";
        message.textContent = textContent;
        return message;
    }

    public void applyTranscription(String transcription, String language) {
        this.textContent = transcription;
        this.detectedLanguage = language;
    }
}
