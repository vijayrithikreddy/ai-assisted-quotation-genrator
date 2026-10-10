package com.vlg.quotation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the VLG quotation processing API.
 *
 * <p>Phase 0/1 scope: stateless processing API (configuration, health,
 * error handling). No quotation persistence here; permanent records stay
 * in Android local storage per the local-first decision.</p>
 */
@OpenAPIDefinition(
        info = @Info(
                title = "VLG Quotation API",
                version = "0.0.1",
                description = "Stateless processing API for VLG handwritten quotation digitization. "
                        + "Phase 0/1 covers setup, configuration, health, and error handling."))
@ConfigurationPropertiesScan("com.vlg.quotation.configuration")
@SpringBootApplication
public class QuotationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuotationApiApplication.class, args);
    }
}
