package org.ai5590.devopsagent.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActionParser {
    private static final Logger log = LoggerFactory.getLogger(ActionParser.class);
    private static final String START_MARKER = "---ACTIONS_JSON_START---";
    private static final String END_MARKER = "---ACTIONS_JSON_END---";
    private final ObjectMapper mapper = new ObjectMapper();

    public static class ParseResult {
        private final String textContent;
        private final String actionsJson;

        public ParseResult(String textContent, String actionsJson) {
            this.textContent = textContent;
            this.actionsJson = actionsJson;
        }

        public String getTextContent() { return textContent; }
        public String getActionsJson() { return actionsJson; }
        public boolean hasActions() { return actionsJson != null && !actionsJson.isBlank(); }
    }

    public ParseResult parse(String aiResponse) {
        if (aiResponse == null) return new ParseResult("", null);

        int startIdx = aiResponse.indexOf(START_MARKER);
        int endIdx = aiResponse.indexOf(END_MARKER);

        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return new ParseResult(aiResponse, null);
        }

        String textBefore = aiResponse.substring(0, startIdx).trim();
        String textAfter = aiResponse.substring(endIdx + END_MARKER.length()).trim();
        String text = (textBefore + "\n" + textAfter).trim();

        String json = aiResponse.substring(startIdx + START_MARKER.length(), endIdx).trim();

        try {
            mapper.readTree(json);
            return new ParseResult(text, json);
        } catch (Exception e) {
            log.warn("Failed to parse actions JSON: {}", e.getMessage());
            return new ParseResult(aiResponse, null);
        }
    }

    public JsonNode getActionsNode(String actionsJson) {
        try {
            return mapper.readTree(actionsJson);
        } catch (Exception e) {
            return null;
        }
    }
}
