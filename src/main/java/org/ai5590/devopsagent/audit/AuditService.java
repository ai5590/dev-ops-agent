package org.ai5590.devopsagent.audit;

import org.ai5590.devopsagent.db.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logAction(String login, String action, String server, String command, long durationMs, String result) {
        String snippet = result != null && result.length() > 200 ? result.substring(0, 200) + "..." : result;
        AUDIT.info("login={} action={} server={} command={} duration_ms={} result={}",
                login, action, server, command, durationMs, snippet);
        auditRepository.addAuditEntry(login, action, server, command, durationMs, snippet);
    }
}
