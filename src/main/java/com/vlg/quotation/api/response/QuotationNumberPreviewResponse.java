package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Formatted preview. Previews never reserve a number. */
public record QuotationNumberPreviewResponse(
        @JsonProperty("formattedNumber") String formattedNumber,
        @JsonProperty("isPreviewOnly") boolean previewOnly) {
}
