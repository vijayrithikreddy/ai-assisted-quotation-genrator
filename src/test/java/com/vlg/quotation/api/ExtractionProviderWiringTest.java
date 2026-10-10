package com.vlg.quotation.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vlg.quotation.configuration.LlmProperties;
import com.vlg.quotation.extraction.FakeExtractionProvider;
import com.vlg.quotation.extraction.QuotationExtractionProviderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Guards configuration binding and provider wiring: YAML defaults must reach
 * the properties bean (empty placeholders must not silently win), test
 * overrides must apply, and the factory must resolve the fake by id.
 */
@SpringBootTest(properties = {"llm.provider=test-fake", "llm.api-key=test-key"})
class ExtractionProviderWiringTest {

    @TestConfiguration
    static class FakeProviderConfiguration {
        @Bean
        FakeExtractionProvider fakeExtractionProvider() {
            return new FakeExtractionProvider();
        }
    }

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private QuotationExtractionProviderFactory providerFactory;

    @Test
    void shouldBindOverridesOverYamlDefaults() {
        assertThat(llmProperties.getProvider()).isEqualTo("test-fake");
        assertThat(llmProperties.getApiKey()).isEqualTo("test-key");
        assertThat(llmProperties.getModel()).isEqualTo("gemini-3.8-flash");
        assertThat(llmProperties.isConfigured()).isTrue();
        assertThat(providerFactory.select(llmProperties.getProvider())).isInstanceOf(FakeExtractionProvider.class);
    }
}
