package org.ai5590.devopsagent.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private final LlmClientFactory clientFactory;

    public OpenAiService(LlmClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public String chat(String systemPrompt, List<Map<String, Object>> history, String userLogin) {
        LlmClient client = clientFactory.getClientForUser(userLogin);
        String model = clientFactory.getModelForUser(userLogin);
        log.info("Chat request for user={}, model={}", userLogin, model);
        return client.chat(systemPrompt, history, model);
    }
}
