package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageRepository.class);
    private final DatabaseInitializer db;

    public MessageRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public long addMessage(String userLogin, String role, String content) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO messages (user_login, role, content) VALUES (?, ?, ?)")) {
                ps.setString(1, userLogin);
                ps.setString(2, role);
                ps.setString(3, content);
                ps.executeUpdate();
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error adding message: {}", e.getMessage());
        }
        return -1;
    }

    public List<Map<String, Object>> getLastMessages(String userLogin, int limit) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        String sql = "SELECT id, role, content, created_at FROM messages WHERE user_login = ? ORDER BY id DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userLogin);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("role", rs.getString("role"));
                m.put("content", rs.getString("content"));
                m.put("created_at", rs.getString("created_at"));
                msgs.add(m);
            }
        } catch (SQLException e) {
            log.error("Error getting messages: {}", e.getMessage());
        }
        Collections.reverse(msgs);
        return msgs;
    }

    public List<Map<String, Object>> getMessagesSince(String userLogin, long sinceId) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        String sql = "SELECT id, role, content, created_at FROM messages WHERE user_login = ? AND id > ? ORDER BY id ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userLogin);
            ps.setLong(2, sinceId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("role", rs.getString("role"));
                m.put("content", rs.getString("content"));
                m.put("created_at", rs.getString("created_at"));
                msgs.add(m);
            }
        } catch (SQLException e) {
            log.error("Error getting messages since: {}", e.getMessage());
        }
        return msgs;
    }

    public int getMessageCount(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM messages WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error counting messages: {}", e.getMessage());
        }
        return 0;
    }

    public void deleteAllMessages(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting messages: {}", e.getMessage());
        }
    }
}
