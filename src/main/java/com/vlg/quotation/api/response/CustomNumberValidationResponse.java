package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Custom-number verdict. {@code unusableReason} is null when the number is usable. */
public record CustomNumberValidationResponse(
        @JsonProperty("normalizedNumber") String normalizedNumber,
        @JsonProperty("usable") boolean usable,
        @JsonProperty("unusableReason") String unusableReason) {
}
