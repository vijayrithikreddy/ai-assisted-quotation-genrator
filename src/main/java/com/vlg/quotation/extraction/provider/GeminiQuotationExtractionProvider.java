package com.vlg.quotation.extraction.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlg.quotation.configuration.LlmProperties;
import com.vlg.quotation.extraction.LlmBadResponseException;
import com.vlg.quotation.extraction.LlmUnavailableException;
import com.vlg.quotation.extraction.QuotationExtractionProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Google Gemini adapter behind {@link QuotationExtractionProvider}. Speaks
 * the Generate Content REST API directly (no vendor SDK) and returns raw
 * JSON text for application-side parsing and validation.
 */
@Component
public class GeminiQuotationExtractionProvider implements QuotationExtractionProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuotationExtractionProvider.class);
    private static final String PROVIDER_ID = "google-gemini";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final long RETRY_BASE_DELAY_MILLIS = 500L;

    private final LlmProperties llmProperties;
    private final RestClient geminiClient;

    public GeminiQuotationExtractionProvider(LlmProperties llmProperties, RestClient.Builder clientBuilder) {
        this.llmProperties = llmProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration providerTimeout = llmProperties.getTimeout() == null ? Duration.ofSeconds(60) : llmProperties.getTimeout();
        requestFactory.setConnectTimeout(providerTimeout);
        requestFactory.setReadTimeout(providerTimeout);
        this.geminiClient = clientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public String fetchDraftPayload(ProviderCall providerCall) {
        if (!StringUtils.hasText(llmProperties.getApiKey())) {
            throw new LlmUnavailableException(
                    "AI extraction is not configured. Please enter the quotation details manually.");
        }
        if (!StringUtils.hasText(llmProperties.getBaseUrl()) || !StringUtils.hasText(llmProperties.getModel())) {
            throw new IllegalStateException("Gemini endpoint is misconfigured: base URL and model are required.");
        }

        String requestUrl = llmProperties.getBaseUrl().replaceAll("/+$", "")
                + "/" + llmProperties.getApiVersion()
                + "/models/" + llmProperties.getModel()
                + ":generateContent";
        GeminiRequest geminiRequest = buildRequest(providerCall);

        int totalAttempts = Math.max(1, 1 + llmProperties.getMaxRetries());
        long callStartMillis = System.currentTimeMillis();
        for (int attempt = 1; ; attempt++) {
            try {
                GeminiResponse geminiResponse = geminiClient
                        .post()
                        .uri(requestUrl)
                        .header(API_KEY_HEADER, llmProperties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(geminiRequest)
                        .retrieve()
                        .body(GeminiResponse.class);
                String draftPayload = extractText(geminiResponse);
                log.info(
                        "Gemini extraction ok model={} durationMs={} attempts={}",
                        llmProperties.getModel(),
                        System.currentTimeMillis() - callStartMillis,
                        attempt);
                return draftPayload;
            } catch (HttpClientErrorException.TooManyRequests rateLimited) {
                failOrRetry(attempt, totalAttempts, "rate-limited", statusOf(rateLimited), rateLimited);
            } catch (HttpServerErrorException providerFailure) {
                failOrRetry(attempt, totalAttempts, "provider-error", statusOf(providerFailure), providerFailure);
            } catch (ResourceAccessException transportFailure) {
                failOrRetry(attempt, totalAttempts, "transport-failure", "no-response", transportFailure);
            } catch (HttpClientErrorException clientFailure) {
                // 400/401/403/404 indicate key, model, or request problems: never worth retrying.
                log.warn(
                        "Gemini extraction rejected model={} category=client-error status={}",
                        llmProperties.getModel(),
                        statusOf(clientFailure));
                throw new LlmUnavailableException(
                        "AI extraction is unavailable. Please check the backend AI configuration or enter details manually.",
                        clientFailure);
            }
        }
    }

    private void failOrRetry(
            int attempt, int totalAttempts, String failureCategory, String upstreamStatus, RuntimeException failure) {
        if (attempt >= totalAttempts) {
            log.warn(
                    "Gemini extraction unavailable model={} category={} status={} attempts={}",
                    llmProperties.getModel(),
                    failureCategory,
                    upstreamStatus,
                    attempt);
            throw new LlmUnavailableException(
                    "AI extraction is temporarily unavailable. Please try again or enter details manually.", failure);
        }
        sleepBeforeRetry(attempt);
    }

    /** Upstream HTTP status for diagnostics. Bodies are never logged. */
    private String statusOf(HttpClientErrorException clientFailure) {
        return String.valueOf(clientFailure.getStatusCode().value());
    }

    /** Upstream HTTP status for diagnostics. Bodies are never logged. */
    private String statusOf(HttpServerErrorException providerFailure) {
        return String.valueOf(providerFailure.getStatusCode().value());
    }

    private void sleepBeforeRetry(int failedAttempt) {
        try {
            Thread.sleep(RETRY_BASE_DELAY_MILLIS * failedAttempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("AI extraction was interrupted. Please try again.", interrupted);
        }
    }

    private GeminiRequest buildRequest(ProviderCall providerCall) {
        List<Part> requestParts = new ArrayList<>();
        requestParts.add(new Part(providerCall.promptText(), null));
        requestParts.add(new Part(null, inlineFile(providerCall.primaryImage())));
        if (providerCall.supportingDocument() != null) {
            requestParts.add(new Part(null, inlineFile(providerCall.supportingDocument())));
        }
        return new GeminiRequest(
                List.of(new Content(requestParts)),
                new GenerationConfig(
                        llmProperties.getTemperature(), llmProperties.getMaxOutputTokens(), "application/json"));
    }

    private InlineData inlineFile(SourceFile sourceFile) {
        return new InlineData(sourceFile.contentType(), Base64.getEncoder().encodeToString(sourceFile.content()));
    }

    private String extractText(GeminiResponse geminiResponse) {
        if (geminiResponse == null || geminiResponse.candidates() == null || geminiResponse.candidates().isEmpty()) {
            throw new LlmBadResponseException("The AI provider returned an empty response. Please try again.");
        }
        StringBuilder responseText = new StringBuilder();
        for (Part responsePart : geminiResponse.candidates().get(0).content().parts()) {
            if (responsePart != null && StringUtils.hasText(responsePart.text())) {
                responseText.append(responsePart.text());
            }
        }
        String draftPayload = responseText.toString().trim();
        if (draftPayload.isEmpty()) {
            throw new LlmBadResponseException("The AI provider returned an empty response. Please try again.");
        }
        return draftPayload;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Content(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Part(String text, @JsonProperty("inline_data") InlineData inlineData) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record InlineData(
            @JsonProperty("mime_type") String mimeType, @JsonProperty("data") String base64Data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerationConfig(
            double temperature,
            int maxOutputTokens,
            @JsonProperty("responseMimeType") String responseMimeType) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Candidate(Content content) {
    }
}
