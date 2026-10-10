package com.vlg.quotation.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Preview inputs plus the client's known used numbers for skip-over-used lookup. */
public record NextAvailableNumberRequest(
        @JsonProperty("prefix")
                @NotNull(message = "prefix is required")
                @Pattern(regexp = "^[A-Za-z0-9]{1,12}$", message = "prefix must be 1 to 12 letters or digits")
                String prefix,
        @JsonProperty("businessYear")
                @NotNull(message = "businessYear is required")
                @Pattern(regexp = "^[0-9]{4}$", message = "businessYear must be a four-digit year from Settings")
                String businessYear,
        @JsonProperty("nextSequence") @NotNull(message = "nextSequence is required") @Min(value = 1, message = "nextSequence must be 1 or greater")
                Long nextSequence,
        @JsonProperty("sequencePadding")
                @NotNull(message = "sequencePadding is required")
                @Min(value = 1, message = "sequencePadding must be between 1 and 6")
                @Max(value = 6, message = "sequencePadding must be between 1 and 6")
                Integer sequencePadding,
        @JsonProperty("usedNumbers")
                @NotNull(message = "usedNumbers is required")
                @Size(max = 100000, message = "usedNumbers exceeds the supported lookup size")
                List<String> usedNumbers) {
}
