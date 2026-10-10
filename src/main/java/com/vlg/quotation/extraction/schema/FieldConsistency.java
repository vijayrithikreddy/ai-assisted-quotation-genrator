package com.vlg.quotation.extraction.schema;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces the {@link ExtractedField} invariant: {@code MISSING} requires a
 * null value, every other status requires a value. Prevents silent guessing
 * such as a missing price arriving as zero.
 */
@Documented
@Constraint(validatedBy = ExtractedFieldConsistencyValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldConsistency {

    String message() default
            "value and status are inconsistent: MISSING requires a null value, other statuses require a value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
