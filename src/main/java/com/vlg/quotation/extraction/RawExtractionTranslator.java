package com.vlg.quotation.extraction;

import com.vlg.quotation.extraction.raw.RawConfidence;
import com.vlg.quotation.extraction.raw.RawCustomer;
import com.vlg.quotation.extraction.raw.RawExtractionResponse;
import com.vlg.quotation.extraction.raw.RawField;
import com.vlg.quotation.extraction.raw.RawItem;
import com.vlg.quotation.extraction.raw.RawSourceStatus;
import com.vlg.quotation.extraction.schema.ExtractedCustomerDetails;
import com.vlg.quotation.extraction.schema.ExtractedField;
import com.vlg.quotation.extraction.schema.ExtractedQuotationItem;
import com.vlg.quotation.extraction.schema.ExtractionWarning;
import com.vlg.quotation.extraction.schema.FieldStatus;
import com.vlg.quotation.extraction.schema.QuotationExtractionResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Translates the provider-facing raw contract ({@code HIGH/MEDIUM/LOW})
 * into the application draft ({@code CONFIDENT/UNCERTAIN/MISSING/CONFLICTED}).
 * Keeps provider confidence scales replaceable: a future vendor only needs a
 * new translator branch, never draft-schema changes.
 */
@Component
public class RawExtractionTranslator {

    private static final Pattern ITEM_FIELD_REFERENCE = Pattern.compile("^items\\[(\\d+)]\\.(\\w+)$");
    private static final int MAX_ISSUE_TEXT_LENGTH = 500;

    /** Translated draft plus review warnings. Throws on contract violations and unusable sources. */
    public TranslatedDraft translate(RawExtractionResponse rawResponse, CustomerMode customerMode) {
        if (!customerMode.providerWorkflowName().equals(rawResponse.workflow())) {
            throw new LlmBadResponseException("The AI provider returned data outside the extraction contract. Please try again.");
        }
        List<ExtractionWarning> draftWarnings = new ArrayList<>();
        if (rawResponse.sourceQuality().status() == RawSourceStatus.UNUSABLE) {
            throw new UnreadableImageException(
                    "The source image is not usable for extraction. Please capture a clearer image.");
        }
        if (rawResponse.sourceQuality().status() == RawSourceStatus.USABLE_WITH_REVIEW) {
            draftWarnings.add(new ExtractionWarning(
                    "SOURCE_QUALITY_REVIEW", "Source quality needs review: " + joinIssues(rawResponse)));
        }

        Map<String, ExtractedField<String>> customerFields = translateCustomerFields(rawResponse.customer());
        if (rawResponse.customer() == null && customerMode == CustomerMode.NEW) {
            draftWarnings.add(new ExtractionWarning(
                    "CUSTOMER_NOT_EXTRACTED", "No customer details could be extracted. Please enter them manually."));
        }
        if (customerMode == CustomerMode.EXISTING) {
            draftWarnings.add(new ExtractionWarning(
                    "CUSTOMER_FROM_SAVED_RECORD",
                    "Customer details come from the saved record. Verify them against the source image."));
        }

        List<ItemDraft> itemDrafts = new ArrayList<>();
        for (RawItem rawItem : rawResponse.items()) {
            itemDrafts.add(new ItemDraft(
                    translateTextField(rawItem.productName()),
                    translateTextField(rawItem.specifications()),
                    translateMoneyField(rawItem.quantity(), "quantity"),
                    translateTextField(rawItem.unit()),
                    translateMoneyField(rawItem.unitPrice(), "unit price")));
        }

        applyConflicts(rawResponse, customerFields, itemDrafts, draftWarnings);

        ExtractedCustomerDetails draftCustomer = new ExtractedCustomerDetails(
                customerFields.get("companyname"),
                customerFields.get("addressline1"),
                customerFields.get("addressline2"),
                customerFields.get("area"),
                customerFields.get("district"),
                customerFields.get("state"),
                customerFields.get("pincode"),
                customerFields.get("gstin"));
        List<ExtractedQuotationItem> draftItems = itemDrafts.stream()
                .map(itemDraft -> new ExtractedQuotationItem(
                        itemDraft.productName(),
                        itemDraft.specifications(),
                        itemDraft.quantity(),
                        itemDraft.unit(),
                        itemDraft.unitPrice()))
                .toList();
        QuotationExtractionResponse draftResponse = new QuotationExtractionResponse(
                "v1", draftCustomer, draftItems, List.of());
        return new TranslatedDraft(draftResponse, List.copyOf(draftWarnings));
    }

    private Map<String, ExtractedField<String>> translateCustomerFields(RawCustomer rawCustomer) {
        Map<String, ExtractedField<String>> customerFields = new LinkedHashMap<>();
        customerFields.put("companyname", translateTextField(rawCustomer == null ? null : rawCustomer.companyName()));
        customerFields.put("addressline1", translateTextField(rawCustomer == null ? null : rawCustomer.addressLine1()));
        customerFields.put("addressline2", translateTextField(rawCustomer == null ? null : rawCustomer.addressLine2()));
        customerFields.put("area", translateTextField(rawCustomer == null ? null : rawCustomer.area()));
        customerFields.put("district", translateTextField(rawCustomer == null ? null : rawCustomer.district()));
        customerFields.put("state", translateTextField(rawCustomer == null ? null : rawCustomer.state()));
        customerFields.put("pincode", translateTextField(rawCustomer == null ? null : rawCustomer.pinCode()));
        customerFields.put("gstin", translateTextField(rawCustomer == null ? null : rawCustomer.gstin()));
        return customerFields;
    }

    private ExtractedField<String> translateTextField(RawField<String> rawField) {
        if (rawField == null || rawField.value() == null) {
            return new ExtractedField<>(null, FieldStatus.MISSING, reviewReasonOf(rawField));
        }
        return new ExtractedField<>(rawField.value(), mapConfidence(rawField), rawField.reviewReason());
    }

    private ExtractedField<BigDecimal> translateMoneyField(RawField<String> rawField, String fieldLabel) {
        if (rawField == null || rawField.value() == null || rawField.value().isBlank()) {
            return new ExtractedField<>(null, FieldStatus.MISSING, reviewReasonOf(rawField));
        }
        try {
            BigDecimal moneyValue = new BigDecimal(rawField.value().trim());
            return new ExtractedField<>(moneyValue, mapConfidence(rawField), rawField.reviewReason());
        } catch (NumberFormatException unparseableMoney) {
            throw new LlmBadResponseException(
                    "The AI provider returned an unreadable " + fieldLabel + ". Please try again.", unparseableMoney);
        }
    }

    private FieldStatus mapConfidence(RawField<?> rawField) {
        if (rawField == null || rawField.confidence() == null) {
            return FieldStatus.UNCERTAIN;
        }
        return switch (rawField.confidence()) {
            case HIGH -> FieldStatus.CONFIDENT;
            case MEDIUM -> FieldStatus.UNCERTAIN;
            case LOW -> rawField.value() == null ? FieldStatus.MISSING : FieldStatus.UNCERTAIN;
        };
    }

    private String reviewReasonOf(RawField<?> rawField) {
        return rawField == null ? null : rawField.reviewReason();
    }

    private void applyConflicts(
            RawExtractionResponse rawResponse,
            Map<String, ExtractedField<String>> customerFields,
            List<ItemDraft> itemDrafts,
            List<ExtractionWarning> draftWarnings) {
        rawResponse.conflicts().forEach(rawConflict -> {
            draftWarnings.add(new ExtractionWarning(rawConflict.code(), rawConflict.message()));
            rawConflict.fields().forEach(fieldReference -> {
                String normalizedReference = fieldReference == null
                        ? ""
                        : fieldReference.trim().toLowerCase().replaceAll("[_\\s]", "");
                if (customerFields.containsKey(normalizedReference)) {
                    ExtractedField<String> conflictedField = customerFields.get(normalizedReference);
                    customerFields.put(
                            normalizedReference,
                            new ExtractedField<>(
                                    conflictedField.value(), FieldStatus.CONFLICTED, conflictedField.reviewReason()));
                    return;
                }
                Matcher itemReference = ITEM_FIELD_REFERENCE.matcher(normalizedReference);
                if (itemReference.matches()) {
                    int itemIndex = Integer.parseInt(itemReference.group(1));
                    String itemField = itemReference.group(2);
                    if (itemIndex >= 0 && itemIndex < itemDrafts.size()) {
                        itemDrafts.set(itemIndex, markItemFieldConflicted(itemDrafts.get(itemIndex), itemField));
                    }
                }
            });
        });
    }

    private ItemDraft markItemFieldConflicted(ItemDraft itemDraft, String itemField) {
        return switch (itemField) {
            case "productname" -> itemDraft.withProductName(conflicted(itemDraft.productName()));
            case "specifications" -> itemDraft.withSpecifications(conflicted(itemDraft.specifications()));
            case "quantity" -> itemDraft.withQuantity(conflicted(itemDraft.quantity()));
            case "unit" -> itemDraft.withUnit(conflicted(itemDraft.unit()));
            case "unitprice" -> itemDraft.withUnitPrice(conflicted(itemDraft.unitPrice()));
            default -> itemDraft;
        };
    }

    private <T> ExtractedField<T> conflicted(ExtractedField<T> extractedField) {
        return new ExtractedField<>(extractedField.value(), FieldStatus.CONFLICTED, extractedField.reviewReason());
    }

    private String joinIssues(RawExtractionResponse rawResponse) {
        String joinedIssues = String.join(" | ", rawResponse.sourceQuality().issues());
        return joinedIssues.length() <= MAX_ISSUE_TEXT_LENGTH
                ? joinedIssues
                : joinedIssues.substring(0, MAX_ISSUE_TEXT_LENGTH) + "...";
    }

    private record ItemDraft(
            ExtractedField<String> productName,
            ExtractedField<String> specifications,
            ExtractedField<BigDecimal> quantity,
            ExtractedField<String> unit,
            ExtractedField<BigDecimal> unitPrice) {

        ItemDraft withProductName(ExtractedField<String> productName) {
            return new ItemDraft(productName, specifications, quantity, unit, unitPrice);
        }

        ItemDraft withSpecifications(ExtractedField<String> specifications) {
            return new ItemDraft(productName, specifications, quantity, unit, unitPrice);
        }

        ItemDraft withQuantity(ExtractedField<BigDecimal> quantity) {
            return new ItemDraft(productName, specifications, quantity, unit, unitPrice);
        }

        ItemDraft withUnit(ExtractedField<String> unit) {
            return new ItemDraft(productName, specifications, quantity, unit, unitPrice);
        }

        ItemDraft withUnitPrice(ExtractedField<BigDecimal> unitPrice) {
            return new ItemDraft(productName, specifications, quantity, unit, unitPrice);
        }
    }

    /** Translated application draft plus review warnings. */
    public record TranslatedDraft(QuotationExtractionResponse draft, List<ExtractionWarning> warnings) {
    }
}
