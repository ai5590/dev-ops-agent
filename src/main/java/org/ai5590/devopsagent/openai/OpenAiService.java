package org.ai5590.devopsagent.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ai5590.devopsagent.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private final ConfigLoader configLoader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiService(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public String chat(String systemPrompt, List<Map<String, Object>> history) {
        try {
            var config = configLoader.getConfig();
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.getOpenaiModel());

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
            String url = config.getOpenaiBaseUrl() + "/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getOpenaiApiKey())
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Sending request to OpenAI: model={}", config.getOpenaiModel());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} {}", response.statusCode(), response.body());
                return "Error from AI service: " + response.statusCode();
            }

            JsonNode root = mapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("OpenAI call failed: {}", e.getMessage(), e);
            return "Error communicating with AI service: " + e.getMessage();
        }
    }
}
