package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Model-reported usability of the supplied sources. */
public record RawSourceQuality(
        @JsonProperty("status") @NotNull(message = "sourceQuality.status is required") RawSourceStatus status,
        @JsonProperty("issues") List<String> issues) {

    public RawSourceQuality {
        issues = issues != null ? List.copyOf(issues) : List.of();
    }
}
