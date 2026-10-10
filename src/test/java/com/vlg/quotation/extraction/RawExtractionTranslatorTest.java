package com.vlg.quotation.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vlg.quotation.extraction.RawExtractionTranslator.TranslatedDraft;
import com.vlg.quotation.extraction.raw.RawConfidence;
import com.vlg.quotation.extraction.raw.RawConflict;
import com.vlg.quotation.extraction.raw.RawCustomer;
import com.vlg.quotation.extraction.raw.RawExtractionResponse;
import com.vlg.quotation.extraction.raw.RawField;
import com.vlg.quotation.extraction.raw.RawItem;
import com.vlg.quotation.extraction.raw.RawSourceQuality;
import com.vlg.quotation.extraction.raw.RawSourceStatus;
import com.vlg.quotation.extraction.schema.FieldStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class RawExtractionTranslatorTest {

    private final RawExtractionTranslator translator = new RawExtractionTranslator();

    @Test
    void shouldMapConfidenceAndPinCode() {
        TranslatedDraft translatedDraft = translator.translate(
                rawResponse(
                        new RawCustomer(
                                new RawField<>("ACME", RawConfidence.HIGH, null),
                                null, null, null, null, null,
                                new RawField<>("502285", RawConfidence.MEDIUM, "Digit unclear."),
                                new RawField<>(null, RawConfidence.LOW, "Not found.")),
                        List.of(item("Gear", "1", "NOS", "450.00"))),
                CustomerMode.NEW);

        assertThat(translatedDraft.draft().customer().companyName().value()).isEqualTo("ACME");
        assertThat(translatedDraft.draft().customer().companyName().status()).isEqualTo(FieldStatus.CONFIDENT);
        assertThat(translatedDraft.draft().customer().pin().value()).isEqualTo("502285");
        assertThat(translatedDraft.draft().customer().pin().status()).isEqualTo(FieldStatus.UNCERTAIN);
        assertThat(translatedDraft.draft().customer().pin().reviewReason()).isEqualTo("Digit unclear.");
        assertThat(translatedDraft.draft().customer().gstin().status()).isEqualTo(FieldStatus.MISSING);
        assertThat(translatedDraft.draft().customer().addressLine1().status()).isEqualTo(FieldStatus.MISSING);
        assertThat(translatedDraft.draft().items().get(0).quantity().value()).isEqualByComparingTo("1");
        assertThat(translatedDraft.draft().items().get(0).unitPrice().value()).isEqualByComparingTo("450.00");
    }

    @Test
    void shouldFlagMissingPriceInsteadOfZero() {
        TranslatedDraft translatedDraft = translator.translate(
                rawResponse(null, List.of(item("Gear", "1", "NOS", null))), CustomerMode.NEW);

        assertThat(translatedDraft.draft().items().get(0).unitPrice().status()).isEqualTo(FieldStatus.MISSING);
        assertThat(translatedDraft.draft().items().get(0).unitPrice().value()).isNull();
    }

    @Test
    void shouldRejectUnparseableMoney() {
        RawExtractionResponse badMoney = rawResponse(null, List.of(item("Gear", "two", "NOS", "450.00")));

        assertThatThrownBy(() -> translator.translate(badMoney, CustomerMode.NEW))
                .isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectWorkflowMismatch() {
        RawExtractionResponse mismatched = rawResponse(null, List.of(item("Gear", "1", "NOS", "10")));

        assertThatThrownBy(() -> translator.translate(mismatched, CustomerMode.EXISTING))
                .isInstanceOf(LlmBadResponseException.class);
    }

    @Test
    void shouldRejectUnusableSource() {
        RawExtractionResponse unusable = new RawExtractionResponse(
                "NEW_CUSTOMER",
                new RawSourceQuality(RawSourceStatus.UNUSABLE, List.of("Fully blurred.")),
                null,
                List.of(item("Gear", "1", "NOS", "10")),
                List.of(),
                true);

        assertThatThrownBy(() -> translator.translate(unusable, CustomerMode.NEW))
                .isInstanceOf(UnreadableImageException.class);
    }

    @Test
    void shouldTurnQualityIssuesAndConflictsIntoWarnings() {
        RawExtractionResponse conflicted = new RawExtractionResponse(
                "NEW_CUSTOMER",
                new RawSourceQuality(RawSourceStatus.USABLE_WITH_REVIEW, List.of("Shadow over the rate.")),
                new RawCustomer(
                        new RawField<>("ACME", RawConfidence.HIGH, null),
                        new RawField<>("Plot 1", RawConfidence.MEDIUM, null),
                        null, null, null, null, null, null),
                List.of(item("Gear", "1", "NOS", "10")),
                List.of(new RawConflict("ADDRESS_MISMATCH", "Challan shows another address.", List.of("addressLine1"))),
                true);

        TranslatedDraft translatedDraft = translator.translate(conflicted, CustomerMode.NEW);

        assertThat(translatedDraft.warnings()).hasSize(2);
        assertThat(translatedDraft.draft().customer().addressLine1().status()).isEqualTo(FieldStatus.CONFLICTED);
        assertThat(translatedDraft.draft().customer().addressLine1().value()).isEqualTo("Plot 1");
    }

    @Test
    void shouldMarkItemFieldConflicted() {
        RawExtractionResponse conflicted = new RawExtractionResponse(
                "NEW_CUSTOMER",
                new RawSourceQuality(RawSourceStatus.USABLE, List.of()),
                null,
                List.of(item("Gear", "1", "NOS", "10")),
                List.of(new RawConflict("QTY_MISMATCH", "Two quantities visible.", List.of("items[0].quantity"))),
                true);

        TranslatedDraft translatedDraft = translator.translate(conflicted, CustomerMode.NEW);

        assertThat(translatedDraft.draft().items().get(0).quantity().status()).isEqualTo(FieldStatus.CONFLICTED);
        assertThat(translatedDraft.draft().items().get(0).unitPrice().status()).isEqualTo(FieldStatus.CONFIDENT);
    }

    @Test
    void shouldExplainMissingCustomerForNewAndSavedRecordForExisting() {
        TranslatedDraft newDraft = translator.translate(
                rawResponse(null, List.of(item("Gear", "1", "NOS", "10"))), CustomerMode.NEW);
        assertThat(newDraft.warnings())
                .anyMatch(warning -> warning.code().equals("CUSTOMER_NOT_EXTRACTED"));
        assertThat(newDraft.draft().customer().companyName().status()).isEqualTo(FieldStatus.MISSING);

        TranslatedDraft existingDraft = translator.translate(
                new RawExtractionResponse(
                        "EXISTING_CUSTOMER",
                        new RawSourceQuality(RawSourceStatus.USABLE, List.of()),
                        null,
                        List.of(item("Gear", "1", "NOS", "10")),
                        List.of(),
                        true),
                CustomerMode.EXISTING);
        assertThat(existingDraft.warnings())
                .anyMatch(warning -> warning.code().equals("CUSTOMER_FROM_SAVED_RECORD"));
    }

    private RawExtractionResponse rawResponse(RawCustomer customer, List<RawItem> items) {
        return new RawExtractionResponse(
                "NEW_CUSTOMER",
                new RawSourceQuality(RawSourceStatus.USABLE, List.of()),
                customer,
                items,
                List.of(),
                true);
    }

    private RawItem item(String productName, String quantity, String unit, String unitPrice) {
        return new RawItem(
                1,
                new RawField<>(productName, RawConfidence.HIGH, null),
                new RawField<>(null, RawConfidence.LOW, "No specs visible."),
                quantity == null
                        ? new RawField<>(null, RawConfidence.LOW, "Rate area cut off.")
                        : new RawField<>(quantity, RawConfidence.HIGH, null),
                new RawField<>(unit, RawConfidence.MEDIUM, null),
                unitPrice == null
                        ? new RawField<>(null, RawConfidence.LOW, "Rate area cut off.")
                        : new RawField<>(unitPrice, RawConfidence.HIGH, null));
    }
}
