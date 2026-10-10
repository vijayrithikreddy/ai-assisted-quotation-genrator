package com.vlg.quotation.extraction;

/** The source image cannot be used for extraction. Mapped to 422 with a retake request. */
public class UnreadableImageException extends RuntimeException {

    public UnreadableImageException(String message) {
        super(message);
    }
}
