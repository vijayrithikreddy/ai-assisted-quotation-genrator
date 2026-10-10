package com.vlg.quotation.api.controller;

import com.vlg.quotation.api.request.CustomNumberValidationRequest;
import com.vlg.quotation.api.request.NextAvailableNumberRequest;
import com.vlg.quotation.api.request.QuotationNumberPreviewRequest;
import com.vlg.quotation.api.response.CustomNumberValidationResponse;
import com.vlg.quotation.api.response.NextAvailableNumberResponse;
import com.vlg.quotation.api.response.QuotationNumberPreviewResponse;
import com.vlg.quotation.domain.numbering.QuotationNumberConfiguration;
import com.vlg.quotation.domain.numbering.QuotationNumberService;
import com.vlg.quotation.domain.numbering.QuotationNumberStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stateless numbering helpers. The Android client owns the permanent
 * used-number set; these endpoints format and look up against the set the
 * client supplies. Nothing here reserves a number.
 */
@Tag(name = "Numbering", description = "Quotation number preview and validation")
@RestController
@RequestMapping(path = "/api/v1/quotation-numbers", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuotationNumberController {

    private final QuotationNumberService numberService;

    public QuotationNumberController(QuotationNumberService numberService) {
        this.numberService = numberService;
    }

    @Operation(summary = "Preview the formatted number without reserving it")
    @PostMapping(path = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public QuotationNumberPreviewResponse previewNumber(
            @Valid @RequestBody QuotationNumberPreviewRequest previewRequest) {
        String formattedNumber = numberService.formatNumber(
                previewRequest.prefix(),
                previewRequest.businessYear(),
                previewRequest.nextSequence(),
                previewRequest.sequencePadding());
        return new QuotationNumberPreviewResponse(formattedNumber, true);
    }

    @Operation(summary = "Find the first free number at or after the configured sequence")
    @PostMapping(path = "/next-available", consumes = MediaType.APPLICATION_JSON_VALUE)
    public NextAvailableNumberResponse nextAvailableNumber(
            @Valid @RequestBody NextAvailableNumberRequest availableRequest) {
        QuotationNumberService.AllocatedNumber nextFree = numberService.nextAvailable(
                new QuotationNumberConfiguration(
                        availableRequest.prefix(),
                        availableRequest.businessYear(),
                        availableRequest.nextSequence(),
                        availableRequest.sequencePadding()),
                new HashSet<>(availableRequest.usedNumbers()));
        return new NextAvailableNumberResponse(nextFree.sequence(), nextFree.formattedNumber(), true);
    }

    @Operation(summary = "Check whether a custom number is usable")
    @PostMapping(path = "/validate-custom", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CustomNumberValidationResponse validateCustomNumber(
            @Valid @RequestBody CustomNumberValidationRequest validationRequest) {
        QuotationNumberService.CustomNumberValidation validationResult = numberService.validateCustomNumber(
                validationRequest.candidateNumber(), new LookupOnlyStore(new HashSet<>(validationRequest.usedNumbers())));
        String reasonName =
                validationResult.unusableReason() == null ? null : validationResult.unusableReason().name();
        return new CustomNumberValidationResponse(validationResult.normalizedNumber(), validationResult.usable(), reasonName);
    }

    /** Read-only store view over a client-supplied used set. Never reserves. */
    private record LookupOnlyStore(HashSet<String> usedNumbers) implements QuotationNumberStore {
        @Override
        public boolean exists(String normalizedNumber) {
            return usedNumbers.contains(normalizedNumber);
        }

        @Override
        public boolean reserveIfAbsent(String normalizedNumber) {
            throw new UnsupportedOperationException("Lookup-only store cannot reserve numbers.");
        }
    }
}
