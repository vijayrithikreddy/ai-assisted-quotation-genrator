package com.vlg.quotation.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PDF template version selection. Rendering itself arrives in Phase 8.
 */
@ConfigurationProperties(prefix = "pdf")
public class PdfProperties {

    /** Active PDF template version. Maps from {@code PDF_TEMPLATE_VERSION}. */
    private String templateVersion = "v1";

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }
}
