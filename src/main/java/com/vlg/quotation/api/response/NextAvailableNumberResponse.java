package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** First free formatted number at or after the configured sequence. Lookup only; nothing reserved. */
public record NextAvailableNumberResponse(
        @JsonProperty("sequence") long sequence,
        @JsonProperty("formattedNumber") String formattedNumber,
        @JsonProperty("isPreviewOnly") boolean previewOnly) {
}
