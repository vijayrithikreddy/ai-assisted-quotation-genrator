package com.vlg.quotation.extraction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Selects the configured provider adapter without coupling callers to any vendor. */
@Component
public class QuotationExtractionProviderFactory {

    private final Map<String, QuotationExtractionProvider> providersById;

    public QuotationExtractionProviderFactory(List<QuotationExtractionProvider> extractionProviders) {
        this.providersById = extractionProviders.stream()
                .collect(Collectors.toMap(QuotationExtractionProvider::providerId, Function.identity()));
    }

    /** Returns the adapter for the configured id, or fails loudly on unknown ids. */
    public QuotationExtractionProvider select(String providerId) {
        QuotationExtractionProvider selectedProvider = providersById.get(providerId);
        if (selectedProvider == null) {
            throw new IllegalStateException("Unsupported LLM provider: " + providerId);
        }
        return selectedProvider;
    }
}
