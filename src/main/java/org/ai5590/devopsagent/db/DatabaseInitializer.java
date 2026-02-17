package org.ai5590.devopsagent.db;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String DB_PATH = "data/app.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    @PostConstruct
    public void init() {
        new File("data").mkdirs();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    prompt_part1_override TEXT,
                    pending_prompt_update INTEGER DEFAULT 0
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_login TEXT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_settings (
                    user_login TEXT PRIMARY KEY,
                    show_debug INTEGER DEFAULT 0
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS audit (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    login TEXT NOT NULL,
                    action TEXT NOT NULL,
                    server TEXT,
                    command TEXT,
                    duration_ms INTEGER,
                    result_snippet TEXT
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pending_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_login TEXT NOT NULL,
                    actions_json TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_messages_user ON messages(user_login)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_login ON audit(login)");
            log.info("Database initialized at {}", DB_PATH);
        } catch (SQLException e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
