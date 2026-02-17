package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class UserSettingsRepository {
    private static final Logger log = LoggerFactory.getLogger(UserSettingsRepository.class);
    private final DatabaseInitializer db;

    public UserSettingsRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public Map<String, Object> getSettings(String userLogin) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("showDebug", false);
        settings.put("selectedLlmServerId", null);
        settings.put("modelOverride", null);
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT show_debug, selected_llm_server_id, model_override FROM user_settings WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                settings.put("showDebug", rs.getInt("show_debug") == 1);
                settings.put("selectedLlmServerId", rs.getString("selected_llm_server_id"));
                settings.put("modelOverride", rs.getString("model_override"));
            }
        } catch (SQLException e) {
            log.error("Error getting settings: {}", e.getMessage());
        }
        return settings;
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

    public String getSelectedLlmServerId(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT selected_llm_server_id FROM user_settings WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("selected_llm_server_id");
        } catch (SQLException e) {
            log.error("Error getting selectedLlmServerId: {}", e.getMessage());
        }
        return null;
    }

    public String getModelOverride(String userLogin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT model_override FROM user_settings WHERE user_login = ?")) {
            ps.setString(1, userLogin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("model_override");
        } catch (SQLException e) {
            log.error("Error getting modelOverride: {}", e.getMessage());
        }
        return null;
    }

    public void saveSettings(String userLogin, boolean showDebug, String selectedLlmServerId, String modelOverride) {
        String sql = "INSERT INTO user_settings (user_login, show_debug, selected_llm_server_id, model_override) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(user_login) DO UPDATE SET show_debug = ?, selected_llm_server_id = ?, model_override = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userLogin);
            ps.setInt(2, showDebug ? 1 : 0);
            ps.setString(3, selectedLlmServerId);
            ps.setString(4, modelOverride);
            ps.setInt(5, showDebug ? 1 : 0);
            ps.setString(6, selectedLlmServerId);
            ps.setString(7, modelOverride);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error saving settings: {}", e.getMessage());
        }
    }
}
