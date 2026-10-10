package com.vlg.quotation.extraction.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.math.BigDecimal;

/**
 * One quotation line as extracted by the multimodal LLM.
 *
 * <p>Quantities and prices arrive as raw facts. A missing price stays
 * {@code MISSING} and is flagged; it is never defaulted to zero.
 * No calculated amount lives here: totals are owned by the deterministic
 * calculation engine (Phase 3), and any LLM-supplied amount is rejected as
 * an unknown property.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record ExtractedQuotationItem(
        @JsonProperty("productName") @Valid ExtractedField<String> productName,
        @JsonProperty("specifications") @Valid ExtractedField<String> specifications,
        @JsonProperty("quantity") @Valid ExtractedField<BigDecimal> quantity,
        @JsonProperty("unit") @Valid ExtractedField<String> unit,
        @JsonProperty("unitPrice") @Valid ExtractedField<BigDecimal> unitPrice) {

    public ExtractedQuotationItem {
        productName = productName != null ? productName : ExtractedField.missing();
        specifications = specifications != null ? specifications : ExtractedField.missing();
        quantity = quantity != null ? quantity : ExtractedField.missing();
        unit = unit != null ? unit : ExtractedField.missing();
        unitPrice = unitPrice != null ? unitPrice : ExtractedField.missing();
    }
}
