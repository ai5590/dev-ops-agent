# Project Specification — dev-ops-agent

## Overview

**dev-ops-agent** is a web-based DevOps chat assistant that combines OpenAI language models with SSH command execution on remote servers. Users interact through a mobile-friendly chat interface, and the AI suggests actionable commands that users can approve and execute.

## Goals

1. Provide a conversational interface for server management tasks
2. Let AI suggest SSH commands with risk assessment before execution
3. Maintain per-user chat history and audit trails for all executed commands
4. Support multi-user authentication with bcrypt password hashing
5. Be deployable via Docker or standalone JAR

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Security | Spring Security (form login, CSRF, bcrypt) |
| Database | SQLite 3.44 (via JDBC, no ORM) |
| AI | OpenAI Chat Completions API |
| SSH Execution | External `ssh-executor-agent` service (HTTP API) |
| Build | Gradle 8 |
| Frontend | Static HTML/CSS/JS served by Spring Boot |
| Containerization | Docker multi-stage build |

## Features

### 1. Multi-User Authentication
- Form-based login via Spring Security
- Passwords stored as bcrypt hashes in SQLite
- Bootstrap users created on startup from `data/config.json`
- Two modes: `UPSERT` (create or update) and `CREATE_ONLY` (skip existing)

### 2. Chat with AI Assistant
- Messages sent to OpenAI Chat Completions API with system prompt + conversation history
- Last 30 messages kept as context window
- System prompt composed of two parts:
  - Part 1: General behavior instructions (customizable per user)
  - Part 2: Available API reference and action JSON format
- AI responds with text and optionally action blocks

### 3. Action Execution
- AI embeds action JSON between `---ACTIONS_JSON_START---` and `---ACTIONS_JSON_END---` markers
- Actions parsed by `ActionParser`, stored in `pending_actions` table
- User approves actions in the UI; `ActionExecutor` dispatches to `SshAgentService`
- Two action APIs: `ssh.list_servers` and `ssh.execute`
- Each action has: id, api, title, description, risk level, params

### 4. SSH Integration
- `SshAgentService` communicates with external `ssh-executor-agent` via HTTP
- `GET /servers` — list available servers
- `POST /exec` — execute command on a server
- Configurable base URL for local, Docker, or remote deployment

### 5. Audit Logging
- Every SSH action logged to:
  - SQLite `audit` table (structured: user, action, server, command, duration, result snippet)
  - `data/logs/audit.log` file (via dedicated Logback logger)
- Result snippets truncated to 200 chars (log) / 500 chars (database)

### 6. Per-User System Prompts
- Default prompt loaded from `data/system_prompt_part1_default.txt`
- Users can override Part 1 via the prompt editing UI
- Override stored in `users.prompt_part1_override` column
- Part 2 (API reference) always appended, not user-editable

### 7. User Settings
- `show_debug` toggle per user (stored in `user_settings` table)
- Accessible via `GET /api/user/id` and `POST /api/user/settings/debug`

### 8. Chat History Management
- Messages stored per-user in `messages` table
- "New Chat" clears all messages and pending actions for the user
- Message limit warning at 30 messages (oldest displaced from context)
- Polling support via `GET /api/chat/state?since=<id>` for incremental updates

## Data Models

### Configuration (`data/config.json`)
```json
{
  "openaiApiKey": "string",
  "openaiBaseUrl": "string",
  "openaiModel": "string",
  "sshAgentBaseUrl": "string",
  "bootstrapUsersMode": "UPSERT | CREATE_ONLY",
  "bootstrapUsers": [{"login": "string", "password": "string"}]
}
```

### Action JSON (embedded in AI response)
```json
{
  "actions": [
    {
      "id": "1",
      "api": "ssh.execute",
      "title": "Check disk usage",
      "description": "Runs df -h on the server",
      "risk": "low",
      "params": {
        "server": "prod-web-01",
        "command": "df -h"
      }
    }
  ]
}
```

### Chat Message (API response)
```json
{
  "text": "AI response text",
  "hasActions": true,
  "actionsJson": "{...}",
  "limitReached": false
}
```

## REST API Endpoints

### Pages
- `GET /login` — Login page
- `GET /chat` — Chat interface (auth required)
- `GET /help` — Help page (auth required)

### Auth
- `POST /login` — Form login (username, password)
- `POST /logout` — Logout

### Chat (`/api/chat`)
- `POST /send` — Send user message, receive AI response
- `POST /new` — Clear chat history
- `GET /state?since=<id>` — Get messages
- `POST /action/{id}` — Execute pending action

### Prompts (`/api/prompt`)
- `POST /start-update` — Begin prompt editing
- `POST /submit` — Save new prompt

### User (`/api/user`)
- `GET /id` — Get user info and settings
- `POST /settings/debug` — Toggle debug mode

## Deployment

- **Standalone**: `java -jar build/libs/dev-ops-agent-1.0.0.jar`
- **Docker**: `docker-compose up -d`
- **Port**: 25003
- **HTTPS**: Not included; use reverse proxy (Caddy, nginx)
- **Data**: All persistent data in `data/` directory (config, DB, logs, prompts)
