package com.vlg.quotation.api.controller;

import com.vlg.quotation.api.response.CapabilitiesResponse;
import com.vlg.quotation.configuration.FileUploadProperties;
import com.vlg.quotation.configuration.LlmProperties;
import com.vlg.quotation.configuration.PdfProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports externally safe configuration state so the Android client can
 * adapt without a rebuild. Never exposes the LLM API key.
 */
@Tag(name = "System", description = "Health and capability probes")
@RestController
@RequestMapping(path = "/api/v1/configuration/capabilities", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigurationCapabilitiesController {

    private static final String SERVICE_NAME = "vlg-quotation-api";

    private final LlmProperties llmProperties;
    private final FileUploadProperties fileUploadProperties;
    private final PdfProperties pdfProperties;

    public ConfigurationCapabilitiesController(
            LlmProperties llmProperties,
            FileUploadProperties fileUploadProperties,
            PdfProperties pdfProperties) {
        this.llmProperties = llmProperties;
        this.fileUploadProperties = fileUploadProperties;
        this.pdfProperties = pdfProperties;
    }

    @Operation(summary = "Report configured capabilities without secrets")
    @GetMapping
    public CapabilitiesResponse reportCapabilities() {
        boolean llmConfigured = llmProperties.isConfigured();
        String message = llmConfigured
                ? "AI extraction is configured."
                : "AI extraction is not configured. Manual entry remains available.";
        return new CapabilitiesResponse(
                SERVICE_NAME,
                llmConfigured,
                llmProperties.getProvider(),
                llmProperties.getExtractionPromptVersion(),
                llmProperties.getExtractionSchemaVersion(),
                pdfProperties.getTemplateVersion(),
                fileUploadProperties.getMaxImageSizeBytes(),
                fileUploadProperties.getMaxSupportingDocumentSizeBytes(),
                fileUploadProperties.getAllowedSupportingDocumentTypes(),
                message);
    }
}
