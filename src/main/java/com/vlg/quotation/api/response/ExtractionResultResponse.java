package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlg.quotation.extraction.schema.ExtractionWarning;
import com.vlg.quotation.extraction.schema.QuotationExtractionResponse;
import java.util.List;

/**
 * Extraction result for human review. {@code status} is always
 * {@code COMPLETED} here; failures use the stable error contract instead.
 * Totals in the draft are never authoritative.
 */
public record ExtractionResultResponse(
        @JsonProperty("extractionId") String extractionId,
        @JsonProperty("status") String status,
        @JsonProperty("modelIdentifier") String modelIdentifier,
        @JsonProperty("promptVersion") String promptVersion,
        @JsonProperty("schemaVersion") String schemaVersion,
        @JsonProperty("draft") QuotationExtractionResponse draft,
        @JsonProperty("warnings") List<ExtractionWarning> warnings) {
}
