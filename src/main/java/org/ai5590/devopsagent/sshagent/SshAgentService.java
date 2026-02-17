package org.ai5590.devopsagent.sshagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class SshAgentService {
    private static final Logger log = LoggerFactory.getLogger(SshAgentService.class);
    private final ConfigLoader configLoader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SshAgentService(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public String listServers() {
        try {
            String url = configLoader.getConfig().getSshAgentBaseUrl() + "/servers";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            return root.path("result").asText(response.body());
        } catch (Exception e) {
            log.error("SSH agent listServers failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String execute(String server, String command) {
        try {
            String url = configLoader.getConfig().getSshAgentBaseUrl() + "/exec";
            ObjectNode body = mapper.createObjectNode();
            body.put("server", server);
            body.put("command", command);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            return root.path("result").asText(response.body());
        } catch (Exception e) {
            log.error("SSH agent execute failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
