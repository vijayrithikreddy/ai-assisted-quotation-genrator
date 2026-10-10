package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Raw provider payload exactly as the model returned it, before translation. */
public record RawExtractionResponse(
        @JsonProperty("workflow") @NotBlank(message = "workflow is required") String workflow,
        @JsonProperty("sourceQuality") @Valid @NotNull(message = "sourceQuality is required") RawSourceQuality sourceQuality,
        @JsonProperty("customer") @Valid RawCustomer customer,
        @JsonProperty("items")
                @Valid
                @NotNull(message = "items are required")
                @Size(min = 1, message = "at least one item is required")
                List<@Valid RawItem> items,
        @JsonProperty("conflicts") @Valid List<@Valid RawConflict> conflicts,
        @JsonProperty("requiresHumanReview")
                @NotNull(message = "requiresHumanReview is required")
                @AssertTrue(message = "requiresHumanReview must remain true")
                Boolean requiresHumanReview) {

    public RawExtractionResponse {
        items = items != null ? List.copyOf(items) : null;
        conflicts = conflicts != null ? List.copyOf(conflicts) : List.of();
    }
}
