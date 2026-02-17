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

public class OllamaClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String chat(String systemPrompt, List<Map<String, Object>> history, String model) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);

            ArrayNode messages = body.putArray("messages");
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);

            for (Map<String, Object> msg : history) {
                ObjectNode m = messages.addObject();
                m.put("role", (String) msg.get("role"));
                m.put("content", (String) msg.get("content"));
            }

            String jsonBody = mapper.writeValueAsString(body);
            String url = baseUrl + "/api/chat";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Ollama request: model={}, url={}", model, url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama API error: {} {}", response.statusCode(), response.body());
                return "Ошибка Ollama: HTTP " + response.statusCode();
            }

            JsonNode root = mapper.readTree(response.body());
            return root.path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Ollama call failed: {}", e.getMessage(), e);
            return "Ошибка связи с Ollama: " + e.getMessage();
        }
    }
}
