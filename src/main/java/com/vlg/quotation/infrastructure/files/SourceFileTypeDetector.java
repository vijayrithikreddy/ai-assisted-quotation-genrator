package com.vlg.quotation.infrastructure.files;

/**
 * Detects file types from content signatures (magic bytes) instead of trusting
 * the uploader-supplied content-type header, which clients set inconsistently.
 * Office formats (DOC/DOCX) have no reliable short signature check here and
 * keep falling back to the declared type.
 */
public final class SourceFileTypeDetector {

    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_WEBP = "image/webp";
    public static final String APPLICATION_PDF = "application/pdf";

    private SourceFileTypeDetector() {
    }

    /**
     * Resolves the canonical content type: content signature first, declared
     * type (with common aliases normalized) second, null when neither works.
     */
    public static String resolveContentType(byte[] fileContent, String declaredType) {
        String sniffedType = sniffKnownType(fileContent);
        if (sniffedType != null) {
            return sniffedType;
        }
        return normalizeDeclaredType(declaredType);
    }

    /** Sniffs JPEG, PNG, WebP, and PDF signatures. Returns null when unknown. */
    public static String sniffKnownType(byte[] fileContent) {
        if (fileContent == null || fileContent.length < 12) {
            return null;
        }
        if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xD8 && fileContent[2] == (byte) 0xFF) {
            return IMAGE_JPEG;
        }
        if (fileContent[0] == (byte) 0x89
                && fileContent[1] == (byte) 0x50
                && fileContent[2] == (byte) 0x4E
                && fileContent[3] == (byte) 0x47) {
            return IMAGE_PNG;
        }
        if (fileContent[0] == 'R'
                && fileContent[1] == 'I'
                && fileContent[2] == 'F'
                && fileContent[3] == 'F'
                && fileContent[8] == 'W'
                && fileContent[9] == 'E'
                && fileContent[10] == 'B'
                && fileContent[11] == 'P') {
            return IMAGE_WEBP;
        }
        if (fileContent[0] == '%'
                && fileContent[1] == 'P'
                && fileContent[2] == 'D'
                && fileContent[3] == 'F'
                && fileContent[4] == '-') {
            return APPLICATION_PDF;
        }
        return null;
    }

    /** Normalizes declared types including widespread aliases. Returns null when unusable. */
    public static String normalizeDeclaredType(String declaredType) {
        if (declaredType == null) {
            return null;
        }
        String trimmedType = declaredType.trim().toLowerCase();
        int parameterSeparator = trimmedType.indexOf(';');
        if (parameterSeparator >= 0) {
            trimmedType = trimmedType.substring(0, parameterSeparator).trim();
        }
        return switch (trimmedType) {
            case IMAGE_JPEG, "image/jpg", "image/pjpeg" -> IMAGE_JPEG;
            case IMAGE_PNG, "image/x-png" -> IMAGE_PNG;
            case IMAGE_WEBP -> IMAGE_WEBP;
            case APPLICATION_PDF -> APPLICATION_PDF;
            case "application/msword" -> "application/msword";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> null;
        };
    }
}
