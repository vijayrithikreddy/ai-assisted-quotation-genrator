package com.vlg.quotation.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload limits and allowlist for quotation images and supporting documents.
 *
 * <p>V1 allows one optional supporting document of type PDF, image, DOC, or
 * DOCX. Exact MIME allowlist and size caps are configurable so business
 * review can tighten them without code changes.</p>
 */
@ConfigurationProperties(prefix = "upload")
public class FileUploadProperties {

    /** Maximum handwritten image size in bytes. Maps from {@code MAX_IMAGE_SIZE_BYTES}. */
    private long maxImageSizeBytes = 10_485_760L;

    /** Maximum supporting document size in bytes. Maps from {@code MAX_SUPPORTING_DOCUMENT_SIZE_BYTES}. */
    private long maxSupportingDocumentSizeBytes = 15_728_640L;

    /** MIME types accepted for the optional supporting document. */
    private List<String> allowedSupportingDocumentTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public long getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(long maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public long getMaxSupportingDocumentSizeBytes() {
        return maxSupportingDocumentSizeBytes;
    }

    public void setMaxSupportingDocumentSizeBytes(long maxSupportingDocumentSizeBytes) {
        this.maxSupportingDocumentSizeBytes = maxSupportingDocumentSizeBytes;
    }

    public List<String> getAllowedSupportingDocumentTypes() {
        return allowedSupportingDocumentTypes;
    }

    public void setAllowedSupportingDocumentTypes(List<String> allowedSupportingDocumentTypes) {
        this.allowedSupportingDocumentTypes = allowedSupportingDocumentTypes;
    }
}
