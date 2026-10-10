package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Stable error JSON returned for every API failure.
 *
 * <p>Never contains API keys, stack traces, or document contents.</p>
 */
public record ApiErrorResponse(
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("status") int status,
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("requestId") String requestId) {
}
