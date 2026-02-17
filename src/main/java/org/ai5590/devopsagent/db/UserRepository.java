package org.ai5590.devopsagent.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;

@Repository
public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final DatabaseInitializer db;

    public UserRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public boolean existsByLogin(String login) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE login = ?")) {
            ps.setString(1, login);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            log.error("Error checking user existence: {}", e.getMessage());
            return false;
        }
    }

    public void createUser(String login, String passwordHash) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (login, password_hash) VALUES (?, ?)")) {
            ps.setString(1, login);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            log.info("Created user: {}", login);
        } catch (SQLException e) {
            log.error("Error creating user {}: {}", login, e.getMessage());
        }
    }

    public void upsertUser(String login, String passwordHash) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (login, password_hash) VALUES (?, ?) ON CONFLICT(login) DO UPDATE SET password_hash = ?")) {
            ps.setString(1, login);
            ps.setString(2, passwordHash);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
            log.info("Upserted user: {}", login);
        } catch (SQLException e) {
            log.error("Error upserting user {}: {}", login, e.getMessage());
        }
    }

    public Optional<String> getPasswordHash(String login) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE login = ?")) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("password_hash"));
        } catch (SQLException e) {
            log.error("Error getting password: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public String getPromptOverride(String login) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT prompt_part1_override FROM users WHERE login = ?")) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("prompt_part1_override");
        } catch (SQLException e) {
            log.error("Error getting prompt override: {}", e.getMessage());
        }
        return null;
    }

    public void setPromptOverride(String login, String override) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET prompt_part1_override = ? WHERE login = ?")) {
            ps.setString(1, override);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting prompt override: {}", e.getMessage());
        }
    }

    public boolean isPendingPromptUpdate(String login) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT pending_prompt_update FROM users WHERE login = ?")) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("pending_prompt_update") == 1;
        } catch (SQLException e) {
            log.error("Error checking pending prompt: {}", e.getMessage());
        }
        return false;
    }

    public void setPendingPromptUpdate(String login, boolean pending) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET pending_prompt_update = ? WHERE login = ?")) {
            ps.setInt(1, pending ? 1 : 0);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting pending prompt: {}", e.getMessage());
        }
    }
}
