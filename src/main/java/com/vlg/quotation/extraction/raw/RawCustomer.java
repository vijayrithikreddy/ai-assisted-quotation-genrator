package com.vlg.quotation.extraction.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

/**
 * Provider-supplied customer draft. All fields nullable: absent objects mean
 * the model offered nothing for that field. Note {@code pinCode}, mapped to
 * the internal {@code pin} during translation.
 */
public record RawCustomer(
        @JsonProperty("companyName") @Valid RawField<String> companyName,
        @JsonProperty("addressLine1") @Valid RawField<String> addressLine1,
        @JsonProperty("addressLine2") @Valid RawField<String> addressLine2,
        @JsonProperty("area") @Valid RawField<String> area,
        @JsonProperty("district") @Valid RawField<String> district,
        @JsonProperty("state") @Valid RawField<String> state,
        @JsonProperty("pinCode") @Valid RawField<String> pinCode,
        @JsonProperty("gstin") @Valid RawField<String> gstin) {
}
