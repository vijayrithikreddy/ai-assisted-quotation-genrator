package com.vlg.quotation.domain.numbering;

/**
 * Settings-driven numbering inputs. The business year comes only from
 * Settings changed manually by the user; the service never derives it from
 * the calendar, so no automatic year transition can occur.
 *
 * @param prefix label such as {@code VLG}
 * @param businessYear four-digit year such as {@code 2025}
 * @param nextSequence first sequence to try, 1 or greater
 * @param sequencePadding zero-pad width, 1 to 6; wider sequences print in full
 */
public record QuotationNumberConfiguration(String prefix, String businessYear, long nextSequence, int sequencePadding) {
}
