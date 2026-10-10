package com.vlg.quotation.extraction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Loads versioned extraction prompts from the classpath and binds workflow
 * context. Prompts stay out of Java string literals so wording can improve
 * without touching business logic.
 */
@Service
public class PromptTemplateService {

    private static final Pattern PROMPT_VERSION_PATTERN = Pattern.compile("^v[0-9]+$");

    private final ResourceLoader resourceLoader;

    public PromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Renders {@code prompts/quotation-extraction-{version}.txt}.
     *
     * @param promptVersion configured prompt version, e.g. {@code v1}
     * @param workflow provider workflow token, e.g. {@code NEW_CUSTOMER}
     * @param customerContext saved-customer context for existing mode, or blank
     */
    public String renderPrompt(String promptVersion, String workflow, String customerContext) {
        if (!StringUtils.hasText(promptVersion) || !PROMPT_VERSION_PATTERN.matcher(promptVersion).matches()) {
            throw new IllegalArgumentException("Unsupported extraction prompt version: " + promptVersion);
        }
        String templateLocation = "classpath:prompts/quotation-extraction-" + promptVersion + ".txt";
        try {
            String templateText = resourceLoader
                    .getResource(templateLocation)
                    .getContentAsString(StandardCharsets.UTF_8);
            return templateText
                    .replace("{{WORKFLOW}}", workflow)
                    .replace(
                            "{{CUSTOMER_CONTEXT}}",
                            StringUtils.hasText(customerContext)
                                    ? customerContext
                                    : "(No saved customer context was supplied.)")
                    .replace("{{SCHEMA_VERSION}}", promptVersion);
        } catch (IOException missingPrompt) {
            throw new IllegalStateException("Extraction prompt is not packaged: " + templateLocation, missingPrompt);
        }
    }
}
