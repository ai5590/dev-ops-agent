package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class UserSettingsRepository {
    private static final Logger log = LoggerFactory.getLogger(UserSettingsRepository.class);
    private final DatabaseInitializer db;

    public UserSettingsRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public boolean getShowDebug(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT show_debug FROM user_settings WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("show_debug") == 1;
        } catch (SQLException e) {
            log.error("Error getting show_debug: {}", e.getMessage());
        }
        return false;
    }

    public void setShowDebug(String userLogin, boolean showDebug) {
        String sql = "INSERT INTO user_settings (user_login, show_debug) VALUES (?, ?) ON CONFLICT(user_login) DO UPDATE SET show_debug = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userLogin);
            ps.setInt(2, showDebug ? 1 : 0);
            ps.setInt(3, showDebug ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting show_debug: {}", e.getMessage());
        }
    }
}
