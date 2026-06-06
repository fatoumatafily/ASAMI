package com.asami.bot.whatsapp;

import com.asami.bot.catalog.CommercialReplyService;
import com.asami.bot.audio.AudioTranscriptionException;
import com.asami.bot.audio.OpenAiTranscriptionService;
import com.asami.bot.audio.TranscriptionResult;
import com.asami.bot.conversation.Conversation;
import com.asami.bot.conversation.ConversationRepository;
import com.asami.bot.conversation.Message;
import com.asami.bot.conversation.MessageRepository;
import com.asami.bot.seller.Seller;
import com.asami.bot.seller.SellerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppWebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppWebhookService.class);
    private final ObjectMapper objectMapper;
    private final SellerRepository sellerRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final WhatsAppClient whatsAppClient;
    private final CommercialReplyService commercialReplyService;
    private final OpenAiTranscriptionService transcriptionService;

    public WhatsAppWebhookService(
            ObjectMapper objectMapper,
            SellerRepository sellerRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WhatsAppClient whatsAppClient,
            CommercialReplyService commercialReplyService,
            OpenAiTranscriptionService transcriptionService
    ) {
        this.objectMapper = objectMapper;
        this.sellerRepository = sellerRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.whatsAppClient = whatsAppClient;
        this.commercialReplyService = commercialReplyService;
        this.transcriptionService = transcriptionService;
    }

    @Transactional
    public void process(String rawPayload) {
        // Meta retries webhooks when the response is slow. Heavy processing will move
        // to an asynchronous worker when message handling is implemented.
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            for (JsonNode entry : payload.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    processChange(change.path("value"));
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid WhatsApp webhook payload", exception);
        }
    }

    private void processChange(JsonNode value) {
        String phoneNumberId = value.path("metadata").path("phone_number_id").asText();
        if (phoneNumberId.isBlank()) {
            return;
        }
        Seller seller = sellerRepository.findByWhatsappPhoneNumberId(phoneNumberId)
                .orElseGet(() -> sellerRepository.save(
                        new Seller("ASAMI Test Seller", phoneNumberId)
                ));

        for (JsonNode message : value.path("messages")) {
            String type = message.path("type").asText();
            String customerPhone = message.path("from").asText();
            String messageId = message.path("id").asText();
            if (customerPhone.isBlank() || messageId.isBlank()
                    || messageRepository.existsByWhatsappMessageId(messageId)) {
                continue;
            }

            Conversation conversation = conversationRepository
                    .findBySellerIdAndCustomerPhone(seller.getId(), customerPhone)
                    .orElseGet(() -> conversationRepository.save(
                            new Conversation(seller, customerPhone)
                    ));

            if ("text".equals(type)) {
                String text = message.path("text").path("body").asText();
                messageRepository.save(new Message(
                        conversation,
                        messageId,
                        type,
                        text,
                        null
                ));
                LOGGER.info(
                        "WhatsApp text received: phoneNumberId={}, customer={}, messageId={}, text={}",
                        phoneNumberId,
                        customerPhone,
                        messageId,
                        text
                );
                String reply = commercialReplyService.reply(seller, text);
                sendAutomaticReply(
                        conversation,
                        phoneNumberId,
                        customerPhone,
                        reply
                );
            } else if ("audio".equals(type)) {
                String mediaId = message.path("audio").path("id").asText();
                Message audioMessage = messageRepository.save(new Message(
                        conversation,
                        messageId,
                        type,
                        null,
                        mediaId
                ));
                LOGGER.info(
                        "WhatsApp audio received: phoneNumberId={}, customer={}, messageId={}, mediaId={}",
                        phoneNumberId,
                        customerPhone,
                        messageId,
                        mediaId
                );
                processAudio(
                        seller,
                        conversation,
                        audioMessage,
                        phoneNumberId,
                        customerPhone,
                        mediaId
                );
            } else {
                messageRepository.save(new Message(
                        conversation,
                        messageId,
                        type.isBlank() ? "unknown" : type,
                        null,
                        null
                ));
                LOGGER.info(
                        "WhatsApp message received: phoneNumberId={}, customer={}, messageId={}, type={}",
                        phoneNumberId,
                        customerPhone,
                        messageId,
                        type
                );
            }
        }
    }

    private void processAudio(
            Seller seller,
            Conversation conversation,
            Message audioMessage,
            String phoneNumberId,
            String customerPhone,
            String mediaId
    ) {
        try {
            WhatsAppMedia media = whatsAppClient.downloadMedia(phoneNumberId, mediaId);
            TranscriptionResult transcription = transcriptionService.transcribe(media);
            audioMessage.applyTranscription(
                    transcription.text(),
                    transcription.language()
            );
            String reply = commercialReplyService.reply(
                    seller,
                    transcription.text(),
                    transcription.language()
            );
            sendAutomaticReply(
                    conversation,
                    phoneNumberId,
                    customerPhone,
                    reply
            );
            LOGGER.info(
                    "WhatsApp audio transcribed: customer={}, language={}, text={}",
                    customerPhone,
                    transcription.language(),
                    transcription.text()
            );
        } catch (WhatsAppApiException | AudioTranscriptionException exception) {
            LOGGER.error(
                    "Unable to process WhatsApp audio for customer={}: {}",
                    customerPhone,
                    exception.getMessage()
            );
            sendAutomaticReply(
                    conversation,
                    phoneNumberId,
                    customerPhone,
                    "Je n'ai pas pu comprendre ce vocal. "
                            + "Pouvez-vous le renvoyer ou ecrire votre demande ?"
            );
        }
    }

    private void sendAutomaticReply(
            Conversation conversation,
            String phoneNumberId,
            String customerPhone,
            String reply
    ) {
        try {
            String outboundMessageId = whatsAppClient.sendText(
                    phoneNumberId,
                    customerPhone,
                    reply
            );
            messageRepository.save(Message.outboundText(
                    conversation,
                    outboundMessageId,
                    reply
            ));
            LOGGER.info(
                    "Automatic WhatsApp reply sent: customer={}, messageId={}",
                    customerPhone,
                    outboundMessageId
            );
        } catch (WhatsAppApiException exception) {
            LOGGER.error(
                    "Unable to send automatic WhatsApp reply to customer={}: {}",
                    customerPhone,
                    exception.getMessage()
            );
        }
    }
}
