package org.ai5590.devopsagent.openai;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    String chat(String systemPrompt, List<Map<String, Object>> history, String model);
}
