package com.vlg.quotation.api.controller;

import com.vlg.quotation.api.response.ExtractionResultResponse;
import com.vlg.quotation.extraction.QuotationExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multimodal extraction entry point. Accepts the handwritten image plus one
 * optional supporting document and returns a review-ready draft. Thin mapping
 * only; orchestration lives in {@link QuotationExtractionService}.
 */
@Tag(name = "Extraction", description = "AI-assisted draft extraction")
@RestController
@RequestMapping(path = "/api/v1/extractions", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExtractionController {

    private final QuotationExtractionService extractionService;

    public ExtractionController(QuotationExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @Operation(summary = "Extract a review-ready quotation draft from source files")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionResultResponse extractQuotation(
            @RequestParam("quotationImage") MultipartFile quotationImage,
            @RequestParam("customerMode") String customerMode,
            @RequestParam(value = "supportingDocument", required = false) MultipartFile supportingDocument,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "customerContext", required = false) String customerContext) {
        QuotationExtractionService.ExtractionOutcome extractionOutcome = extractionService.extract(
                quotationImage, customerMode, supportingDocument, customerId, customerContext);
        return new ExtractionResultResponse(
                extractionOutcome.extractionId(),
                "COMPLETED",
                extractionOutcome.modelIdentifier(),
                extractionOutcome.promptVersion(),
                extractionOutcome.schemaVersion(),
                extractionOutcome.draft(),
                extractionOutcome.warnings());
    }
}
