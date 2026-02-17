# Database Schema — dev-ops-agent

## Overview

The application uses **SQLite** as its database, stored at `data/app.db`. Schema is created automatically on startup by `DatabaseInitializer.java`. No migrations framework is used — tables are created with `CREATE TABLE IF NOT EXISTS`.

## Tables

### 1. `users`

Stores user accounts for authentication and per-user prompt customization.

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    prompt_part1_override TEXT,
    pending_prompt_update INTEGER DEFAULT 0
)
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | INTEGER | NO | Auto-increment primary key |
| `login` | TEXT | NO | Unique username |
| `password_hash` | TEXT | NO | bcrypt-hashed password |
| `prompt_part1_override` | TEXT | YES | Per-user system prompt override (Part 1). NULL = use default |
| `pending_prompt_update` | INTEGER | NO | Flag (0/1) indicating user is in prompt-edit mode |

**Used by:** `UserRepository`, `CustomUserDetailsService`, `BootstrapService`

---

### 2. `messages`

Stores per-user chat history (both user messages and AI responses).

```sql
CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_login TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | INTEGER | NO | Auto-increment primary key |
| `user_login` | TEXT | NO | Username who owns this message |
| `role` | TEXT | NO | `user` or `assistant` |
| `content` | TEXT | NO | Message text content |
| `created_at` | TIMESTAMP | NO | Auto-set to current timestamp |

**Index:** `idx_messages_user ON messages(user_login)`

**Used by:** `MessageRepository`, `ChatService`

**Notes:**
- Last 30 messages per user are used as context window for OpenAI
- "New Chat" deletes all messages for the user
- Messages are retrieved in descending order (most recent first) then reversed for display

---

### 3. `user_settings`

Stores per-user UI preferences.

```sql
CREATE TABLE IF NOT EXISTS user_settings (
    user_login TEXT PRIMARY KEY,
    show_debug INTEGER DEFAULT 0
)
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `user_login` | TEXT | NO | Primary key, references user login |
| `show_debug` | INTEGER | NO | Flag (0/1) — whether to show debug info in the UI |

**Used by:** `UserSettingsRepository`, `UserController`

**Notes:**
- Uses `INSERT ... ON CONFLICT DO UPDATE` for upsert behavior
- Row created on first toggle, not on user creation

---

### 4. `audit`

Records every SSH action executed through the system for compliance and debugging.

```sql
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
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | INTEGER | NO | Auto-increment primary key |
| `timestamp` | TEXT | NO | ISO 8601 timestamp (from `Instant.now()`) |
| `login` | TEXT | NO | Username who initiated the action |
| `action` | TEXT | NO | API called: `ssh.list_servers` or `ssh.execute` |
| `server` | TEXT | YES | Target server (NULL for `ssh.list_servers`) |
| `command` | TEXT | YES | Command executed (NULL for `ssh.list_servers`) |
| `duration_ms` | INTEGER | YES | Execution time in milliseconds |
| `result_snippet` | TEXT | YES | First 500 characters of the result |

**Index:** `idx_audit_login ON audit(login)`

**Used by:** `AuditRepository`, `AuditService`

**Notes:**
- Result snippet is truncated to 500 chars in the repository and 200 chars in the log file
- Audit entries are write-only from the application — no read/query UI exists yet

---

### 5. `pending_actions`

Stores the latest action block proposed by the AI for each user, awaiting user approval.

```sql
CREATE TABLE IF NOT EXISTS pending_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_login TEXT NOT NULL,
    actions_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | INTEGER | NO | Auto-increment primary key |
| `user_login` | TEXT | NO | Username who owns these pending actions |
| `actions_json` | TEXT | NO | Full action JSON string from AI response |
| `created_at` | TIMESTAMP | NO | Auto-set to current timestamp |

**Used by:** `PendingActionsRepository`, `ChatController`, `ChatService`

**Notes:**
- Only one set of pending actions per user — old ones are deleted before inserting new
- Cleared when user starts a new chat or when a new AI response has no actions
- The most recent entry (by ID) is used when executing actions

## Indexes

```sql
CREATE INDEX IF NOT EXISTS idx_messages_user ON messages(user_login);
CREATE INDEX IF NOT EXISTS idx_audit_login ON audit(login);
```

## Database Access Pattern

All database access uses direct JDBC with `PreparedStatement`:
1. Get connection from `DatabaseInitializer.getConnection()`
2. Prepare statement with parameterized queries (SQL injection safe)
3. Execute and process results
4. Connection auto-closed via try-with-resources

No ORM (JPA/Hibernate) is used. No connection pooling — each operation opens a new SQLite connection.

## Database File Location

- **Default path:** `data/app.db`
- **Docker mount:** `./data:/app/data` maps host `data/` to container
- **Auto-created:** The `data/` directory and database file are created automatically on first startup
