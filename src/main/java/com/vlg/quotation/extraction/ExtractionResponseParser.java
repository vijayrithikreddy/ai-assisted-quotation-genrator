package com.vlg.quotation.extraction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlg.quotation.extraction.raw.RawExtractionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Parses provider text into the raw extraction contract and validates its
 * shape. Anything off-contract fails here, never downstream.
 */
@Component
public class ExtractionResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ExtractionResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /** Parses and shape-validates the raw payload. Never returns a bad draft. */
    public RawExtractionResponse parse(String draftPayload) {
        if (draftPayload == null || draftPayload.isBlank()) {
            throw new LlmBadResponseException("The AI provider returned an empty response. Please try again.");
        }
        RawExtractionResponse rawResponse;
        try {
            rawResponse = objectMapper.readValue(stripCodeFences(draftPayload), RawExtractionResponse.class);
        } catch (JsonProcessingException malformedPayload) {
            throw new LlmBadResponseException(
                    "The AI provider returned malformed data. Please try again.", malformedPayload);
        }
        Set<ConstraintViolation<RawExtractionResponse>> violations = validator.validate(rawResponse);
        if (!violations.isEmpty()) {
            throw new LlmBadResponseException(
                    "The AI provider returned data outside the extraction contract. Please try again.");
        }
        return rawResponse;
    }

    private String stripCodeFences(String draftPayload) {
        String trimmedPayload = draftPayload.trim();
        if (trimmedPayload.startsWith("```")) {
            int firstNewline = trimmedPayload.indexOf('\n');
            int closingFence = trimmedPayload.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                return trimmedPayload.substring(firstNewline + 1, closingFence).trim();
            }
        }
        return trimmedPayload;
    }
}
