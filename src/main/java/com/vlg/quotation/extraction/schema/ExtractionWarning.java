package com.vlg.quotation.extraction.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Machine-readable flag for ambiguous, conflicting, or incomplete input. */
public record ExtractionWarning(
        @JsonProperty("code") @NotBlank(message = "warning code is required") String code,
        @JsonProperty("message") @NotBlank(message = "warning message is required") String message) {
}
