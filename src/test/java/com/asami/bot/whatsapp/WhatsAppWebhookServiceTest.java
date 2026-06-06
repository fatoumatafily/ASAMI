package com.asami.bot.whatsapp;

import com.asami.bot.catalog.CommercialReplyService;
import com.asami.bot.audio.OpenAiTranscriptionService;
import com.asami.bot.audio.TranscriptionResult;
import com.asami.bot.conversation.Conversation;
import com.asami.bot.conversation.ConversationRepository;
import com.asami.bot.conversation.Message;
import com.asami.bot.conversation.MessageRepository;
import com.asami.bot.seller.Seller;
import com.asami.bot.seller.SellerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppWebhookServiceTest {

    private final SellerRepository sellerRepository = mock(SellerRepository.class);
    private final ConversationRepository conversationRepository =
            mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final WhatsAppClient whatsAppClient = mock(WhatsAppClient.class);
    private final CommercialReplyService commercialReplyService =
            mock(CommercialReplyService.class);
    private final OpenAiTranscriptionService transcriptionService =
            mock(OpenAiTranscriptionService.class);
    private final WhatsAppWebhookService service = new WhatsAppWebhookService(
            new ObjectMapper(),
            sellerRepository,
            conversationRepository,
            messageRepository,
            whatsAppClient,
            commercialReplyService,
            transcriptionService
    );

    @Test
    void storesIncomingTextMessage() {
        Seller seller = mock(Seller.class);
        Conversation conversation = mock(Conversation.class);
        UUID sellerId = UUID.randomUUID();
        when(seller.getId()).thenReturn(sellerId);
        when(sellerRepository.findByWhatsappPhoneNumberId("1144015498802314"))
                .thenReturn(Optional.of(seller));
        when(messageRepository.existsByWhatsappMessageId("wamid.incoming"))
                .thenReturn(false);
        when(conversationRepository.findBySellerIdAndCustomerPhone(
                sellerId,
                "221783802344"
        )).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(commercialReplyService.reply(seller, "Bonjour ASAMI"))
                .thenReturn("Reponse commerciale");
        when(whatsAppClient.sendText(
                "1144015498802314",
                "221783802344",
                "Reponse commerciale"
        )).thenReturn("wamid.outgoing");

        service.process(textPayload());

        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(Message.class));
        verify(whatsAppClient).sendText(
                "1144015498802314",
                "221783802344",
                "Reponse commerciale"
        );
    }

    @Test
    void ignoresMessageAlreadyStored() {
        Seller seller = mock(Seller.class);
        when(sellerRepository.findByWhatsappPhoneNumberId("1144015498802314"))
                .thenReturn(Optional.of(seller));
        when(messageRepository.existsByWhatsappMessageId("wamid.incoming"))
                .thenReturn(true);

        service.process(textPayload());

        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
        verify(whatsAppClient, never()).sendText(any(), any(), any());
    }

    @Test
    void transcribesAudioAndRepliesFromCatalog() {
        Seller seller = mock(Seller.class);
        Conversation conversation = mock(Conversation.class);
        Message audioMessage = mock(Message.class);
        UUID sellerId = UUID.randomUUID();
        when(seller.getId()).thenReturn(sellerId);
        when(sellerRepository.findByWhatsappPhoneNumberId("1144015498802314"))
                .thenReturn(Optional.of(seller));
        when(messageRepository.existsByWhatsappMessageId("wamid.audio"))
                .thenReturn(false);
        when(conversationRepository.findBySellerIdAndCustomerPhone(
                sellerId,
                "221783802344"
        )).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(audioMessage);
        WhatsAppMedia media = new WhatsAppMedia(new byte[]{1, 2, 3}, "audio/ogg");
        when(whatsAppClient.downloadMedia(
                "1144015498802314",
                "media-123"
        )).thenReturn(media);
        when(transcriptionService.transcribe(media))
                .thenReturn(new TranscriptionResult(
                        "Am nga chaussures ?",
                        "wo"
                ));
        when(commercialReplyService.reply(seller, "Am nga chaussures ?", "wo"))
                .thenReturn("Chaussures coute 25000 XOF. En stock : 4.");
        when(whatsAppClient.sendText(
                "1144015498802314",
                "221783802344",
                "Chaussures coute 25000 XOF. En stock : 4."
        )).thenReturn("wamid.audio-reply");

        service.process(audioPayload());

        verify(audioMessage).applyTranscription("Am nga chaussures ?", "wo");
        verify(whatsAppClient).sendText(
                "1144015498802314",
                "221783802344",
                "Chaussures coute 25000 XOF. En stock : 4."
        );
    }

    private String textPayload() {
        return """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "metadata": {
                          "phone_number_id": "1144015498802314"
                        },
                        "messages": [{
                          "from": "221783802344",
                          "id": "wamid.incoming",
                          "type": "text",
                          "text": {
                            "body": "Bonjour ASAMI"
                          }
                        }]
                      }
                    }]
                  }]
                }
                """;
    }

    private String audioPayload() {
        return """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "metadata": {
                          "phone_number_id": "1144015498802314"
                        },
                        "messages": [{
                          "from": "221783802344",
                          "id": "wamid.audio",
                          "type": "audio",
                          "audio": {
                            "id": "media-123",
                            "mime_type": "audio/ogg"
                          }
                        }]
                      }
                    }]
                  }]
                }
                """;
    }
}
