package com.vlg.quotation.domain.numbering;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe test double for {@link QuotationNumberStore}. Mirrors the
 * contract the Android SQLite implementation must honor: atomic
 * reserve-if-absent over exact-match numbers.
 */
public class InMemoryQuotationNumberStore implements QuotationNumberStore {

    private final Set<String> usedNumbers = ConcurrentHashMap.newKeySet();

    @Override
    public boolean exists(String normalizedNumber) {
        return usedNumbers.contains(normalizedNumber);
    }

    @Override
    public boolean reserveIfAbsent(String normalizedNumber) {
        return usedNumbers.add(normalizedNumber);
    }

    public int usedCount() {
        return usedNumbers.size();
    }
}
