package org.ai5590.devopsagent.service;

import org.ai5590.devopsagent.config.ConfigLoader;
import org.ai5590.devopsagent.db.UserRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PromptService {
    private final UserRepository userRepository;
    private final ConfigLoader configLoader;

    public PromptService(UserRepository userRepository, ConfigLoader configLoader) {
        this.userRepository = userRepository;
        this.configLoader = configLoader;
    }

    public Map<String, Object> startUpdate(String userLogin) {
        userRepository.setPendingPromptUpdate(userLogin, true);
        String currentPrompt = userRepository.getPromptOverride(userLogin);
        if (currentPrompt == null || currentPrompt.isBlank()) {
            currentPrompt = configLoader.getCachedPromptPart1();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentPrompt", currentPrompt);
        result.put("pending", true);
        return result;
    }

    public Map<String, Object> submitPrompt(String userLogin, String text) {
        userRepository.setPromptOverride(userLogin, text);
        userRepository.setPendingPromptUpdate(userLogin, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", true);
        return result;
    }
}
