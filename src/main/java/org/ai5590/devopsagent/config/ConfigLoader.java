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
    private volatile String cachedPrompt;

    @PostConstruct
    public void init() {
        load();
        reloadPrompt();
    }

    public synchronized void load() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                log.error("Config file not found: {}", CONFIG_PATH);
                config = new AppConfig();
                config.ensureDefaults();
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(file, AppConfig.class);
            config.ensureDefaults();
            log.info("Config loaded from {}", CONFIG_PATH);
        } catch (IOException e) {
            log.error("Failed to load config: {}", e.getMessage());
            config = new AppConfig();
            config.ensureDefaults();
        }
    }

    public void reloadPrompt() {
        cachedPrompt = loadPromptFromFile();
        log.info("Prompt reloaded");
    }

    public synchronized void reloadAll() {
        load();
        reloadPrompt();
        log.info("All settings reloaded");
    }

    private String loadPromptFromFile() {
        try {
            Path promptRu = Path.of("data/prompt_ru.txt");
            if (Files.exists(promptRu)) {
                return Files.readString(promptRu);
            }
        } catch (IOException e) {
            log.warn("Could not load prompt_ru.txt: {}", e.getMessage());
        }
        try {
            return Files.readString(Path.of("data/system_prompt_part1_default.txt"));
        } catch (IOException e) {
            log.warn("Could not load default system prompt: {}", e.getMessage());
            return "Ты — DevOps-ассистент. Помогай пользователям управлять серверами.";
        }
    }

    public AppConfig getConfig() {
        return config;
    }

    public String getSystemPrompt() {
        if (cachedPrompt == null) {
            cachedPrompt = loadPromptFromFile();
        }
        String part2 = loadSystemPromptPart2();
        return cachedPrompt + "\n\n" + part2;
    }

    public String getCachedPromptPart1() {
        return cachedPrompt != null ? cachedPrompt : loadPromptFromFile();
    }

    public String loadSystemPromptPart1Default() {
        return getCachedPromptPart1();
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
