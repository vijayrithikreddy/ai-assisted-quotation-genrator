package com.vlg.quotation.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Custom-number candidate plus known used numbers. Blank candidates report unusable, not a 400. */
public record CustomNumberValidationRequest(
        @JsonProperty("candidateNumber") String candidateNumber,
        @JsonProperty("usedNumbers")
                @NotNull(message = "usedNumbers is required")
                @Size(max = 100000, message = "usedNumbers exceeds the supported lookup size")
                List<String> usedNumbers) {
}
