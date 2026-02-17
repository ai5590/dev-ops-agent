package org.ai5590.devopsagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_PATH = "data/config.json";
    private AppConfig config;

    @PostConstruct
    public void init() {
        load();
    }

    public void load() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                log.error("Config file not found: {}", CONFIG_PATH);
                config = new AppConfig();
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(file, AppConfig.class);
            log.info("Config loaded from {}", CONFIG_PATH);
        } catch (IOException e) {
            log.error("Failed to load config: {}", e.getMessage());
            config = new AppConfig();
        }
    }

    public AppConfig getConfig() {
        return config;
    }

    public String loadSystemPromptPart1Default() {
        try {
            return Files.readString(Path.of("data/system_prompt_part1_default.txt"));
        } catch (IOException e) {
            log.warn("Could not load default system prompt part1: {}", e.getMessage());
            return "You are a DevOps assistant.";
        }
    }

    public String loadSystemPromptPart2() {
        try {
            return Files.readString(Path.of("data/system_prompt_part2_apis.md"));
        } catch (IOException e) {
            log.warn("Could not load system prompt part2: {}", e.getMessage());
            return "";
        }
    }
}
