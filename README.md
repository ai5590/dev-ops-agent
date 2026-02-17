# dev-ops-agent

**Mobile-friendly web chat interface for DevOps** — an OpenAI-powered AI assistant with SSH command execution on remote servers.

> **RU:** Мобильный веб-чат для DevOps — ИИ-ассистент на базе OpenAI с выполнением SSH-команд на удалённых серверах.

---

## Features

- **Multi-user authentication** — Spring Security form-based login with bcrypt password hashing
- **Chat with AI assistant** — OpenAI GPT integration with configurable models
- **SSH command execution** — AI suggests actions; user approves; commands run on remote servers via `ssh-executor-agent`
- **Per-user system prompts** — Each user can customize their AI assistant behavior
- **Action risk assessment** — Actions are tagged with risk levels (low / medium / high)
- **Chat history** — Persistent per-user message history (SQLite), last 30 messages as context window
- **Audit logging** — Every SSH command is logged to database and file with user, server, command, duration, result
- **Mobile-friendly UI** — Responsive HTML/CSS/JS chat interface
- **CSRF protection** — Cookie-based CSRF tokens via Spring Security
- **Debug mode** — Per-user toggle to show/hide debug information

> **RU:** Многопользовательская авторизация, чат с ИИ, выполнение SSH-команд, аудит-лог, мобильный интерфейс.

---

## Quick Start

### Prerequisites

- **Java 17** (JDK)
- **Gradle 8+** (or use the included Gradle wrapper `./gradlew`)
- **OpenAI API key**
- (Optional) **ssh-executor-agent** running on port 25005 for SSH command execution

### 1. Configure

Edit `data/config.json` (see [Configuration](#configuration) below):

```bash
cp data/config.template.jsonc data/config.json
# Edit data/config.json — set your OpenAI API key and bootstrap users
```

### 2. Run Locally

```bash
# Option A: Gradle wrapper
./gradlew bootRun

# Option B: Build JAR and run
./gradlew bootJar
java -jar build/libs/dev-ops-agent-1.0.0.jar
```

The application starts on **http://localhost:25003**

### 3. Run with Docker

```bash
docker-compose up -d
```

> **RU:** Отредактируйте `data/config.json`, затем запустите `./gradlew bootRun` или `docker-compose up -d`.

---

## Configuration

All configuration is stored in `data/config.json`. See `data/config.template.jsonc` for a commented template.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `openaiApiKey` | string | — | Your OpenAI API key (required) |
| `openaiBaseUrl` | string | `https://api.openai.com/v1` | OpenAI API base URL. Override for proxies or compatible APIs |
| `openaiModel` | string | `gpt-4o-mini` | Model to use: `gpt-4o-mini`, `gpt-4o`, `gpt-4-turbo`, `gpt-3.5-turbo`, `o1-mini`, `o1-preview` |
| `sshAgentBaseUrl` | string | `http://127.0.0.1:25005` | URL of the ssh-executor-agent. Use `http://host.docker.internal:25005` from Docker |
| `bootstrapUsersMode` | string | `UPSERT` | How to handle bootstrap users on startup: `UPSERT` or `CREATE_ONLY` |
| `bootstrapUsers` | array | `[]` | List of `{login, password}` objects to create on startup |

Example:

```json
{
  "openaiApiKey": "sk-...",
  "openaiBaseUrl": "https://api.openai.com/v1",
  "openaiModel": "gpt-4o-mini",
  "sshAgentBaseUrl": "http://127.0.0.1:25005",
  "bootstrapUsersMode": "UPSERT",
  "bootstrapUsers": [
    {"login": "admin", "password": "MySecurePassword123"}
  ]
}
```

> **RU:** Все настройки в `data/config.json`. Обязательно укажите `openaiApiKey`.

### Bootstrap Users: UPSERT vs CREATE_ONLY

- **UPSERT** (recommended): Creates users if they don't exist. If a user already exists, updates their password hash. Useful for resetting passwords or ensuring a known admin account always exists.
- **CREATE_ONLY**: Creates users only if they don't exist. Skips existing users without modifying them. Useful when you don't want startup to overwrite manually changed passwords.

> **RU:** `UPSERT` — создаёт или обновляет пользователей. `CREATE_ONLY` — только создаёт новых, пропускает существующих.

---

## Connecting ssh-executor-agent

The `ssh-executor-agent` is a separate service that manages SSH connections to remote servers. It must be running and accessible at the URL configured in `sshAgentBaseUrl`.

- **Local setup**: Run ssh-executor-agent on port 25005, set `sshAgentBaseUrl` to `http://127.0.0.1:25005`
- **Docker setup**: Use `host.docker.internal` to reach the host: `http://host.docker.internal:25005`
- **Remote setup**: Point to the remote server: `http://your-server:25005`

The agent exposes two endpoints consumed by dev-ops-agent:
- `GET /servers` — list configured SSH servers
- `POST /exec` — execute a command on a server (`{server, command}`)

> **RU:** `ssh-executor-agent` — отдельный сервис для SSH-подключений. Укажите его URL в `sshAgentBaseUrl`.

---

## Security

- **Passwords** are hashed with **bcrypt** (via Spring Security `BCryptPasswordEncoder`)
- **CSRF protection** uses `CookieCsrfTokenRepository` — CSRF token is set in a cookie readable by JavaScript
- **Session management** — standard Spring Security HTTP sessions with `SameSite=Lax` cookies
- **Audit logging** — every SSH action is logged to both `data/logs/audit.log` and the `audit` SQLite table with: user, action, server, command, duration, result snippet
- **No secrets in git** — `data/config.json` contains the API key and should be in `.gitignore`
- **HTTPS** is **not** handled by this application. Use a reverse proxy (Caddy, nginx, Traefik) to terminate TLS in front of port 25003

> **RU:** Пароли хешируются bcrypt, CSRF через cookie, аудит всех SSH-команд. HTTPS — через внешний прокси (Caddy/nginx).

---

## Project Structure

```
dev-ops-agent/
├── build.gradle                          # Gradle build config (Spring Boot 3.2, Java 17)
├── settings.gradle                       # Project name
├── Dockerfile                            # Multi-stage Docker build
├── docker-compose.yml                    # Docker Compose setup
├── data/
│   ├── config.json                       # Runtime configuration (API keys, users)
│   ├── config.template.jsonc             # Commented config template
│   ├── system_prompt_part1_default.txt   # Default system prompt (part 1)
│   ├── system_prompt_part2_apis.md       # API reference for AI (part 2)
│   ├── app.db                            # SQLite database (auto-created)
│   └── logs/
│       ├── app.log                       # Application log
│       └── audit.log                     # Audit log (SSH actions)
├── src/main/java/org/ai5590/devopsagent/
│   ├── app/
│   │   └── DevOpsAgentApplication.java   # Spring Boot entry point
│   ├── api/
│   │   ├── ChatController.java           # Chat REST API
│   │   ├── PageController.java           # Page routing (login, chat, help)
│   │   ├── PromptController.java         # Prompt management API
│   │   └── UserController.java           # User settings API
│   ├── service/
│   │   ├── ChatService.java              # Chat business logic
│   │   └── PromptService.java            # Prompt update logic
│   ├── actions/
│   │   ├── ActionParser.java             # Parse AI response for action JSON
│   │   └── ActionExecutor.java           # Execute approved actions via SSH
│   ├── openai/
│   │   └── OpenAiService.java            # OpenAI API client
│   ├── sshagent/
│   │   └── SshAgentService.java          # SSH executor agent HTTP client
│   ├── security/
│   │   ├── SecurityConfig.java           # Spring Security config
│   │   ├── CustomUserDetailsService.java # User authentication
│   │   └── BootstrapService.java         # User bootstrapping on startup
│   ├── config/
│   │   ├── AppConfig.java                # Configuration POJO
│   │   └── ConfigLoader.java             # JSON config file loader
│   ├── audit/
│   │   └── AuditService.java             # Audit logging service
│   └── db/
│       ├── DatabaseInitializer.java      # SQLite schema setup
│       ├── UserRepository.java           # User CRUD
│       ├── MessageRepository.java        # Chat message CRUD
│       ├── UserSettingsRepository.java   # User settings CRUD
│       ├── AuditRepository.java          # Audit log CRUD
│       └── PendingActionsRepository.java # Pending action storage
├── src/main/resources/
│   ├── application.properties            # Spring Boot properties
│   ├── logback-spring.xml                # Logging config
│   └── static/
│       ├── login.html                    # Login page
│       ├── chat.html                     # Chat UI
│       ├── chat.css                      # Chat styles
│       ├── chat.js                       # Chat JavaScript
│       └── help.html                     # Help page
└── docs/
    ├── PROJECT_SPEC.md                   # Full project specification
    ├── ARCHITECTURE.md                   # Architecture overview
    ├── DB_SCHEMA.md                      # Database schema docs
    ├── PROMPTS_AND_ACTIONS.md            # Prompt system & action format
    ├── SECURITY.md                       # Security documentation
    ├── DEV_GUIDE.md                      # Developer guide
    └── OPENHANDS_HINTS.md                # Hints for AI code generators
```

---

## API Endpoints

### Pages (GET, returns HTML)

| Endpoint | Description |
|----------|-------------|
| `GET /login` | Login page |
| `GET /chat` | Chat interface (requires auth) |
| `GET /help` | Help page (requires auth) |

### Authentication

| Endpoint | Method | Description |
|----------|--------|-------------|
| `POST /login` | POST | Form login (`username` + `password` fields) |
| `POST /logout` | POST | Logout, redirects to `/login?logout=true` |

### Chat API (requires auth)

| Endpoint | Method | Body | Description |
|----------|--------|------|-------------|
| `POST /api/chat/send` | POST | `{"text": "..."}` | Send message, get AI response |
| `POST /api/chat/new` | POST | — | Clear chat history |
| `GET /api/chat/state` | GET | `?since=<id>` | Get messages (all or since ID) |
| `POST /api/chat/action/{id}` | POST | — | Execute a pending action by ID |

### Prompt API (requires auth)

| Endpoint | Method | Body | Description |
|----------|--------|------|-------------|
| `POST /api/prompt/start-update` | POST | — | Start prompt editing (returns current prompt) |
| `POST /api/prompt/submit` | POST | `{"text": "..."}` | Save new system prompt |

### User API (requires auth)

| Endpoint | Method | Body | Description |
|----------|--------|------|-------------|
| `GET /api/user/id` | GET | — | Get current user info + settings |
| `POST /api/user/settings/debug` | POST | `{"showDebug": true}` | Toggle debug mode |

---

## HTTPS Note

This application does **not** handle HTTPS/TLS directly. In production, place a reverse proxy in front of it:

```
# Example with Caddy (automatic HTTPS)
your-domain.com {
    reverse_proxy localhost:25003
}
```

Other options: nginx, Traefik, HAProxy.

> **RU:** Приложение не обрабатывает HTTPS напрямую. Используйте Caddy, nginx или Traefik как reverse proxy.

---

## Push to GitHub

To push to GitHub, create a repository at [github.com/ai5590/dev-ops-agent](https://github.com/ai5590/dev-ops-agent), then:

```bash
git remote add origin https://github.com/ai5590/dev-ops-agent.git
git push -u origin main
```

---

## License

Private project by ai5590.
