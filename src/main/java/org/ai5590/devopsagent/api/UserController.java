package org.ai5590.devopsagent.api;

import org.ai5590.devopsagent.config.AppConfig;
import org.ai5590.devopsagent.config.ConfigLoader;
import org.ai5590.devopsagent.db.UserSettingsRepository;
import org.ai5590.devopsagent.sshagent.SshAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserSettingsRepository userSettingsRepository;
    private final ConfigLoader configLoader;
    private final SshAgentService sshAgentService;

    public UserController(UserSettingsRepository userSettingsRepository, ConfigLoader configLoader, SshAgentService sshAgentService) {
        this.userSettingsRepository = userSettingsRepository;
        this.configLoader = configLoader;
        this.sshAgentService = sshAgentService;
    }

    @GetMapping("/user/id")
    public ResponseEntity<Map<String, Object>> getUserId(Authentication auth) {
        Map<String, Object> settings = userSettingsRepository.getSettings(auth.getName());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("login", auth.getName());
        result.putAll(settings);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/settings/debug")
    public ResponseEntity<Map<String, Object>> toggleDebug(@RequestBody Map<String, Boolean> body, Authentication auth) {
        boolean show = body.getOrDefault("showDebug", false);
        userSettingsRepository.setShowDebug(auth.getName(), show);
        return ResponseEntity.ok(Map.of("showDebug", show));
    }

    @GetMapping("/user/settings")
    public ResponseEntity<Map<String, Object>> getSettings(Authentication auth) {
        Map<String, Object> settings = userSettingsRepository.getSettings(auth.getName());
        settings.put("login", auth.getName());
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/user/settings")
    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> body, Authentication auth) {
        boolean showDebug = Boolean.TRUE.equals(body.get("showDebug"));
        String selectedLlmServerId = (String) body.get("selectedLlmServerId");
        String modelOverride = (String) body.get("modelOverride");
        userSettingsRepository.saveSettings(auth.getName(), showDebug, selectedLlmServerId, modelOverride);
        return ResponseEntity.ok(Map.of("success", true, "message", "Настройки сохранены"));
    }

    @GetMapping("/llm-servers")
    public ResponseEntity<Map<String, Object>> getLlmServers(Authentication auth) {
        AppConfig config = configLoader.getConfig();
        List<Map<String, Object>> servers = new ArrayList<>();
        for (AppConfig.LlmServer s : config.getEnabledLlmServers()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("type", s.getType());
            m.put("defaultModel", s.getDefaultModel());
            servers.add(m);
        }
        String defaultId = config.getDefaults().getDefaultLlmServerId();
        return ResponseEntity.ok(Map.of("servers", servers, "defaultServerId", defaultId));
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfig(Authentication auth) {
        configLoader.reloadAll();
        return ResponseEntity.ok(Map.of("success", true, "message", "Настройки обновлены"));
    }

    @GetMapping("/servers")
    public ResponseEntity<Map<String, Object>> getServers(Authentication auth) {
        String result = sshAgentService.listServers();
        return ResponseEntity.ok(Map.of("result", result));
    }
}
