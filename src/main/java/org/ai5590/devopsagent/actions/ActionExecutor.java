package org.ai5590.devopsagent.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai5590.devopsagent.audit.AuditService;
import org.ai5590.devopsagent.sshagent.SshAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);
    private final SshAgentService sshAgentService;
    private final AuditService auditService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ActionExecutor(SshAgentService sshAgentService, AuditService auditService) {
        this.sshAgentService = sshAgentService;
        this.auditService = auditService;
    }

    public Map<String, Object> executeAction(String userLogin, String actionsJson, String actionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            JsonNode root = mapper.readTree(actionsJson);
            JsonNode actions = root.path("actions");
            JsonNode action = null;
            for (JsonNode a : actions) {
                if (actionId.equals(a.path("id").asText())) {
                    action = a;
                    break;
                }
            }

            if (action == null) {
                result.put("success", false);
                result.put("error", "Action not found: " + actionId);
                return result;
            }

            String api = action.path("api").asText();
            long startTime = System.currentTimeMillis();
            String output;
            String server = null;
            String command = null;

            if ("ssh.list_servers".equals(api)) {
                output = sshAgentService.listServers();
            } else if ("ssh.execute".equals(api)) {
                server = action.path("params").path("server").asText();
                command = action.path("params").path("command").asText();
                output = sshAgentService.execute(server, command);
            } else {
                result.put("success", false);
                result.put("error", "Unknown API: " + api);
                return result;
            }

            long duration = System.currentTimeMillis() - startTime;
            auditService.logAction(userLogin, api, server, command, duration, output);

            result.put("success", true);
            result.put("output", output);
            result.put("api", api);
            result.put("server", server);
            result.put("command", command);
            result.put("duration_ms", duration);

        } catch (Exception e) {
            log.error("Action execution failed: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
