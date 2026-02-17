package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;

@Repository
public class AuditRepository {
    private static final Logger log = LoggerFactory.getLogger(AuditRepository.class);
    private final DatabaseInitializer db;

    public AuditRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public void addAuditEntry(String login, String action, String server, String command, long durationMs, String resultSnippet) {
        String sql = "INSERT INTO audit (timestamp, login, action, server, command, duration_ms, result_snippet) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Instant.now().toString());
            ps.setString(2, login);
            ps.setString(3, action);
            ps.setString(4, server);
            ps.setString(5, command);
            ps.setLong(6, durationMs);
            ps.setString(7, resultSnippet != null && resultSnippet.length() > 500 ? resultSnippet.substring(0, 500) : resultSnippet);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error adding audit entry: {}", e.getMessage());
        }
    }
}
