package org.ai5590.devopsagent.openai;

import org.ai5590.devopsagent.config.AppConfig;
import org.ai5590.devopsagent.config.ConfigLoader;
import org.ai5590.devopsagent.db.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {
    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);
    private final ConfigLoader configLoader;
    private final UserSettingsRepository userSettingsRepository;

    public LlmClientFactory(ConfigLoader configLoader, UserSettingsRepository userSettingsRepository) {
        this.configLoader = configLoader;
        this.userSettingsRepository = userSettingsRepository;
    }

    public LlmClient getClientForUser(String userLogin) {
        AppConfig config = configLoader.getConfig();
        String serverId = userSettingsRepository.getSelectedLlmServerId(userLogin);
        if (serverId == null || serverId.isBlank()) {
            serverId = config.getDefaults().getDefaultLlmServerId();
        }
        AppConfig.LlmServer server = config.findLlmServer(serverId);
        if (server == null) {
            var enabled = config.getEnabledLlmServers();
            if (!enabled.isEmpty()) {
                server = enabled.get(0);
            } else {
                log.error("No LLM servers available");
                return (sys, hist, model) -> "Ошибка: нет доступных LLM серверов";
            }
        }
        return createClient(server);
    }

    public String getModelForUser(String userLogin) {
        AppConfig config = configLoader.getConfig();
        String override = userSettingsRepository.getModelOverride(userLogin);
        if (override != null && !override.isBlank()) {
            return override;
        }
        String serverId = userSettingsRepository.getSelectedLlmServerId(userLogin);
        if (serverId == null || serverId.isBlank()) {
            serverId = config.getDefaults().getDefaultLlmServerId();
        }
        AppConfig.LlmServer server = config.findLlmServer(serverId);
        if (server != null) {
            return server.getDefaultModel();
        }
        return "gpt-4o-mini";
    }

    private LlmClient createClient(AppConfig.LlmServer server) {
        String type = server.getType().toUpperCase();
        switch (type) {
            case "OLLAMA":
                return new OllamaClient(server.getBaseUrl());
            case "OPENAI":
            default:
                return new OpenAiClient(server.getBaseUrl(), server.resolveApiKey());
        }
    }
}
