package com.vlg.quotation.extraction.schema;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks the {@link FieldConsistency} invariant for any extracted field. */
public class ExtractedFieldConsistencyValidator
        implements ConstraintValidator<FieldConsistency, ExtractedField<?>> {

    @Override
    public boolean isValid(ExtractedField<?> extractedField, ConstraintValidatorContext context) {
        if (extractedField == null || extractedField.status() == null) {
            // Null containers and null statuses are reported by @NotNull instead.
            return true;
        }
        if (extractedField.status() == FieldStatus.MISSING) {
            return extractedField.value() == null;
        }
        return extractedField.value() != null;
    }
}
