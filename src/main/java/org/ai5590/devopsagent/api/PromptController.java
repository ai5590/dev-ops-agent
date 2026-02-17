package org.ai5590.devopsagent.api;

import org.ai5590.devopsagent.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prompt")
public class PromptController {
    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping("/start-update")
    public ResponseEntity<Map<String, Object>> startUpdate(Authentication auth) {
        return ResponseEntity.ok(promptService.startUpdate(auth.getName()));
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, String> body, Authentication auth) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        return ResponseEntity.ok(promptService.submitPrompt(auth.getName(), text.trim()));
    }
}
