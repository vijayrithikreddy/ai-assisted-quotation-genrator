package com.vlg.quotation.domain.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Deterministic quotation money engine. The LLM never computes these values.
 *
 * <p>Frozen rounding policy (business-approved): every intermediate value is
 * exact {@link BigDecimal} arithmetic with no rounding; only the final grand
 * total is rounded to whole rupees with {@link RoundingMode#HALF_UP}. Tax
 * amounts therefore keep paise precision (division by 100 is exact) and the
 * amount in words derives from the rounded grand total.</p>
 */
@Service
public class QuotationCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_RATE = new BigDecimal("100");

    /**
     * Calculates line amounts, subtotal, taxes, rounded grand total, and
     * amount in words for already-validated inputs.
     *
     * @param cgstRate agreed CGST percentage, 0 to 100 inclusive
     * @param sgstRate agreed SGST percentage, 0 to 100 inclusive
     * @param pricedLines lines with quantity greater than zero and non-negative prices
     * @return totals with exact intermediates and a rounded grand total
     */
    public QuotationTotals calculate(BigDecimal cgstRate, BigDecimal sgstRate, List<LineInput> pricedLines) {
        BigDecimal validatedCgstRate = requireValidRate(cgstRate, "cgstRate");
        BigDecimal validatedSgstRate = requireValidRate(sgstRate, "sgstRate");
        if (pricedLines == null || pricedLines.isEmpty()) {
            throw new IllegalArgumentException("At least one quotation item is required.");
        }

        List<LineResult> calculatedLines = pricedLines.stream().map(this::calculateLine).toList();
        BigDecimal calculatedSubtotal =
                calculatedLines.stream().map(LineResult::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgstAmount = calculatedSubtotal.multiply(validatedCgstRate).divide(HUNDRED);
        BigDecimal sgstAmount = calculatedSubtotal.multiply(validatedSgstRate).divide(HUNDRED);
        BigDecimal roundedGrandTotal =
                calculatedSubtotal.add(cgstAmount).add(sgstAmount).setScale(0, RoundingMode.HALF_UP);

        return new QuotationTotals(
                calculatedLines,
                calculatedSubtotal,
                validatedCgstRate,
                cgstAmount,
                validatedSgstRate,
                sgstAmount,
                roundedGrandTotal,
                AmountInWordsConverter.convert(roundedGrandTotal.longValueExact()));
    }

    private LineResult calculateLine(LineInput pricedLine) {
        Objects.requireNonNull(pricedLine, "Quotation item is required.");
        if (pricedLine.productName() == null || pricedLine.productName().isBlank()) {
            throw new IllegalArgumentException("Product name is required for every item.");
        }
        if (pricedLine.quantity() == null || pricedLine.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (pricedLine.unit() == null || pricedLine.unit().isBlank()) {
            throw new IllegalArgumentException("Unit is required for every item.");
        }
        if (pricedLine.unitPrice() == null || pricedLine.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        return new LineResult(
                pricedLine.productName(),
                pricedLine.quantity(),
                pricedLine.unit(),
                pricedLine.unitPrice(),
                pricedLine.quantity().multiply(pricedLine.unitPrice()));
    }

    private BigDecimal requireValidRate(BigDecimal taxRate, String rateName) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(MAX_RATE) > 0) {
            throw new IllegalArgumentException(rateName + " must be between 0 and 100.");
        }
        return taxRate;
    }

    /** Validated price facts for one line; arithmetic happens in {@link #calculate}. */
    public record LineInput(String productName, BigDecimal quantity, String unit, BigDecimal unitPrice) {
    }

    /** One line enriched with its exact amount. */
    public record LineResult(
            String productName, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal amount) {
    }

    /**
     * Totals with exact intermediates. Only {@code grandTotal} is rounded;
     * {@code amountInWords} derives from it.
     */
    public record QuotationTotals(
            List<LineResult> items,
            BigDecimal subtotal,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal grandTotal,
            String amountInWords) {
    }
}
