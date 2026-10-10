package com.vlg.quotation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QuotationNumberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPreviewFormattedNumber() throws Exception {
        mockMvc.perform(post("/api/v1/quotation-numbers/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prefix": "VLG", "businessYear": "2025", "nextSequence": 8, "sequencePadding": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedNumber").value("VLG-2025/008"))
                .andExpect(jsonPath("$.isPreviewOnly").value(true));
    }

    @Test
    void shouldRejectInvalidBusinessYear() throws Exception {
        mockMvc.perform(post("/api/v1/quotation-numbers/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prefix": "VLG", "businessYear": "25", "nextSequence": 8, "sequencePadding": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void shouldSkipUsedNumbers() throws Exception {
        mockMvc.perform(post("/api/v1/quotation-numbers/next-available")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prefix": "VLG",
                                  "businessYear": "2025",
                                  "nextSequence": 8,
                                  "sequencePadding": 3,
                                  "usedNumbers": ["VLG-2025/008"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(9))
                .andExpect(jsonPath("$.formattedNumber").value("VLG-2025/009"))
                .andExpect(jsonPath("$.isPreviewOnly").value(true));
    }

    @Test
    void shouldAcceptFreeCustomNumber() throws Exception {
        mockMvc.perform(post("/api/v1/quotation-numbers/validate-custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateNumber": "VLG-2025/100", "usedNumbers": ["VLG-2025/008"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.normalizedNumber").value("VLG-2025/100"))
                .andExpect(jsonPath("$.usable").value(true));
    }

    @Test
    void shouldFlagDuplicateCustomNumber() throws Exception {
        mockMvc.perform(post("/api/v1/quotation-numbers/validate-custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateNumber": "VLG-2025/008", "usedNumbers": ["VLG-2025/008"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usable").value(false))
                .andExpect(jsonPath("$.unusableReason").value("ALREADY_USED"));
    }
}
