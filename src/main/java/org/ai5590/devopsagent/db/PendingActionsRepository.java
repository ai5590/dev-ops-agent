package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class PendingActionsRepository {
    private static final Logger log = LoggerFactory.getLogger(PendingActionsRepository.class);
    private final DatabaseInitializer db;

    public PendingActionsRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public void savePendingActions(String userLogin, String actionsJson) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM pending_actions WHERE user_login = ?")) {
                del.setString(1, userLogin);
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO pending_actions (user_login, actions_json) VALUES (?, ?)")) {
                ps.setString(1, userLogin);
                ps.setString(2, actionsJson);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error saving pending actions: {}", e.getMessage());
        }
    }

    public String getPendingActions(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT actions_json FROM pending_actions WHERE user_login = ? ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("actions_json");
        } catch (SQLException e) {
            log.error("Error getting pending actions: {}", e.getMessage());
        }
        return null;
    }

    public void clearPendingActions(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_actions WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error clearing pending actions: {}", e.getMessage());
        }
    }
}
