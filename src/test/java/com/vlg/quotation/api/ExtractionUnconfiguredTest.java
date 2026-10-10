package com.vlg.quotation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Without an API key the endpoint must fail safe with a manual-entry
 * message instead of calling any provider.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExtractionUnconfiguredTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRefuseExtractionWithoutApiKey() throws Exception {
        BufferedImage testImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGraphics = testImage.createGraphics();
        imageGraphics.setColor(Color.WHITE);
        imageGraphics.fillRect(0, 0, 10, 10);
        imageGraphics.dispose();
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", imageBytes);

        mockMvc.perform(multipart("/api/v1/extractions")
                        .file(new MockMultipartFile(
                                "quotationImage", "quote.png", "image/png", imageBytes.toByteArray()))
                        .param("customerMode", "NEW"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("manually")));
    }
}
