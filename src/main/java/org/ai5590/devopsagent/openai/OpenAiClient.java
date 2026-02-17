package org.ai5590.devopsagent.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class OpenAiClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String chat(String systemPrompt, List<Map<String, Object>> history, String model) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);

            ArrayNode messages = body.putArray("messages");
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);

            for (Map<String, Object> msg : history) {
                ObjectNode m = messages.addObject();
                m.put("role", (String) msg.get("role"));
                m.put("content", (String) msg.get("content"));
            }

            body.put("max_tokens", 4096);
            body.put("temperature", 0.7);

            String jsonBody = mapper.writeValueAsString(body);
            String url = baseUrl + "/chat/completions";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            log.info("OpenAI request: model={}, url={}", model, url);
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} {}", response.statusCode(), response.body());
                return "Ошибка AI-сервиса: HTTP " + response.statusCode();
            }

            JsonNode root = mapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("OpenAI call failed: {}", e.getMessage(), e);
            return "Ошибка связи с AI-сервисом: " + e.getMessage();
        }
    }
}
