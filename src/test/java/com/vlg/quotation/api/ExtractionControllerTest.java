package com.vlg.quotation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vlg.quotation.extraction.FakeExtractionProvider;
import com.vlg.quotation.extraction.LlmUnavailableException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"llm.provider=test-fake", "llm.api-key=test-key"})
@AutoConfigureMockMvc
class ExtractionControllerTest {

    @TestConfiguration
    static class FakeProviderConfiguration {
        @Bean
        FakeExtractionProvider fakeExtractionProvider() {
            return new FakeExtractionProvider();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeExtractionProvider fakeExtractionProvider;

    @BeforeEach
    void stageSuccessPayload() {
        fakeExtractionProvider.stagePayload(successfulRawJson());
    }

    @Test
    void shouldReturnReviewDraftForNewCustomer() throws Exception {
        MockMultipartFile quotationImage = new MockMultipartFile(
                "quotationImage", "quote.png", "image/png", decodableImageBytes());
        MockMultipartFile supportingDocument = new MockMultipartFile(
                "supportingDocument", "challan.pdf", "application/pdf", "%PDF-1.4 fake".getBytes());

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(quotationImage)
                        .file(supportingDocument)
                        .param("customerMode", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractionId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelIdentifier").value("gemini-3.8-flash"))
                .andExpect(jsonPath("$.promptVersion").value("v1"))
                .andExpect(jsonPath("$.schemaVersion").value("v1"))
                .andExpect(jsonPath("$.draft.customer.companyName.value").value("EXEL RUBBER PVT. LTD."))
                .andExpect(jsonPath("$.draft.customer.companyName.status").value("CONFIDENT"))
                .andExpect(jsonPath("$.draft.items[0].productName.value").value("P.B WORM GEAR"))
                .andExpect(jsonPath("$.draft.items[0].unitPrice.status").value("MISSING"))
                .andExpect(jsonPath("$.draft.items[0].unitPrice.value").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.warnings").isArray());

        assertThat(fakeExtractionProvider.lastCall().supportingDocument().contentType()).isEqualTo("application/pdf");
        assertThat(fakeExtractionProvider.lastCall().promptText()).contains("NEW_CUSTOMER");
    }

    @Test
    void shouldUseSavedRecordWarningForExistingCustomer() throws Exception {
        fakeExtractionProvider.stagePayload(successfulRawJson().replace("NEW_CUSTOMER", "EXISTING_CUSTOMER"));

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .param("customerMode", "EXISTING")
                        .param("customerId", "cust-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.customer.companyName.value").value("EXEL RUBBER PVT. LTD."))
                .andExpect(jsonPath("$.warnings.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.warnings[?(@.code == 'CUSTOMER_FROM_SAVED_RECORD')]").isNotEmpty());
    }

    @Test
    void shouldRejectExistingModeWithoutCustomerId() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .param("customerMode", "EXISTING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void shouldRejectUnsupportedImageType() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile("quotationImage", "quote.txt", "text/plain", "hello".getBytes()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldAcceptRealImageDespiteWrongDeclaredType() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.bin", "application/octet-stream", decodableImageBytes()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(fakeExtractionProvider.lastCall().primaryImage().contentType()).isEqualTo("image/png");
    }

    @Test
    void shouldRejectUndecodableImage() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.jpg", "image/jpeg", new byte[] {1, 2, 3, 4}))
                        .param("customerMode", "NEW"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMAGE_UNREADABLE"));
    }

    @Test
    void shouldRejectWordSupportingDocumentClearly() throws Exception {
        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .file(new MockMultipartFile(
                                "supportingDocument",
                                "notes.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                new byte[] {1, 2, 3}))
                        .param("customerMode", "NEW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Word")));
    }

    @Test
    void shouldMapMalformedPayloadToBadResponse() throws Exception {
        fakeExtractionProvider.stagePayload("{not json");

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_BAD_RESPONSE"));
    }

    @Test
    void shouldMapUnparseableMoneyToBadResponse() throws Exception {
        fakeExtractionProvider.stagePayload(successfulRawJson().replace(
                "\"quantity\": {\"value\": \"1\"",
                "\"quantity\": {\"value\": \"two\"")); 

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_BAD_RESPONSE"));
    }

    @Test
    void shouldMapProviderOutageToUnavailable() throws Exception {
        fakeExtractionProvider.stageFailure(new LlmUnavailableException("AI extraction is temporarily unavailable."));

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", decodableImageBytes()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"));
    }

    private byte[] decodableImageBytes() throws Exception {
        BufferedImage testImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGraphics = testImage.createGraphics();
        imageGraphics.setColor(Color.WHITE);
        imageGraphics.fillRect(0, 0, 10, 10);
        imageGraphics.dispose();
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", imageBytes);
        return imageBytes.toByteArray();
    }

    private String successfulRawJson() {
        return """
                {
                  "workflow": "NEW_CUSTOMER",
                  "sourceQuality": {"status": "USABLE_WITH_REVIEW", "issues": ["Shadow over the rate."]},
                  "customer": {
                    "companyName": {"value": "EXEL RUBBER PVT. LTD.", "confidence": "HIGH", "reviewReason": null},
                    "addressLine1": {"value": "SY NO 107/A", "confidence": "MEDIUM", "reviewReason": "Handwriting unclear."},
                    "area": {"value": "Cheriyal Road", "confidence": "HIGH", "reviewReason": null},
                    "state": {"value": "Telangana", "confidence": "HIGH", "reviewReason": null},
                    "pinCode": {"value": "502285", "confidence": "HIGH", "reviewReason": null},
                    "gstin": {"value": null, "confidence": "LOW", "reviewReason": "No GSTIN found."}
                  },
                  "items": [
                    {
                      "sourceOrder": 1,
                      "productName": {"value": "P.B WORM GEAR", "confidence": "HIGH", "reviewReason": null},
                      "specifications": {"value": "126 TEETH", "confidence": "MEDIUM", "reviewReason": null},
                      "quantity": {"value": "1", "confidence": "HIGH", "reviewReason": null},
                      "unit": {"value": "NOS", "confidence": "HIGH", "reviewReason": null},
                      "unitPrice": {"value": null, "confidence": "LOW", "reviewReason": "Rate area cut off."}
                    }
                  ],
                  "conflicts": [],
                  "requiresHumanReview": true
                }
                """;
    }
}
