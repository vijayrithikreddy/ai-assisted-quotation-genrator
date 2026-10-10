package com.vlg.quotation.domain.numbering;

/**
 * Permanent record of used quotation numbers. In V1 this lives in the
 * Android local database, which enforces uniqueness; the backend stays
 * stateless. Any implementation backing {@code allocateNext} must make
 * {@link #reserveIfAbsent} atomic (transaction or equivalent), otherwise two
 * quotations could claim the same number.
 */
public interface QuotationNumberStore {

    /** Returns true when the exact (case-sensitive, trimmed) number is already used. */
    boolean exists(String normalizedNumber);

    /**
     * Atomically reserves the number, returning true when this caller won it
     * and false when it was already present.
     */
    boolean reserveIfAbsent(String normalizedNumber);
}
