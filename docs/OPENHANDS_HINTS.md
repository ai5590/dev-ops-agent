# AI Code Generator Hints — dev-ops-agent

This document helps AI code generators (OpenHands, Cursor, Copilot, etc.) understand the project structure and conventions for making effective modifications.

## Project Overview

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.0
- **Build:** Gradle (`build.gradle`)
- **Database:** SQLite via direct JDBC (no ORM, no JPA)
- **Frontend:** Static HTML/CSS/JS (no build tools, no npm)
- **Config:** `data/config.json` (read by `ConfigLoader`)
- **Port:** 25003

## Key Files by Task

### "I want to add a new API endpoint"
1. Create/edit controller in `src/main/java/org/ai5590/devopsagent/api/`
2. Optionally add service in `service/` package
3. All endpoints under `/api/**` require authentication (Spring Security)
4. Return `ResponseEntity<Map<String, Object>>` — the project uses Maps, not DTOs
5. Get current user via `Authentication auth` parameter → `auth.getName()`

### "I want to add a new SSH action type"
1. Edit `src/main/java/org/ai5590/devopsagent/actions/ActionExecutor.java` — add `else if` branch
2. Edit `data/system_prompt_part2_apis.md` — document the new API for the AI
3. Optionally add a service class for the new integration

### "I want to change AI behavior"
1. Edit `data/system_prompt_part1_default.txt` for default behavior
2. Edit `data/system_prompt_part2_apis.md` for action format / API reference
3. Prompt assembly in `src/main/java/org/ai5590/devopsagent/service/ChatService.java`

### "I want to add a new database table"
1. Add `CREATE TABLE IF NOT EXISTS` to `src/main/java/org/ai5590/devopsagent/db/DatabaseInitializer.java`
2. Create repository class in `db/` package extending the same pattern
3. Inject `DatabaseInitializer` for `getConnection()`

### "I want to modify the frontend"
1. HTML/CSS/JS files: `src/main/resources/static/`
2. Page routes: `src/main/java/org/ai5590/devopsagent/api/PageController.java`
3. No frontend build step — edit files directly
4. CSRF: all POST requests must include `X-XSRF-TOKEN` header (read from `XSRF-TOKEN` cookie)

### "I want to change authentication"
1. Security config: `src/main/java/org/ai5590/devopsagent/security/SecurityConfig.java`
2. User loading: `security/CustomUserDetailsService.java`
3. Bootstrap: `security/BootstrapService.java`
4. User DB: `db/UserRepository.java`

### "I want to change the config"
1. Config POJO: `src/main/java/org/ai5590/devopsagent/config/AppConfig.java` — add field + getter/setter
2. Config loader: `config/ConfigLoader.java`
3. Config file: `data/config.json`
4. Template: `data/config.template.jsonc`

## Coding Conventions

### Java Style
- **No Lombok** — all getters/setters are explicit
- **No DTOs** — API responses use `Map<String, Object>` and `LinkedHashMap` for ordered keys
- **Constructor injection** — no `@Autowired` on fields
- **Slf4j logging** — `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- **Try-with-resources** — always for `Connection`, `PreparedStatement`
- **No ORM** — raw JDBC with `PreparedStatement` parameter binding
- **@JsonIgnoreProperties(ignoreUnknown = true)** on config POJOs

### Controller Pattern
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private final SomeService someService;

    public ResourceController(SomeService someService) {
        this.someService = someService;
    }

    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> action(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String userLogin = auth.getName();
        Map<String, Object> result = someService.doAction(userLogin, body.get("field"));
        return ResponseEntity.ok(result);
    }
}
```

### Repository Pattern
```java
@Repository
public class SomeRepository {
    private static final Logger log = LoggerFactory.getLogger(SomeRepository.class);
    private final DatabaseInitializer db;

    public SomeRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public SomeResult query(String param) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ... WHERE col = ?")) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            // process
        } catch (SQLException e) {
            log.error("Error: {}", e.getMessage());
        }
        return result;
    }
}
```

### Service Pattern
```java
@Service
public class SomeService {
    private static final Logger log = LoggerFactory.getLogger(SomeService.class);
    private final SomeRepository someRepository;

    public SomeService(SomeRepository someRepository) {
        this.someRepository = someRepository;
    }

    public Map<String, Object> doAction(String userLogin, String input) {
        Map<String, Object> result = new LinkedHashMap<>();
        // business logic
        result.put("success", true);
        return result;
    }
}
```

## Component Scan

The main application uses:
```java
@SpringBootApplication(scanBasePackages = "org.ai5590.devopsagent", exclude = {DataSourceAutoConfiguration.class})
```
All new classes under `org.ai5590.devopsagent` are auto-detected. `DataSourceAutoConfiguration` is excluded because SQLite connections are managed manually.

## Important Notes

1. **No test directory exists** — the project has no automated tests yet
2. **Config is file-based** — `data/config.json`, not Spring `application.properties` for app config
3. **SQLite limitations** — no concurrent write support; avoid parallel writes
4. **Action parsing relies on exact markers** — `---ACTIONS_JSON_START---` and `---ACTIONS_JSON_END---` must be exact
5. **Prompt files are read on every chat request** — changes to prompt files take effect immediately without restart
6. **Config is read once at startup** — changes to `data/config.json` require restart
7. **No roles** — all authenticated users have identical permissions
8. **Message limit** — 30 messages per user as context window; older messages remain in DB but aren't sent to AI
9. **Frontend includes CSRF** — all fetch/XHR calls must include the `X-XSRF-TOKEN` header

## File Dependencies Graph (simplified)

```
DevOpsAgentApplication
    └── (component scan picks up everything)

ChatController → ChatService → OpenAiService → ConfigLoader → AppConfig
                             → ActionParser
                             → MessageRepository → DatabaseInitializer
                             → UserRepository → DatabaseInitializer
                             → PendingActionsRepository → DatabaseInitializer
              → ActionExecutor → SshAgentService → ConfigLoader
                               → AuditService → AuditRepository → DatabaseInitializer

SecurityConfig → CustomUserDetailsService → UserRepository
BootstrapService → ConfigLoader, UserRepository, PasswordEncoder
```
