package com.vlg.quotation.extraction.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Phase 2 acceptance: schema parsing is stable without external AI calls.
 * Malformed model output is rejected; sparse output normalizes to MISSING
 * for human review instead of invented values.
 */
@SpringBootTest
class ExtractionSchemaValidationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @Test
    void shouldParseValidSingleItemResponse() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(validSingleItemJson(), QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isEmpty();
        assertThat(parsedResponse.schemaVersion()).isEqualTo("v1");
        assertThat(parsedResponse.customer().companyName().value()).isEqualTo("EXEL RUBBER PVT. LTD.");
        assertThat(parsedResponse.items()).hasSize(1);
        assertThat(parsedResponse.items().get(0).productName().value()).isEqualTo("P.B WORM GEAR");
        assertThat(parsedResponse.items().get(0).quantity().value()).isEqualByComparingTo("1");
        assertThat(parsedResponse.warnings()).isEmpty();
    }

    @Test
    void shouldParseMultipleItems() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(twoItemJson(), QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isEmpty();
        assertThat(parsedResponse.items()).hasSize(2);
        assertThat(parsedResponse.items().get(1).productName().value()).isEqualTo("Sprocket EN19");
    }

    @Test
    void shouldNormalizeAbsentFieldsToMissing() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(sparseJson(), QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isEmpty();
        assertThat(parsedResponse.customer().addressLine2().isMissing()).isTrue();
        assertThat(parsedResponse.customer().district().isMissing()).isTrue();
        assertThat(parsedResponse.items().get(0).unitPrice().isMissing()).isTrue();
        assertThat(parsedResponse.items().get(0).unitPrice().value()).isNull();
    }

    @Test
    void shouldRejectNonNumericQuantityText() {
        String quantityAsWordJson = """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": "three", "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(quantityAsWordJson, QuotationExtractionResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectBareStringQuantity() {
        String bareStringQuantityJson = """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": "one",
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(bareStringQuantityJson, QuotationExtractionResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectUnknownStatus() {
        String unknownStatusJson = """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "GUESSY"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(unknownStatusJson, QuotationExtractionResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectEmptyItemList() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": []
                }
                """,
                QuotationExtractionResponse.class);

        assertThat(violationMessages(validator.validate(parsedResponse)))
                .anyMatch(message -> message.contains("at least one item"));
    }

    @Test
    void shouldRejectMissingStatusWithValue() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "MISSING"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """,
                QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isNotEmpty();
    }

    @Test
    void shouldRejectConfidentStatusWithoutValue() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": null, "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """,
                QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isNotEmpty();
    }

    @Test
    void shouldRejectModelSuppliedAmount() {
        String amountFromModelJson = """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"},
                      "amount": 40000
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(amountFromModelJson, QuotationExtractionResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                validSingleItemJson().replace("\"schemaVersion\": \"v1\"", "\"schemaVersion\": \"v2\""),
                QuotationExtractionResponse.class);

        assertThat(violationMessages(validator.validate(parsedResponse)))
                .anyMatch(message -> message.contains("unsupported schemaVersion"));
    }

    @Test
    void shouldRejectMissingCustomer() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                """
                {
                  "schemaVersion": "v1",
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """,
                QuotationExtractionResponse.class);

        assertThat(validator.validate(parsedResponse)).isNotEmpty();
    }

    @Test
    void shouldParseWarningsAndRejectBlankWarningCode() throws Exception {
        QuotationExtractionResponse parsedResponse = objectMapper.readValue(
                """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 40000, "status": "UNCERTAIN"}
                    }
                  ],
                  "warnings": [
                    {"code": "UNIT_PRICE_UNCERTAIN", "message": "Rate handwriting is unclear."},
                    {"code": "", "message": "Blank code."}
                  ]
                }
                """,
                QuotationExtractionResponse.class);

        assertThat(parsedResponse.warnings()).hasSize(2);
        assertThat(validator.validate(parsedResponse)).isNotEmpty();
    }

    private Set<String> violationMessages(Set<ConstraintViolation<QuotationExtractionResponse>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private String validSingleItemJson() {
        return """
                {
                  "schemaVersion": "v1",
                  "customer": {
                    "companyName": {"value": "EXEL RUBBER PVT. LTD.", "status": "CONFIDENT"},
                    "addressLine1": {"value": "SY NO 107/A, AA, B, BB 108/A", "status": "UNCERTAIN"},
                    "addressLine2": {"value": null, "status": "MISSING"},
                    "area": {"value": "Cheriyal Road", "status": "CONFIDENT"},
                    "district": {"value": null, "status": "MISSING"},
                    "state": {"value": "Telangana", "status": "CONFIDENT"},
                    "pin": {"value": "502285", "status": "CONFIDENT"},
                    "gstin": {"value": "36AAACE4495J1ZD", "status": "UNCERTAIN"}
                  },
                  "items": [
                    {
                      "productName": {"value": "P.B WORM GEAR", "status": "CONFIDENT"},
                      "specifications": {"value": "\\u00d8662 \\u00d7 46\\n126 TEETH", "status": "UNCERTAIN"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 210000, "status": "UNCERTAIN"}
                    }
                  ],
                  "warnings": []
                }
                """;
    }

    private String twoItemJson() {
        return """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "SPARK ADHESIVES", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Duplex Sprocket EN19", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 3, "status": "CONFIDENT"},
                      "unit": {"value": "PCS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 1700, "status": "CONFIDENT"}
                    },
                    {
                      "productName": {"value": "Sprocket EN19", "status": "CONFIDENT"},
                      "specifications": {"value": null, "status": "MISSING"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "PCS", "status": "CONFIDENT"},
                      "unitPrice": {"value": 4000, "status": "CONFIDENT"}
                    }
                  ]
                }
                """;
    }

    private String sparseJson() {
        return """
                {
                  "schemaVersion": "v1",
                  "customer": {"companyName": {"value": "ACME", "status": "CONFIDENT"}},
                  "items": [
                    {
                      "productName": {"value": "Worm Wheel", "status": "CONFIDENT"},
                      "quantity": {"value": 1, "status": "CONFIDENT"},
                      "unit": {"value": "NOS", "status": "CONFIDENT"}
                    }
                  ]
                }
                """;
    }
}
