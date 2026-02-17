package org.ai5590.devopsagent.api;

import org.ai5590.devopsagent.actions.ActionExecutor;
import org.ai5590.devopsagent.db.PendingActionsRepository;
import org.ai5590.devopsagent.db.MessageRepository;
import org.ai5590.devopsagent.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ActionExecutor actionExecutor;
    private final PendingActionsRepository pendingActionsRepository;
    private final MessageRepository messageRepository;

    public ChatController(ChatService chatService, ActionExecutor actionExecutor,
                          PendingActionsRepository pendingActionsRepository,
                          MessageRepository messageRepository) {
        this.chatService = chatService;
        this.actionExecutor = actionExecutor;
        this.pendingActionsRepository = pendingActionsRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(@RequestBody Map<String, String> body, Authentication auth) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        Map<String, Object> result = chatService.sendMessage(auth.getName(), text.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String, Object>> newChat(Authentication auth) {
        chatService.newChat(auth.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/action/{id}")
    public ResponseEntity<Map<String, Object>> executeAction(@PathVariable("id") String actionId, Authentication auth) {
        String userLogin = auth.getName();
        String actionsJson = pendingActionsRepository.getPendingActions(userLogin);
        if (actionsJson == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No pending actions"));
        }
        Map<String, Object> result = actionExecutor.executeAction(userLogin, actionsJson, actionId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            String output = (String) result.get("output");
            String api = (String) result.get("api");
            String resultMsg = "Action result (" + api + "):\n```\n" + output + "\n```";
            messageRepository.addMessage(userLogin, "assistant", resultMsg);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState(@RequestParam(value = "since", defaultValue = "0") long sinceId, Authentication auth) {
        Map<String, Object> state = chatService.getState(auth.getName(), sinceId);
        return ResponseEntity.ok(state);
    }
}
