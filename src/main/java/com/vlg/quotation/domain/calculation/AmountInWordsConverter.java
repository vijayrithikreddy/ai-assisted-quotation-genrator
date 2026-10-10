package com.vlg.quotation.domain.calculation;

/**
 * Converts a whole-rupee amount into words using the Indian numbering system
 * (Thousand, Lakh, Crore, Arab). Paise are never included: callers pass the
 * already-rounded grand total.
 */
public final class AmountInWordsConverter {

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /** Indian-system units above hundreds, largest first. */
    private static final long[] UNIT_VALUES = {
        1_00_00_00_000L, // Arab (1e9)
        1_00_00_000L, // Crore (1e7)
        1_00_000L, // Lakh (1e5)
        1_000L // Thousand (1e3)
    };

    private static final String[] UNIT_NAMES = {"Arab", "Crore", "Lakh", "Thousand"};

    private AmountInWordsConverter() {
    }

    /**
     * Converts a non-negative whole-rupee amount to words with an "Only" suffix.
     *
     * @param roundedRupees already-rounded grand total, must be {@code >= 0}
     * @return words such as {@code "Two Lakh Ten Thousand Only"}
     */
    public static String convert(long roundedRupees) {
        if (roundedRupees < 0) {
            throw new IllegalArgumentException("Amount in words requires a non-negative amount.");
        }
        if (roundedRupees == 0) {
            return "Zero Only";
        }
        return convertPositive(roundedRupees) + " Only";
    }

    private static String convertPositive(long rupees) {
        if (rupees < 20) {
            return ONES[(int) rupees];
        }
        if (rupees < 100) {
            return TENS[(int) (rupees / 10)] + (rupees % 10 == 0 ? "" : " " + ONES[(int) (rupees % 10)]);
        }
        if (rupees < 1000) {
            return ONES[(int) (rupees / 100)] + " Hundred" + (rupees % 100 == 0 ? "" : " " + convertPositive(rupees % 100));
        }
        for (int unitIndex = 0; unitIndex < UNIT_VALUES.length; unitIndex++) {
            if (rupees >= UNIT_VALUES[unitIndex]) {
                long leadingPart = rupees / UNIT_VALUES[unitIndex];
                long remainderPart = rupees % UNIT_VALUES[unitIndex];
                return convertPositive(leadingPart)
                        + " " + UNIT_NAMES[unitIndex]
                        + (remainderPart == 0 ? "" : " " + convertPositive(remainderPart));
            }
        }
        throw new IllegalStateException("Unreachable amount: " + rupees);
    }
}
