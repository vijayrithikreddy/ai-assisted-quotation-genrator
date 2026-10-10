package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** One provider-supplied field: value, confidence hint, and optional review reason. */
public record RawField<T>(
        @JsonProperty("value") T value,
        @JsonProperty("confidence") @NotNull(message = "confidence is required") RawConfidence confidence,
        @JsonProperty("reviewReason") String reviewReason) {
}
