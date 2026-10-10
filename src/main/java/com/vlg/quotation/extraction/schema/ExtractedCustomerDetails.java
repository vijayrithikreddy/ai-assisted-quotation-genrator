package com.vlg.quotation.extraction.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

/**
 * Customer facts as extracted by the multimodal LLM for a new customer.
 *
 * <p>Every field may be {@code MISSING}; missing data is flagged for human
 * review, never invented. Absent JSON field objects normalize to
 * {@code MISSING} so sparse extractions stay parseable. Required-field
 * decisions belong to reviewed-draft validation (Phase 7), not to this
 * schema.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record ExtractedCustomerDetails(
        @JsonProperty("companyName") @Valid ExtractedField<String> companyName,
        @JsonProperty("addressLine1") @Valid ExtractedField<String> addressLine1,
        @JsonProperty("addressLine2") @Valid ExtractedField<String> addressLine2,
        @JsonProperty("area") @Valid ExtractedField<String> area,
        @JsonProperty("district") @Valid ExtractedField<String> district,
        @JsonProperty("state") @Valid ExtractedField<String> state,
        @JsonProperty("pin") @Valid ExtractedField<String> pin,
        @JsonProperty("gstin") @Valid ExtractedField<String> gstin) {

    public ExtractedCustomerDetails {
        companyName = companyName != null ? companyName : ExtractedField.missing();
        addressLine1 = addressLine1 != null ? addressLine1 : ExtractedField.missing();
        addressLine2 = addressLine2 != null ? addressLine2 : ExtractedField.missing();
        area = area != null ? area : ExtractedField.missing();
        district = district != null ? district : ExtractedField.missing();
        state = state != null ? state : ExtractedField.missing();
        pin = pin != null ? pin : ExtractedField.missing();
        gstin = gstin != null ? gstin : ExtractedField.missing();
    }
}
