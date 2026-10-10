package com.vlg.quotation.extraction;

/** The provider cannot serve the request right now. Mapped to 503. Message must stay user-safe. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
