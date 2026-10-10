package com.vlg.quotation.extraction.schema;

/**
 * Review status of a single LLM-extracted field.
 *
 * <p>The model's self-reported confidence is a review hint only.
 * Deterministic validation and human review decide the final value.
 * Unknown status values are rejected during JSON parsing.</p>
 */
public enum FieldStatus {
    CONFIDENT,
    UNCERTAIN,
    MISSING,
    CONFLICTED
}
