package com.vlg.quotation.extraction;

/** The provider answered, but the payload was empty, malformed, or off-contract. Mapped to 502. */
public class LlmBadResponseException extends RuntimeException {

    public LlmBadResponseException(String message) {
        super(message);
    }

    public LlmBadResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
