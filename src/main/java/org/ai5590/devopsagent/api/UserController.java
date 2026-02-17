package org.ai5590.devopsagent.api;

import org.ai5590.devopsagent.db.UserSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserSettingsRepository userSettingsRepository;

    public UserController(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    @GetMapping("/id")
    public ResponseEntity<Map<String, Object>> getUserId(Authentication auth) {
        boolean showDebug = userSettingsRepository.getShowDebug(auth.getName());
        return ResponseEntity.ok(Map.of("login", auth.getName(), "showDebug", showDebug));
    }

    @PostMapping("/settings/debug")
    public ResponseEntity<Map<String, Object>> toggleDebug(@RequestBody Map<String, Boolean> body, Authentication auth) {
        boolean show = body.getOrDefault("showDebug", false);
        userSettingsRepository.setShowDebug(auth.getName(), show);
        return ResponseEntity.ok(Map.of("showDebug", show));
    }
}
