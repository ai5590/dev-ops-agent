package org.ai5590.devopsagent.service;

import org.ai5590.devopsagent.actions.ActionParser;
import org.ai5590.devopsagent.config.ConfigLoader;
import org.ai5590.devopsagent.db.MessageRepository;
import org.ai5590.devopsagent.db.PendingActionsRepository;
import org.ai5590.devopsagent.db.UserRepository;
import org.ai5590.devopsagent.openai.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MESSAGE_LIMIT = 30;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PendingActionsRepository pendingActionsRepository;
    private final OpenAiService openAiService;
    private final ConfigLoader configLoader;
    private final ActionParser actionParser;

    public ChatService(MessageRepository messageRepository, UserRepository userRepository,
                       PendingActionsRepository pendingActionsRepository, OpenAiService openAiService,
                       ConfigLoader configLoader, ActionParser actionParser) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.pendingActionsRepository = pendingActionsRepository;
        this.openAiService = openAiService;
        this.configLoader = configLoader;
        this.actionParser = actionParser;
    }

    public Map<String, Object> sendMessage(String userLogin, String text) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (userRepository.isPendingPromptUpdate(userLogin)) {
            userRepository.setPromptOverride(userLogin, text);
            userRepository.setPendingPromptUpdate(userLogin, false);
            response.put("promptUpdated", true);
            response.put("message", "Системный промпт обновлён.");
            return response;
        }

        messageRepository.addMessage(userLogin, "user", text);
        int msgCount = messageRepository.getMessageCount(userLogin);
        boolean limitReached = msgCount >= MESSAGE_LIMIT;

        List<Map<String, Object>> history = messageRepository.getLastMessages(userLogin, MESSAGE_LIMIT);

        String promptOverride = userRepository.getPromptOverride(userLogin);
        String part1 = (promptOverride != null && !promptOverride.isBlank())
                ? promptOverride : configLoader.getCachedPromptPart1();
        String part2 = configLoader.loadSystemPromptPart2();
        String systemPrompt = part1 + "\n\n" + part2;

        String aiResponse = openAiService.chat(systemPrompt, history, userLogin);
        ActionParser.ParseResult parsed = actionParser.parse(aiResponse);

        String displayText = parsed.getTextContent();
        if (limitReached) {
            displayText = "\u26a0\ufe0f История диалога достигла лимита (30 сообщений). Самые старые сообщения будут вытесняться.\n\n" + displayText;
        }

        messageRepository.addMessage(userLogin, "assistant", parsed.getTextContent());

        if (parsed.hasActions()) {
            pendingActionsRepository.savePendingActions(userLogin, parsed.getActionsJson());
        } else {
            pendingActionsRepository.clearPendingActions(userLogin);
        }

        response.put("text", displayText);
        response.put("hasActions", parsed.hasActions());
        if (parsed.hasActions()) {
            response.put("actionsJson", parsed.getActionsJson());
        }
        response.put("limitReached", limitReached);
        return response;
    }

    public void newChat(String userLogin) {
        messageRepository.deleteAllMessages(userLogin);
        pendingActionsRepository.clearPendingActions(userLogin);
    }

    public Map<String, Object> getState(String userLogin, long sinceId) {
        Map<String, Object> state = new LinkedHashMap<>();
        List<Map<String, Object>> msgs;
        if (sinceId <= 0) {
            msgs = messageRepository.getLastMessages(userLogin, MESSAGE_LIMIT);
        } else {
            msgs = messageRepository.getMessagesSince(userLogin, sinceId);
        }
        state.put("messages", msgs);
        String actionsJson = pendingActionsRepository.getPendingActions(userLogin);
        state.put("actionsJson", actionsJson);
        state.put("hasActions", actionsJson != null);
        return state;
    }
}
