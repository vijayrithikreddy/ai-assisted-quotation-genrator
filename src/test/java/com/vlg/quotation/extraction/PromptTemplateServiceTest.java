package com.vlg.quotation.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class PromptTemplateServiceTest {

    private final PromptTemplateService promptService = new PromptTemplateService(new DefaultResourceLoader());

    @Test
    void shouldRenderVersionedPromptWithWorkflow() {
        String renderedPrompt = promptService.renderPrompt("v1", "NEW_CUSTOMER", "Saved details: none.");

        assertThat(renderedPrompt).contains("NEW_CUSTOMER");
        assertThat(renderedPrompt).contains("Saved details: none.");
        assertThat(renderedPrompt).doesNotContain("{{WORKFLOW}}");
        assertThat(renderedPrompt).doesNotContain("{{CUSTOMER_CONTEXT}}");
        assertThat(renderedPrompt).doesNotContain("{{SCHEMA_VERSION}}");
        assertThat(renderedPrompt).contains("JSON only");
    }

    @Test
    void shouldRejectBadVersion() {
        assertThatThrownBy(() -> promptService.renderPrompt("../secret", "NEW_CUSTOMER", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailLoudlyOnMissingPromptResource() {
        assertThatThrownBy(() -> promptService.renderPrompt("v9", "NEW_CUSTOMER", ""))
                .isInstanceOf(IllegalStateException.class);
    }
}
