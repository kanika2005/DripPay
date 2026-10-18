package com.drippay.ai;

import com.drippay.config.DrippayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class LlmInsightsClient {

    private static final Logger log = LoggerFactory.getLogger(LlmInsightsClient.class);

    private final DrippayProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public LlmInsightsClient(DrippayProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.getAi().isEnabled();
    }

    /**
     * @param ledgerContext factual JSON/text from read-only tools
     */
    public String complete(String userMessage, String ledgerContext) {
        if (!isEnabled()) {
            return null;
        }
        try {
            String system = "You are DripPay's read-only money assistant. Answer using ONLY the ledger context below. "
                    + "Never instruct users to bypass security. You cannot move money; if they want to pay someone, "
                    + "tell them to confirm a payment draft with their transfer PIN in the app. "
                    + "If context is insufficient, say what data is missing.\n\nLEDGER CONTEXT:\n" + ledgerContext;
            Map<String, Object> body = Map.of(
                    "model", properties.getAi().getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 600,
                    "temperature", 0.2
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getAi().getBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                    .header("Authorization", "Bearer " + properties.getAi().getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("LLM HTTP {}: {}", response.statusCode(), response.body());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("LLM call failed: {}", e.getMessage());
            return null;
        }
    }
}
