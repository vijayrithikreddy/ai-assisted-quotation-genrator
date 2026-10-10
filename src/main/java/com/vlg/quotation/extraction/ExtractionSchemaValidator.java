package com.vlg.quotation.extraction;

import com.vlg.quotation.extraction.schema.QuotationExtractionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Final safety net: validates the translated application draft against the
 * Phase 2 schema before it reaches human review.
 */
@Component
public class ExtractionSchemaValidator {

    private final Validator validator;

    public ExtractionSchemaValidator(Validator validator) {
        this.validator = validator;
    }

    /** Throws when the translated draft violates the application schema. */
    public void validateDraft(QuotationExtractionResponse validatedDraft) {
        Set<ConstraintViolation<QuotationExtractionResponse>> violations = validator.validate(validatedDraft);
        if (!violations.isEmpty()) {
            throw new LlmBadResponseException(
                    "The AI provider returned data outside the extraction contract. Please try again.");
        }
    }
}
