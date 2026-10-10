package com.vlg.quotation.domain.numbering;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Pure quotation-number logic: formatting, skip-over-used allocation, and
 * custom-number validation. Holds no state and reads no clock, so output
 * depends only on the supplied configuration and known used numbers.
 */
@Service
public class QuotationNumberService {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,12}$");
    private static final Pattern BUSINESS_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");
    private static final Pattern CUSTOM_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 \\-/]*$");

    private static final int MIN_SEQUENCE_PADDING = 1;
    private static final int MAX_SEQUENCE_PADDING = 6;
    private static final int MAX_CUSTOM_NUMBER_LENGTH = 30;
    private static final long MAX_ALLOCATION_SCAN = 100_000L;

    /**
     * Formats {@code PREFIX-YEAR/SEQ}, e.g. {@code VLG-2025/008}.
     * Sequences wider than the padding print in full instead of truncating.
     */
    public String formatNumber(String prefix, String businessYear, long sequence, int sequencePadding) {
        requireValidConfiguration(prefix, businessYear, sequence, sequencePadding);
        return prefix + "-" + businessYear + "/" + String.format("%0" + sequencePadding + "d", sequence);
    }

    /** Formats the configuration's next sequence without reserving anything. */
    public String previewNext(QuotationNumberConfiguration configuration) {
        Objects.requireNonNull(configuration, "Numbering configuration is required.");
        return formatNumber(
                configuration.prefix(),
                configuration.businessYear(),
                configuration.nextSequence(),
                configuration.sequencePadding());
    }

    /**
     * Finds the first formatted number at or after the configured sequence
     * that is absent from the supplied used set. Never reserves.
     */
    public AllocatedNumber nextAvailable(QuotationNumberConfiguration configuration, Set<String> usedNumbers) {
        Objects.requireNonNull(configuration, "Numbering configuration is required.");
        Set<String> knownUsedNumbers = usedNumbers == null ? Set.of() : usedNumbers;
        long candidateSequence = configuration.nextSequence();
        for (long scanned = 0; scanned < MAX_ALLOCATION_SCAN; scanned++, candidateSequence++) {
            String candidateNumber =
                    formatNumber(configuration.prefix(), configuration.businessYear(), candidateSequence, configuration.sequencePadding());
            if (!knownUsedNumbers.contains(candidateNumber)) {
                return new AllocatedNumber(candidateSequence, candidateNumber);
            }
        }
        throw new IllegalArgumentException(
                "Quotation number range is exhausted near sequence " + configuration.nextSequence() + ".");
    }

    /**
     * Atomically allocates the first free number via the owning store. Under
     * contention a loser retries with the next sequence, so numbers may skip
     * but never duplicate.
     */
    public AllocatedNumber allocateNext(QuotationNumberConfiguration configuration, QuotationNumberStore numberStore) {
        Objects.requireNonNull(configuration, "Numbering configuration is required.");
        Objects.requireNonNull(numberStore, "Number store is required.");
        long candidateSequence = configuration.nextSequence();
        for (long scanned = 0; scanned < MAX_ALLOCATION_SCAN; scanned++, candidateSequence++) {
            String candidateNumber =
                    formatNumber(configuration.prefix(), configuration.businessYear(), candidateSequence, configuration.sequencePadding());
            if (numberStore.reserveIfAbsent(candidateNumber)) {
                return new AllocatedNumber(candidateSequence, candidateNumber);
            }
        }
        throw new IllegalArgumentException(
                "Quotation number range is exhausted near sequence " + configuration.nextSequence() + ".");
    }

    /** Validates a user-supplied custom number against shape rules and the owning store. */
    public CustomNumberValidation validateCustomNumber(String candidateNumber, QuotationNumberStore numberStore) {
        Objects.requireNonNull(numberStore, "Number store is required.");
        String normalizedNumber = candidateNumber == null ? "" : candidateNumber.trim();
        if (normalizedNumber.isEmpty()) {
            return new CustomNumberValidation("", false, UnusableReason.BLANK_NUMBER);
        }
        if (normalizedNumber.length() > MAX_CUSTOM_NUMBER_LENGTH) {
            return new CustomNumberValidation(normalizedNumber, false, UnusableReason.NUMBER_TOO_LONG);
        }
        if (!CUSTOM_NUMBER_PATTERN.matcher(normalizedNumber).matches()) {
            return new CustomNumberValidation(normalizedNumber, false, UnusableReason.INVALID_CHARACTERS);
        }
        if (numberStore.exists(normalizedNumber)) {
            return new CustomNumberValidation(normalizedNumber, false, UnusableReason.ALREADY_USED);
        }
        return new CustomNumberValidation(normalizedNumber, true, null);
    }

    private void requireValidConfiguration(String prefix, String businessYear, long sequence, int sequencePadding) {
        if (prefix == null || !PREFIX_PATTERN.matcher(prefix).matches()) {
            throw new IllegalArgumentException("Numbering prefix must be 1 to 12 letters or digits.");
        }
        if (businessYear == null || !BUSINESS_YEAR_PATTERN.matcher(businessYear).matches()) {
            throw new IllegalArgumentException("Business year must be a four-digit year set manually in Settings.");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("Next sequence must be 1 or greater.");
        }
        if (sequencePadding < MIN_SEQUENCE_PADDING || sequencePadding > MAX_SEQUENCE_PADDING) {
            throw new IllegalArgumentException("Sequence padding must be between 1 and 6.");
        }
    }

    /** A sequence paired with its formatted number. Reservation happens only via {@code allocateNext}. */
    public record AllocatedNumber(long sequence, String formattedNumber) {
    }

    /** Outcome of custom-number validation. {@code unusableReason} is null when usable. */
    public record CustomNumberValidation(String normalizedNumber, boolean usable, UnusableReason unusableReason) {
    }

    public enum UnusableReason {
        BLANK_NUMBER,
        NUMBER_TOO_LONG,
        INVALID_CHARACTERS,
        ALREADY_USED
    }
}
