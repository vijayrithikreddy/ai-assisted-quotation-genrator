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
 * Proves a configured API key flips the flag yet never appears in the
 * response body.
 */
@SpringBootTest(
        properties = {
            "llm.provider=gemini",
            "llm.api-key=test-secret-value-12345",
            "llm.model=test-vision-model"
        })
@AutoConfigureMockMvc
class ConfiguredCapabilitiesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldFlagConfiguredLlmWithoutExposingKey() throws Exception {
        mockMvc.perform(get("/api/v1/configuration/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.llmConfigured").value(true))
                .andExpect(jsonPath("$.llmProvider").value("gemini"))
                .andExpect(content().string(not(containsString("test-secret-value-12345"))));
    }
}
