package com.vlg.quotation.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlg.quotation.extraction.raw.RawExtractionResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class ExtractionResponseParserTest {

    private final ExtractionResponseParser parser =
            new ExtractionResponseParser(new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void shouldParseValidPayload() {
        RawExtractionResponse parsedResponse = parser.parse(minimalRawJson());

        assertThat(parsedResponse.workflow()).isEqualTo("NEW_CUSTOMER");
        assertThat(parsedResponse.items()).hasSize(1);
    }

    @Test
    void shouldStripCodeFences() {
        RawExtractionResponse parsedResponse = parser.parse("```json\n" + minimalRawJson() + "\n```");

        assertThat(parsedResponse.items()).hasSize(1);
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not json")).isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectEmptyPayload() {
        assertThatThrownBy(() -> parser.parse("   ")).isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectUnknownConfidence() {
        assertThatThrownBy(() -> parser.parse(minimalRawJson().replaceFirst("\"confidence\": \"HIGH\"", "\"confidence\": \"GUESSY\"")))
                .isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectEmptyItemList() {
        String itemsBlock = minimalRawJson().substring(
                minimalRawJson().indexOf("\"items\": ["), minimalRawJson().indexOf("],", minimalRawJson().indexOf("\"items\": [")));
        assertThatThrownBy(() -> parser.parse(minimalRawJson().replace(itemsBlock, "\"items\": []")))
                .isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectHumanReviewOptOut() {
        assertThatThrownBy(() -> parser.parse(minimalRawJson().replace("\"requiresHumanReview\": true", "\"requiresHumanReview\": false")))
                .isInstanceOf(LlmBadResponseException.class);
    }

    private String minimalRawJson() {
        return """
                {
                  "workflow": "NEW_CUSTOMER",
                  "sourceQuality": {"status": "USABLE", "issues": []},
                  "customer": {"companyName": {"value": "ACME", "confidence": "HIGH", "reviewReason": null}},
                  "items": [
                    {
                      "sourceOrder": 1,
                      "productName": {"value": "Gear", "confidence": "HIGH", "reviewReason": null},
                      "specifications": {"value": null, "confidence": "LOW", "reviewReason": null},
                      "quantity": {"value": "1", "confidence": "HIGH", "reviewReason": null},
                      "unit": {"value": "NOS", "confidence": "HIGH", "reviewReason": null},
                      "unitPrice": {"value": "10", "confidence": "HIGH", "reviewReason": null}
                    }
                  ],
                  "conflicts": [],
                  "requiresHumanReview": true
                }
                """;
    }
}
