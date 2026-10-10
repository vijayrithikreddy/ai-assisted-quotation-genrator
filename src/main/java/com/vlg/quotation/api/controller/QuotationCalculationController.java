package com.vlg.quotation.api.controller;

import com.vlg.quotation.api.request.QuotationCalculationRequest;
import com.vlg.quotation.api.response.CalculatedQuotation;
import com.vlg.quotation.domain.calculation.QuotationCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Deterministic money endpoint. Thin mapping only; math lives in the domain service. */
@Tag(name = "Quotations", description = "Quotation validation and calculation")
@RestController
@RequestMapping(path = "/api/v1/quotations/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuotationCalculationController {

    private final QuotationCalculationService calculationService;

    public QuotationCalculationController(QuotationCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @Operation(summary = "Calculate quotation totals deterministically")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CalculatedQuotation calculateQuotation(@Valid @RequestBody QuotationCalculationRequest calculationRequest) {
        QuotationCalculationService.QuotationTotals calculatedTotals = calculationService.calculate(
                calculationRequest.cgstRate(),
                calculationRequest.sgstRate(),
                calculationRequest.items().stream()
                        .map(item -> new QuotationCalculationService.LineInput(
                                item.productName(), item.quantity(), item.unit(), item.unitPrice()))
                        .toList());
        return new CalculatedQuotation(
                calculatedTotals.items().stream()
                        .map(line -> new CalculatedQuotation.CalculatedItem(
                                line.productName(), line.quantity(), line.unit(), line.unitPrice(), line.amount()))
                        .toList(),
                calculatedTotals.subtotal(),
                calculatedTotals.cgstRate(),
                calculatedTotals.cgstAmount(),
                calculatedTotals.sgstRate(),
                calculatedTotals.sgstAmount(),
                calculatedTotals.grandTotal(),
                calculatedTotals.amountInWords());
    }
}
