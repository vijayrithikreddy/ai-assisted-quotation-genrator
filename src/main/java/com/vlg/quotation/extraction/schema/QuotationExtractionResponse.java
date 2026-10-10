package com.vlg.quotation.extraction.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Top-level structured contract produced by the multimodal LLM.
 *
 * <p>Carries facts only: customer details, item facts, and warnings.
 * Financial totals, GST, and amount-in-words are intentionally absent;
 * accepting them from the model would violate the AI boundary.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record QuotationExtractionResponse(
        @JsonProperty("schemaVersion")
                @NotBlank(message = "schemaVersion is required")
                @Pattern(regexp = "v1", message = "unsupported schemaVersion; expected v1")
                String schemaVersion,
        @JsonProperty("customer") @Valid @NotNull(message = "customer is required")
                ExtractedCustomerDetails customer,
        @JsonProperty("items") @Valid @NotNull(message = "items are required")
                @Size(min = 1, message = "at least one item is required")
                List<@Valid ExtractedQuotationItem> items,
        @JsonProperty("warnings") @Valid List<@Valid ExtractionWarning> warnings) {

    public QuotationExtractionResponse {
        items = items != null ? List.copyOf(items) : null;
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }
}
