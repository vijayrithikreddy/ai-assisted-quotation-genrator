package com.vlg.quotation.domain.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vlg.quotation.domain.calculation.QuotationCalculationService.LineInput;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuotationCalculationServiceTest {

    private final QuotationCalculationService calculationService = new QuotationCalculationService();

    @Test
    void shouldMatchReferenceExample() {
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(
                        new LineInput("Duplex Sprocket EN19", new BigDecimal("3"), "PCS", new BigDecimal("1700")),
                        new LineInput("Sprocket EN19", new BigDecimal("1"), "PCS", new BigDecimal("4000"))));

        assertThat(calculatedTotals.items().get(0).amount()).isEqualByComparingTo("5100");
        assertThat(calculatedTotals.items().get(1).amount()).isEqualByComparingTo("4000");
        assertThat(calculatedTotals.subtotal()).isEqualByComparingTo("9100");
        assertThat(calculatedTotals.cgstAmount()).isEqualByComparingTo("819");
        assertThat(calculatedTotals.sgstAmount()).isEqualByComparingTo("819");
        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("10738");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("Ten Thousand Seven Hundred Thirty Eight Only");
    }

    @Test
    void shouldCalculateSingleItem() {
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(new LineInput("Worm Wheel", new BigDecimal("1"), "NOS", new BigDecimal("40000"))));

        assertThat(calculatedTotals.subtotal()).isEqualByComparingTo("40000");
        assertThat(calculatedTotals.cgstAmount()).isEqualByComparingTo("3600");
        assertThat(calculatedTotals.sgstAmount()).isEqualByComparingTo("3600");
        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("47200");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("Forty Seven Thousand Two Hundred Only");
    }

    @Test
    void shouldRoundOnlyTheGrandTotal() {
        // 111 + 9.99 + 9.99 = 130.98 -> 131. Tax amounts stay exact.
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(new LineInput("Gear Blank", new BigDecimal("1"), "NOS", new BigDecimal("111"))));

        assertThat(calculatedTotals.cgstAmount()).isEqualByComparingTo("9.99");
        assertThat(calculatedTotals.sgstAmount()).isEqualByComparingTo("9.99");
        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("131");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("One Hundred Thirty One Only");
    }

    @Test
    void shouldRoundHalfUpOnExactHalf() {
        // 25 + 2.25 + 2.25 = 29.5 -> 30.
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(new LineInput("Pinion", new BigDecimal("5"), "PCS", new BigDecimal("5"))));

        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("30");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("Thirty Only");
    }

    @Test
    void shouldAllowZeroUnitPrice() {
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(new LineInput("Sample", new BigDecimal("1"), "NOS", BigDecimal.ZERO)));

        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("0");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("Zero Only");
    }

    @Test
    void shouldHandleLargeAmounts() {
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                new BigDecimal("9"),
                new BigDecimal("9"),
                List.of(new LineInput("Worm Gear", new BigDecimal("100"), "NOS", new BigDecimal("210000"))));

        assertThat(calculatedTotals.subtotal()).isEqualByComparingTo("21000000");
        assertThat(calculatedTotals.grandTotal()).isEqualByComparingTo("24780000");
        assertThat(calculatedTotals.amountInWords()).isEqualTo("Two Crore Forty Seven Lakh Eighty Thousand Only");
    }

    @Test
    void shouldRejectZeroQuantity() {
        assertThatThrownBy(() -> calculationService.calculate(
                        new BigDecimal("9"),
                        new BigDecimal("9"),
                        List.of(new LineInput("Gear", BigDecimal.ZERO, "NOS", new BigDecimal("100")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> calculationService.calculate(
                        new BigDecimal("9"),
                        new BigDecimal("9"),
                        List.of(new LineInput("Gear", new BigDecimal("-2"), "NOS", new BigDecimal("100")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        assertThatThrownBy(() -> calculationService.calculate(
                        new BigDecimal("9"),
                        new BigDecimal("9"),
                        List.of(new LineInput("Gear", BigDecimal.ONE, "NOS", new BigDecimal("-5")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectEmptyItems() {
        assertThatThrownBy(() -> calculationService.calculate(new BigDecimal("9"), new BigDecimal("9"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectOutOfRangeRate() {
        LineInput validLine = new LineInput("Gear", BigDecimal.ONE, "NOS", new BigDecimal("100"));
        assertThatThrownBy(() -> calculationService.calculate(new BigDecimal("101"), new BigDecimal("9"), List.of(validLine)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculationService.calculate(new BigDecimal("9"), new BigDecimal("-1"), List.of(validLine)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
