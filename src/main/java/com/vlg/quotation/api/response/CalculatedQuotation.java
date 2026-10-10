package com.vlg.quotation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic calculation result. Intermediate amounts are exact; only
 * {@code grandTotal} is rounded to whole rupees, and {@code amountInWords}
 * derives from it.
 */
public record CalculatedQuotation(
        @JsonProperty("items") List<CalculatedItem> items,
        @JsonProperty("subtotal") BigDecimal subtotal,
        @JsonProperty("cgstRate") BigDecimal cgstRate,
        @JsonProperty("cgstAmount") BigDecimal cgstAmount,
        @JsonProperty("sgstRate") BigDecimal sgstRate,
        @JsonProperty("sgstAmount") BigDecimal sgstAmount,
        @JsonProperty("grandTotal") BigDecimal grandTotal,
        @JsonProperty("amountInWords") String amountInWords) {

    public record CalculatedItem(
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("unit") String unit,
            @JsonProperty("unitPrice") BigDecimal unitPrice,
            @JsonProperty("amount") BigDecimal amount) {
    }
}
