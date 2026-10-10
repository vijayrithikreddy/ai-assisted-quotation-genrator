package com.vlg.quotation.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Capabilities must report configuration state without ever leaking the
 * LLM API key. Default environment has no AI credentials.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CapabilitiesEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReportUnconfiguredLlmWithoutSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/configuration/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("vlg-quotation-api"))
                .andExpect(jsonPath("$.llmConfigured").value(false))
                .andExpect(jsonPath("$.pdfTemplateVersion").value("v1"))
                .andExpect(jsonPath("$.maxImageSizeBytes").value(10485760))
                .andExpect(content().string(not(containsString("apiKey"))));
    }
}
