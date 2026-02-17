package org.ai5590.devopsagent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    private String openaiApiKey;
    private String openaiBaseUrl;
    private String openaiModel;
    private String sshAgentBaseUrl = "http://127.0.0.1:25005";
    private String bootstrapUsersMode = "UPSERT";
    private List<BootstrapUser> bootstrapUsers = List.of();
    private Defaults defaults;
    private List<LlmServer> llmServers;

    public static class Defaults {
        private String defaultLlmServerId = "openai_default";
        public String getDefaultLlmServerId() { return defaultLlmServerId; }
        public void setDefaultLlmServerId(String v) { this.defaultLlmServerId = v; }
    }

    public static class LlmServer {
        private String id;
        private String title;
        private String type;
        private String baseUrl;
        private String apiKeyEnv = "";
        private String defaultModel;
        private boolean enabled = true;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKeyEnv() { return apiKeyEnv; }
        public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String resolveApiKey() {
            if (apiKeyEnv == null || apiKeyEnv.isBlank()) return "";
            String val = System.getenv(apiKeyEnv);
            return val != null ? val : "";
        }
    }

    public static class BootstrapUser {
        private String login;
        private String password;
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public void ensureDefaults() {
        if (defaults == null) {
            defaults = new Defaults();
        }
        if (llmServers == null || llmServers.isEmpty()) {
            llmServers = new ArrayList<>();
            LlmServer s = new LlmServer();
            s.setId("openai_default");
            s.setTitle("OpenAI");
            s.setType("OPENAI");
            s.setBaseUrl(openaiBaseUrl != null ? openaiBaseUrl : "https://api.openai.com/v1");
            s.setApiKeyEnv("OPENAI_API_KEY");
            s.setDefaultModel(openaiModel != null ? openaiModel : "gpt-4o-mini");
            s.setEnabled(true);
            llmServers.add(s);
        }
    }

    public LlmServer findLlmServer(String id) {
        if (llmServers == null) return null;
        for (LlmServer s : llmServers) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public List<LlmServer> getEnabledLlmServers() {
        if (llmServers == null) return List.of();
        List<LlmServer> result = new ArrayList<>();
        for (LlmServer s : llmServers) {
            if (s.isEnabled()) result.add(s);
        }
        return result;
    }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String v) { this.openaiApiKey = v; }
    public String getOpenaiBaseUrl() { return openaiBaseUrl; }
    public void setOpenaiBaseUrl(String v) { this.openaiBaseUrl = v; }
    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String v) { this.openaiModel = v; }
    public String getSshAgentBaseUrl() { return sshAgentBaseUrl; }
    public void setSshAgentBaseUrl(String v) { this.sshAgentBaseUrl = v; }
    public String getBootstrapUsersMode() { return bootstrapUsersMode; }
    public void setBootstrapUsersMode(String v) { this.bootstrapUsersMode = v; }
    public List<BootstrapUser> getBootstrapUsers() { return bootstrapUsers; }
    public void setBootstrapUsers(List<BootstrapUser> v) { this.bootstrapUsers = v; }
    public Defaults getDefaults() { return defaults; }
    public void setDefaults(Defaults v) { this.defaults = v; }
    public List<LlmServer> getLlmServers() { return llmServers; }
    public void setLlmServers(List<LlmServer> v) { this.llmServers = v; }
}
