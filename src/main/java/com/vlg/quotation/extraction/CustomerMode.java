package com.vlg.quotation.extraction;

/**
 * Explicit workflow choice made by the user. Never inferred from image text.
 */
public enum CustomerMode {
    NEW,
    EXISTING;

    /**
     * Parses client values leniently: {@code NEW} and {@code NEW_CUSTOMER}
     * both mean new-customer extraction, likewise for existing.
     */
    public static CustomerMode fromRequestValue(String requestValue) {
        String normalizedValue = requestValue == null ? "" : requestValue.trim().toUpperCase();
        return switch (normalizedValue) {
            case "NEW", "NEW_CUSTOMER" -> NEW;
            case "EXISTING", "EXISTING_CUSTOMER" -> EXISTING;
            default -> throw new IllegalArgumentException("customerMode must be NEW or EXISTING.");
        };
    }

    /** Workflow token sent to the provider, per {@code docs/llm_config.md}. */
    public String providerWorkflowName() {
        return this == NEW ? "NEW_CUSTOMER" : "EXISTING_CUSTOMER";
    }
}
