package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Reports which capabilities are currently configured.
 *
 * <p>Intentionally contains no secrets: the API key is never serialized,
 * only the derived {@code llmConfigured} flag.</p>
 */
public record CapabilitiesResponse(
        @JsonProperty("service") String service,
        @JsonProperty("llmConfigured") boolean llmConfigured,
        @JsonProperty("llmProvider") String llmProvider,
        @JsonProperty("extractionPromptVersion") String extractionPromptVersion,
        @JsonProperty("extractionSchemaVersion") String extractionSchemaVersion,
        @JsonProperty("pdfTemplateVersion") String pdfTemplateVersion,
        @JsonProperty("maxImageSizeBytes") long maxImageSizeBytes,
        @JsonProperty("maxSupportingDocumentSizeBytes") long maxSupportingDocumentSizeBytes,
        @JsonProperty("supportedSupportingDocumentTypes") List<String> supportedSupportingDocumentTypes,
        @JsonProperty("message") String message) {
}
