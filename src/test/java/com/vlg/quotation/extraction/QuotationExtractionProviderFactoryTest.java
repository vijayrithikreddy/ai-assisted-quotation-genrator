package com.vlg.quotation.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuotationExtractionProviderFactoryTest {

    @Test
    void shouldSelectRegisteredProvider() {
        FakeExtractionProvider fakeProvider = new FakeExtractionProvider();
        QuotationExtractionProviderFactory providerFactory =
                new QuotationExtractionProviderFactory(List.of(fakeProvider));

        assertThat(providerFactory.select("test-fake")).isSameAs(fakeProvider);
    }

    @Test
    void shouldFailLoudlyOnUnknownProvider() {
        QuotationExtractionProviderFactory providerFactory =
                new QuotationExtractionProviderFactory(List.of(new FakeExtractionProvider()));

        assertThatThrownBy(() -> providerFactory.select("no-such-provider")).isInstanceOf(IllegalStateException.class);
    }
}
