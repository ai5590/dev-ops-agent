package org.ai5590.devopsagent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    private String openaiApiKey;
    private String openaiBaseUrl = "https://api.openai.com/v1";
    private String openaiModel = "gpt-4o-mini";
    private String sshAgentBaseUrl = "http://127.0.0.1:25005";
    private String bootstrapUsersMode = "UPSERT";
    private List<BootstrapUser> bootstrapUsers = List.of();

    public static class BootstrapUser {
        private String login;
        private String password;
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    public String getOpenaiBaseUrl() { return openaiBaseUrl; }
    public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }
    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }
    public String getSshAgentBaseUrl() { return sshAgentBaseUrl; }
    public void setSshAgentBaseUrl(String sshAgentBaseUrl) { this.sshAgentBaseUrl = sshAgentBaseUrl; }
    public String getBootstrapUsersMode() { return bootstrapUsersMode; }
    public void setBootstrapUsersMode(String bootstrapUsersMode) { this.bootstrapUsersMode = bootstrapUsersMode; }
    public List<BootstrapUser> getBootstrapUsers() { return bootstrapUsers; }
    public void setBootstrapUsers(List<BootstrapUser> bootstrapUsers) { this.bootstrapUsers = bootstrapUsers; }
}
