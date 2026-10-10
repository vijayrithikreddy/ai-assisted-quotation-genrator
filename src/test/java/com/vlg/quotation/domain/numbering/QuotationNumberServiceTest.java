package com.vlg.quotation.domain.numbering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vlg.quotation.domain.numbering.QuotationNumberService.AllocatedNumber;
import com.vlg.quotation.domain.numbering.QuotationNumberService.CustomNumberValidation;
import com.vlg.quotation.domain.numbering.QuotationNumberService.UnusableReason;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuotationNumberServiceTest {

    private final QuotationNumberService numberService = new QuotationNumberService();

    @Test
    void shouldFormatWithZeroPadding() {
        assertThat(numberService.formatNumber("VLG", "2025", 8, 3)).isEqualTo("VLG-2025/008");
    }

    @Test
    void shouldPrintWideSequencesInFullInsteadOfTruncating() {
        assertThat(numberService.formatNumber("VLG", "2025", 1234, 3)).isEqualTo("VLG-2025/1234");
    }

    @Test
    void shouldFollowManualYearChange() {
        // Year comes only from Settings input; the service reads no clock.
        assertThat(numberService.formatNumber("VLG", "2026", 1, 3)).isEqualTo("VLG-2026/001");
        assertThat(numberService.previewNext(new QuotationNumberConfiguration("VLG", "2025", 8, 3)))
                .isEqualTo("VLG-2025/008");
    }

    @Test
    void shouldSkipUsedNumbersWithoutReserving() {
        InMemoryQuotationNumberStore numberStore = new InMemoryQuotationNumberStore();

        AllocatedNumber nextFree = numberService.nextAvailable(
                new QuotationNumberConfiguration("VLG", "2025", 8, 3), Set.of("VLG-2025/008", "VLG-2025/009"));

        assertThat(nextFree.sequence()).isEqualTo(10);
        assertThat(nextFree.formattedNumber()).isEqualTo("VLG-2025/010");
        assertThat(numberStore.exists("VLG-2025/010")).isFalse();
    }

    @Test
    void shouldReturnConfiguredSequenceWhenNothingIsUsed() {
        AllocatedNumber nextFree = numberService.nextAvailable(
                new QuotationNumberConfiguration("VLG", "2025", 8, 3), Set.of());

        assertThat(nextFree.sequence()).isEqualTo(8);
        assertThat(nextFree.formattedNumber()).isEqualTo("VLG-2025/008");
    }

    @Test
    void shouldAllocateThroughOwningStore() {
        InMemoryQuotationNumberStore numberStore = new InMemoryQuotationNumberStore();
        QuotationNumberConfiguration configuration = new QuotationNumberConfiguration("VLG", "2025", 8, 3);

        AllocatedNumber firstAllocation = numberService.allocateNext(configuration, numberStore);
        AllocatedNumber secondAllocation = numberService.allocateNext(configuration, numberStore);

        assertThat(firstAllocation.formattedNumber()).isEqualTo("VLG-2025/008");
        assertThat(secondAllocation.formattedNumber()).isEqualTo("VLG-2025/009");
    }

    @Test
    void shouldAcceptFreeCustomNumber() {
        CustomNumberValidation validationResult =
                numberService.validateCustomNumber("VLG-2025/100", new InMemoryQuotationNumberStore());

        assertThat(validationResult.usable()).isTrue();
        assertThat(validationResult.normalizedNumber()).isEqualTo("VLG-2025/100");
        assertThat(validationResult.unusableReason()).isNull();
    }

    @Test
    void shouldTrimCustomNumber() {
        CustomNumberValidation validationResult =
                numberService.validateCustomNumber("  VLG-2025/100  ", new InMemoryQuotationNumberStore());

        assertThat(validationResult.usable()).isTrue();
        assertThat(validationResult.normalizedNumber()).isEqualTo("VLG-2025/100");
    }

    @Test
    void shouldRejectDuplicateCustomNumber() {
        InMemoryQuotationNumberStore numberStore = new InMemoryQuotationNumberStore();
        numberStore.reserveIfAbsent("VLG-2025/100");

        CustomNumberValidation validationResult = numberService.validateCustomNumber("VLG-2025/100", numberStore);

        assertThat(validationResult.usable()).isFalse();
        assertThat(validationResult.unusableReason()).isEqualTo(UnusableReason.ALREADY_USED);
    }

    @Test
    void shouldRejectBlankCustomNumber() {
        assertThat(numberService.validateCustomNumber("   ", new InMemoryQuotationNumberStore()).unusableReason())
                .isEqualTo(UnusableReason.BLANK_NUMBER);
        assertThat(numberService.validateCustomNumber(null, new InMemoryQuotationNumberStore()).unusableReason())
                .isEqualTo(UnusableReason.BLANK_NUMBER);
    }

    @Test
    void shouldRejectTooLongCustomNumber() {
        assertThat(numberService.validateCustomNumber("VLG-2025/" + "0".repeat(30), new InMemoryQuotationNumberStore())
                        .unusableReason())
                .isEqualTo(UnusableReason.NUMBER_TOO_LONG);
    }

    @Test
    void shouldRejectCustomNumberWithInvalidCharacters() {
        assertThat(numberService.validateCustomNumber("Q.No! 123", new InMemoryQuotationNumberStore()).unusableReason())
                .isEqualTo(UnusableReason.INVALID_CHARACTERS);
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> numberService.formatNumber("", "2025", 8, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG!", "2025", 8, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG", "25", 8, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG", "2025-26", 8, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG", "2025", 0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG", "2025", 8, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> numberService.formatNumber("VLG", "2025", 8, 7)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailAllocationWhenRangeIsExhausted() {
        QuotationNumberStore alwaysDenyingStore = new QuotationNumberStore() {
            @Override
            public boolean exists(String normalizedNumber) {
                return true;
            }

            @Override
            public boolean reserveIfAbsent(String normalizedNumber) {
                return false;
            }
        };

        assertThatThrownBy(() -> numberService.allocateNext(
                        new QuotationNumberConfiguration("VLG", "2025", 8, 3), alwaysDenyingStore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exhausted");
    }
}
