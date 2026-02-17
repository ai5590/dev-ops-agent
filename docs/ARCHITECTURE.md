# Architecture — dev-ops-agent

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      User (Browser)                         │
│                  Mobile-friendly Chat UI                     │
│              (HTML/CSS/JS served as static files)            │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP (port 25003)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                     │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐  │
│  │ Security │  │   API    │  │ Service  │  │   Config   │  │
│  │  Layer   │  │  Layer   │  │  Layer   │  │   Layer    │  │
│  └──────────┘  └──────────┘  └──────────┘  └────────────┘  │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │   Actions    │  │    OpenAI    │  │   SSH Agent      │  │
│  │   Layer      │  │    Client    │  │   Client         │  │
│  └──────────────┘  └──────────────┘  └───────────────────┘  │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Database Layer (SQLite)                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└────────────────────────┬──────────────────┬─────────────────┘
                         │                  │
                         ▼                  ▼
              ┌──────────────┐   ┌──────────────────────┐
              │  OpenAI API  │   │  ssh-executor-agent   │
              │  (external)  │   │  (port 25005)         │
              └──────────────┘   └──────────┬───────────┘
                                            │ SSH
                                            ▼
                                 ┌──────────────────────┐
                                 │   Remote Servers      │
                                 └──────────────────────┘
```

## Package Structure

```
org.ai5590.devopsagent
├── app/                    # Application entry point
│   └── DevOpsAgentApplication.java
├── api/                    # REST controllers (API layer)
│   ├── ChatController.java
│   ├── PageController.java
│   ├── PromptController.java
│   └── UserController.java
├── service/                # Business logic (Service layer)
│   ├── ChatService.java
│   └── PromptService.java
├── actions/                # Action parsing and execution
│   ├── ActionParser.java
│   └── ActionExecutor.java
├── openai/                 # OpenAI API client
│   └── OpenAiService.java
├── sshagent/               # SSH executor agent client
│   └── SshAgentService.java
├── security/               # Authentication & authorization
│   ├── SecurityConfig.java
│   ├── CustomUserDetailsService.java
│   └── BootstrapService.java
├── config/                 # Configuration loading
│   ├── AppConfig.java
│   └── ConfigLoader.java
├── audit/                  # Audit logging
│   └── AuditService.java
└── db/                     # Database access (Repository layer)
    ├── DatabaseInitializer.java
    ├── UserRepository.java
    ├── MessageRepository.java
    ├── UserSettingsRepository.java
    ├── AuditRepository.java
    └── PendingActionsRepository.java
```

## Layer Responsibilities

### API Layer (`api/`)
- REST controllers that handle HTTP requests
- Extract authenticated user from `Authentication` object
- Validate input, delegate to service layer
- Return JSON responses

### Service Layer (`service/`)
- Business logic orchestration
- `ChatService`: manages message flow — saves user message, builds system prompt, calls OpenAI, parses actions, saves AI response
- `PromptService`: manages per-user prompt override workflow

### Actions Layer (`actions/`)
- `ActionParser`: extracts action JSON from AI response using `---ACTIONS_JSON_START---` / `---ACTIONS_JSON_END---` markers
- `ActionExecutor`: dispatches approved actions to the appropriate API handler (SSH list servers or SSH execute)

### OpenAI Client (`openai/`)
- Constructs Chat Completions API request with system prompt + message history
- Sends via Java `HttpClient`, parses response
- Configurable model, base URL, API key

### SSH Agent Client (`sshagent/`)
- HTTP client for the external `ssh-executor-agent` service
- Two operations: `listServers()` and `execute(server, command)`
- Timeout: 30s for list, 60s for execute

### Security Layer (`security/`)
- `SecurityConfig`: Spring Security filter chain — CSRF, form login, authorization rules
- `CustomUserDetailsService`: loads user credentials from SQLite for Spring Security
- `BootstrapService`: creates/updates users from config on application startup

### Config Layer (`config/`)
- `AppConfig`: POJO representing `data/config.json`
- `ConfigLoader`: reads config file and prompt files from `data/` directory

### Audit Layer (`audit/`)
- `AuditService`: dual logging — writes to Logback AUDIT logger and SQLite audit table

### Database Layer (`db/`)
- `DatabaseInitializer`: creates tables and indexes on startup
- Repository classes: direct JDBC with `PreparedStatement` (no ORM)
- All repositories obtain connections from `DatabaseInitializer.getConnection()`

## Data Flow: User Message → AI Response → Action Execution

### Phase 1: Chat Message

```
User types message
    → POST /api/chat/send
    → ChatController.send()
    → ChatService.sendMessage()
        1. Check if user is in prompt-update mode
        2. Save user message to messages table
        3. Load last 30 messages as history
        4. Build system prompt (part1 override or default + part2 APIs)
        5. Call OpenAiService.chat(systemPrompt, history)
        6. Parse AI response with ActionParser
        7. Save AI response text to messages table
        8. Store any pending actions in pending_actions table
    → Return JSON: {text, hasActions, actionsJson, limitReached}
```

### Phase 2: Action Execution (if user approves)

```
User clicks action button
    → POST /api/chat/action/{id}
    → ChatController.executeAction()
        1. Load pending actions JSON for user
        2. Find action by ID
        3. ActionExecutor.executeAction()
            → SshAgentService.listServers() or SshAgentService.execute()
        4. AuditService.logAction() — log to DB + file
        5. Save action result as assistant message
    → Return JSON: {success, output, api, server, command, duration_ms}
```

## External Dependencies

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-web` | Embedded Tomcat, REST API |
| `spring-boot-starter-security` | Authentication, CSRF |
| `spring-security-crypto` | BCrypt password encoding |
| `sqlite-jdbc` | SQLite database driver |
| `jackson-databind` | JSON serialization |
| `spring-boot-starter-logging` | Logback logging |

## Configuration Files

| File | Purpose |
|------|---------|
| `data/config.json` | Runtime config: API keys, users, SSH agent URL |
| `data/system_prompt_part1_default.txt` | Default AI behavior instructions |
| `data/system_prompt_part2_apis.md` | API reference and action JSON format |
| `src/main/resources/application.properties` | Spring Boot properties (port, session) |
| `src/main/resources/logback-spring.xml` | Logging configuration |

## Key Design Decisions

1. **No ORM** — Direct JDBC with PreparedStatement for simplicity and SQLite compatibility
2. **File-based config** — JSON config file (`data/config.json`) instead of Spring properties for easy editing
3. **External SSH agent** — SSH execution delegated to a separate service for security isolation
4. **Action markers** — AI embeds structured JSON between text markers for reliable parsing
5. **Per-user prompts** — Users can customize AI behavior without affecting other users
6. **DataSource excluded** — `DataSourceAutoConfiguration` excluded since SQLite connections are managed manually
