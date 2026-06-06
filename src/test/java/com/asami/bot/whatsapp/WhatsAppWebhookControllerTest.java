package com.asami.bot.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhatsAppWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "asami.whatsapp.verify-token=test-token")
class WhatsAppWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhatsAppWebhookService webhookService;

    @MockitoBean
    private WhatsAppSignatureVerifier signatureVerifier;

    @Test
    void returnsChallengeWhenVerificationTokenMatches() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", "test-token")
                        .queryParam("hub.challenge", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("123456"));
    }

    @Test
    void rejectsInvalidVerificationToken() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", "wrong-token")
                        .queryParam("hub.challenge", "123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsWhatsAppEvent() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType("application/json")
                        .content("""
                                {"object":"whatsapp_business_account","entry":[]}
                                """))
                .andExpect(status().isOk());

        verify(webhookService).process(any());
    }

    @Test
    void rejectsWhatsAppEventWithInvalidSignature() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

}
