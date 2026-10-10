package com.vlg.quotation.extraction.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * One LLM-extracted value together with its review status.
 *
 * @param value extracted value, or null when the status is {@code MISSING}
 * @param status review hint; never silently treated as verified truth
 * @param reviewReason model-supplied review hint, or null
 */
@FieldConsistency
public record ExtractedField<T>(
        @JsonProperty("value") T value,
        @JsonProperty("status") @NotNull(message = "status is required") FieldStatus status,
        @JsonProperty("reviewReason") String reviewReason) {

    /** Factory for absent values. Absent JSON field objects normalize to this. */
    public static <T> ExtractedField<T> missing() {
        return new ExtractedField<>(null, FieldStatus.MISSING, null);
    }

    public boolean isMissing() {
        return status == FieldStatus.MISSING;
    }

    public boolean needsHumanReview() {
        return status != FieldStatus.CONFIDENT;
    }
}
