package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Liveness response. Must work without AI credentials. */
public record HealthResponse(
        @JsonProperty("status") String status,
        @JsonProperty("service") String service) {
}
