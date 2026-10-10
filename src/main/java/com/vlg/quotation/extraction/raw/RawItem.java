package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

/**
 * Provider-supplied item draft. Quantities and prices arrive as decimal
 * strings (never JSON floats) and are converted to {@code BigDecimal} in
 * Java; order follows the list position.
 */
public record RawItem(
        @JsonProperty("sourceOrder") Integer sourceOrder,
        @JsonProperty("productName") @Valid RawField<String> productName,
        @JsonProperty("specifications") @Valid RawField<String> specifications,
        @JsonProperty("quantity") @Valid RawField<String> quantity,
        @JsonProperty("unit") @Valid RawField<String> unit,
        @JsonProperty("unitPrice") @Valid RawField<String> unitPrice) {
}
