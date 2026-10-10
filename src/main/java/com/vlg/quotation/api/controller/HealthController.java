package com.vlg.quotation.api.controller;

import com.vlg.quotation.api.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness probe. Deliberately independent of LLM configuration. */
@Tag(name = "System", description = "Health and capability probes")
@RestController
@RequestMapping(path = "/api/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private static final String SERVICE_NAME = "vlg-quotation-api";

    @Operation(summary = "Check backend liveness")
    @GetMapping
    public HealthResponse checkHealth() {
        return new HealthResponse("UP", SERVICE_NAME);
    }
}
