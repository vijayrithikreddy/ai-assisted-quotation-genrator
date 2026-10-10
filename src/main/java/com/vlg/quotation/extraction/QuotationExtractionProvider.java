package com.vlg.quotation.extraction;

/**
 * Provider abstraction for multimodal extraction. Implementations keep every
 * provider-specific type (SDK, HTTP envelope) inside the adapter and return
 * only the raw JSON text, which the application parses and validates.
 */
public interface QuotationExtractionProvider {

    /** Provider id matched against the {@code llm.provider} setting, e.g. {@code google-gemini}. */
    String providerId();

    /**
     * Sends one extraction call and returns the raw JSON text.
     *
     * @throws LlmUnavailableException for timeouts, rate limits, auth/config failures, and provider outages
     * @throws LlmBadResponseException for empty or otherwise unusable provider payloads
     */
    String fetchDraftPayload(ProviderCall providerCall);

    /** Provider-agnostic call content: prompt plus file bytes. No SDK types. */
    record ProviderCall(String promptText, SourceFile primaryImage, SourceFile supportingDocument) {
        public ProviderCall {
            if (promptText == null || promptText.isBlank()) {
                throw new IllegalArgumentException("Extraction prompt is required.");
            }
            if (primaryImage == null) {
                throw new IllegalArgumentException("Primary quotation image is required.");
            }
        }
    }

    /** One file as bytes with its validated content type. */
    record SourceFile(String contentType, byte[] content) {
    }
}
