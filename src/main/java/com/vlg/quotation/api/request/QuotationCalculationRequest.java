package com.vlg.quotation.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Calculation request. Totals in the response are always recomputed; any
 * client-supplied totals would be ignored because no such fields exist here.
 */
public record QuotationCalculationRequest(
        @JsonProperty("cgstRate")
                @NotNull(message = "cgstRate is required")
                @DecimalMin(value = "0", message = "cgstRate must be between 0 and 100")
                @DecimalMax(value = "100", message = "cgstRate must be between 0 and 100")
                BigDecimal cgstRate,
        @JsonProperty("sgstRate")
                @NotNull(message = "sgstRate is required")
                @DecimalMin(value = "0", message = "sgstRate must be between 0 and 100")
                @DecimalMax(value = "100", message = "sgstRate must be between 0 and 100")
                BigDecimal sgstRate,
        @JsonProperty("items")
                @NotNull(message = "items are required")
                @Size(min = 1, message = "at least one item is required")
                List<@Valid CalculationItemRequest> items) {

    /** One priced line. Quantity must exceed zero; prices cannot be negative. */
    public record CalculationItemRequest(
            @JsonProperty("productName") @NotBlank(message = "productName is required") String productName,
            @JsonProperty("quantity")
                    @NotNull(message = "quantity is required")
                    @DecimalMin(value = "0", inclusive = false, message = "quantity must be greater than zero")
                    BigDecimal quantity,
            @JsonProperty("unit") @NotBlank(message = "unit is required") String unit,
            @JsonProperty("unitPrice")
                    @NotNull(message = "unitPrice is required")
                    @DecimalMin(value = "0", message = "unitPrice cannot be negative")
                    BigDecimal unitPrice) {
    }
}
