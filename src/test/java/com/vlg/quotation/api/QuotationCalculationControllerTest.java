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
class QuotationCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCalculateReferenceExample() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cgstRate": 9,
                                  "sgstRate": 9,
                                  "items": [
                                    {"productName": "Duplex Sprocket EN19", "quantity": 3, "unit": "PCS", "unitPrice": 1700},
                                    {"productName": "Sprocket EN19", "quantity": 1, "unit": "PCS", "unitPrice": 4000}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].amount").value(5100))
                .andExpect(jsonPath("$.items[1].amount").value(4000))
                .andExpect(jsonPath("$.subtotal").value(9100))
                .andExpect(jsonPath("$.cgstRate").value(9))
                .andExpect(jsonPath("$.cgstAmount").value(819))
                .andExpect(jsonPath("$.sgstRate").value(9))
                .andExpect(jsonPath("$.sgstAmount").value(819))
                .andExpect(jsonPath("$.grandTotal").value(10738))
                .andExpect(jsonPath("$.amountInWords").value("Ten Thousand Seven Hundred Thirty Eight Only"));
    }

    @Test
    void shouldRejectZeroQuantityWithStableError() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cgstRate": 9,
                                  "sgstRate": 9,
                                  "items": [
                                    {"productName": "Gear", "quantity": 0, "unit": "NOS", "unitPrice": 100}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void shouldRejectNegativeUnitPriceWithStableError() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cgstRate": 9,
                                  "sgstRate": 9,
                                  "items": [
                                    {"productName": "Gear", "quantity": 1, "unit": "NOS", "unitPrice": -5}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void shouldRejectEmptyItemsWithStableError() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cgstRate": 9,
                                  "sgstRate": 9,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
