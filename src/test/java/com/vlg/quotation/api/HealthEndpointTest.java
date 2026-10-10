package com.vlg.quotation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vlg.quotation.common.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUpWithoutAiCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestIdFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("vlg-quotation-api"));
    }

    @Test
    void shouldEchoIncomingRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header(RequestIdFilter.REQUEST_ID_HEADER, "req-manual-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "req-manual-1"));
    }

    @Test
    void shouldExposeOpenApiDocument() throws Exception {
        mockMvc.perform(get("/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths./api/v1/health.get").exists());
    }

    @Test
    void shouldRejectPostOnHealthWithStableError() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .content("{}")
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
