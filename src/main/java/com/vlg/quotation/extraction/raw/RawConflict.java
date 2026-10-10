package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** One disagreement between sources, or between a source and the saved customer context. */
public record RawConflict(
        @JsonProperty("code") @NotBlank(message = "conflict code is required") String code,
        @JsonProperty("message") @NotBlank(message = "conflict message is required") String message,
        @JsonProperty("fields") List<String> fields) {

    public RawConflict {
        fields = fields != null ? List.copyOf(fields) : List.of();
    }
}
