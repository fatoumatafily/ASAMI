package com.asami.bot.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhatsAppTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "asami.whatsapp.test-endpoint-enabled=true")
class WhatsAppTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhatsAppClient whatsAppClient;

    @Test
    void sendsValidTextMessage() throws Exception {
        when(whatsAppClient.sendText("221771234567", "Bonjour"))
                .thenReturn("wamid.test-message");

        mockMvc.perform(post("/api/test/whatsapp/text")
                        .contentType("application/json")
                        .content("""
                                {
                                  "recipientPhone": "221771234567",
                                  "text": "Bonjour"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("wamid.test-message"))
                .andExpect(jsonPath("$.status").value("accepted"));

        verify(whatsAppClient).sendText("221771234567", "Bonjour");
    }

    @Test
    void rejectsPhoneNumberWithPlusPrefix() throws Exception {
        mockMvc.perform(post("/api/test/whatsapp/text")
                        .contentType("application/json")
                        .content("""
                                {
                                  "recipientPhone": "+221771234567",
                                  "text": "Bonjour"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendsHelloWorldTemplate() throws Exception {
        when(whatsAppClient.sendTemplate("221783802344", "hello_world", "en_US"))
                .thenReturn("wamid.template-message");

        mockMvc.perform(post("/api/test/whatsapp/hello-world")
                        .contentType("application/json")
                        .content("""
                                {
                                  "recipientPhone": "221783802344"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("wamid.template-message"))
                .andExpect(jsonPath("$.status").value("accepted"));

        verify(whatsAppClient).sendTemplate("221783802344", "hello_world", "en_US");
    }
}
