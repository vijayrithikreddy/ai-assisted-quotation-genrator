package com.vlg.quotation.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Typed LLM provider configuration bound from environment variables.
 *
 * <p>Effective defaults live in {@code application.yml} (which follows
 * {@code docs/llm_config.md}); field initializers below are only fallbacks
 * when a key is absent from configuration entirely. The API key has no
 * default and stays out of the repository; it arrives only through the
 * backend runtime environment. The application must start without AI
 * credentials so health checks and manual entry keep working when AI is
 * unavailable.</p>
 */
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** Provider id, e.g. {@code google-gemini}. Maps from {@code LLM_PROVIDER}. */
    private String provider = "";

    /** Secret API key. Maps from {@code LLM_API_KEY}. Never logged or returned. */
    private String apiKey = "";

    /** Provider endpoint. Maps from {@code LLM_BASE_URL}. */
    private String baseUrl = "";

    /** Vision-capable model id. Maps from {@code LLM_MODEL}. */
    private String model = "";

    /** Provider API version, when applicable. Maps from {@code LLM_API_VERSION}. */
    private String apiVersion = "";

    /** Request timeout. Maps from {@code LLM_TIMEOUT_SECONDS}. */
    private Duration timeout = Duration.ofSeconds(60);

    /** Retry attempts on transient failures. Maps from {@code LLM_MAX_RETRIES}. */
    private int maxRetries = 2;

    /** Output token cap. Maps from {@code LLM_MAX_OUTPUT_TOKENS}. */
    private int maxOutputTokens = 4096;

    /** Sampling temperature. Maps from {@code LLM_TEMPERATURE}. */
    private double temperature = 0.0;

    /** Active prompt resource version. Maps from {@code LLM_EXTRACTION_PROMPT_VERSION}. */
    private String extractionPromptVersion = "v1";

    /** Active extraction schema version. Maps from {@code LLM_EXTRACTION_SCHEMA_VERSION}. */
    private String extractionSchemaVersion = "v1";

    public boolean isConfigured() {
        return StringUtils.hasText(provider)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(model);
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getExtractionPromptVersion() {
        return extractionPromptVersion;
    }

    public void setExtractionPromptVersion(String extractionPromptVersion) {
        this.extractionPromptVersion = extractionPromptVersion;
    }

    public String getExtractionSchemaVersion() {
        return extractionSchemaVersion;
    }

    public void setExtractionSchemaVersion(String extractionSchemaVersion) {
        this.extractionSchemaVersion = extractionSchemaVersion;
    }
}
