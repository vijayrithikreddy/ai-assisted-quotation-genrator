package com.vlg.quotation.infrastructure.files;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceFileTypeDetectorTest {

    @Test
    void shouldSniffJpegSignature() {
        assertThat(SourceFileTypeDetector.resolveContentType(
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0},
                        "application/octet-stream"))
                .isEqualTo("image/jpeg");
    }

    @Test
    void shouldSniffPngSignature() {
        assertThat(SourceFileTypeDetector.resolveContentType(
                        new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}, null))
                .isEqualTo("image/png");
    }

    @Test
    void shouldSniffWebpSignature() {
        assertThat(SourceFileTypeDetector.resolveContentType(
                        new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, "text/plain"))
                .isEqualTo("image/webp");
    }

    @Test
    void shouldSniffPdfSignature() {
        assertThat(SourceFileTypeDetector.resolveContentType(
                        new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '4', 0, 0, 0, 0}, null))
                .isEqualTo("application/pdf");
    }

    @Test
    void shouldFallBackToDeclaredType() {
        assertThat(SourceFileTypeDetector.resolveContentType(new byte[] {1, 2, 3}, "image/png"))
                .isEqualTo("image/png");
    }

    @Test
    void shouldNormalizeAliases() {
        assertThat(SourceFileTypeDetector.normalizeDeclaredType("image/jpg")).isEqualTo("image/jpeg");
        assertThat(SourceFileTypeDetector.normalizeDeclaredType("image/x-png")).isEqualTo("image/png");
        assertThat(SourceFileTypeDetector.normalizeDeclaredType("text/plain")).isNull();
        assertThat(SourceFileTypeDetector.normalizeDeclaredType(null)).isNull();
    }
}
