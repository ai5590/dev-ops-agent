# Developer Guide — dev-ops-agent

## Prerequisites

- **Java 17** (JDK, not just JRE)
- **Gradle 8+** (or use `./gradlew` wrapper)
- **OpenAI API key** (for AI features)
- **ssh-executor-agent** running on port 25005 (for SSH features)

## Building

```bash
# Build the fat JAR
./gradlew bootJar

# Output: build/libs/dev-ops-agent-1.0.0.jar

# Clean and rebuild
./gradlew clean bootJar
```

## Running

### Development
```bash
# Run directly with Gradle (auto-recompiles)
./gradlew bootRun

# Or run the JAR
java -jar build/libs/dev-ops-agent-1.0.0.jar
```

Application starts on **http://localhost:25003**

### Docker
```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f

# Rebuild after code changes
docker-compose up -d --build
```

### Configuration
Before running, ensure `data/config.json` exists with your OpenAI API key:
```bash
cp data/config.template.jsonc data/config.json
# Edit data/config.json
```

## Project Organization

```
src/main/java/org/ai5590/devopsagent/
├── app/        → Spring Boot entry point
├── api/        → REST controllers
├── service/    → Business logic
├── actions/    → AI action parsing and execution
├── openai/     → OpenAI API client
├── sshagent/   → SSH agent HTTP client
├── security/   → Authentication, authorization
├── config/     → Configuration loading
├── audit/      → Audit logging
└── db/         → Database repositories

src/main/resources/
├── application.properties    → Spring Boot config
├── logback-spring.xml        → Logging config
└── static/                   → Frontend HTML/CSS/JS

data/
├── config.json               → Runtime config
├── system_prompt_part1_default.txt → Default AI prompt
├── system_prompt_part2_apis.md     → API reference for AI
├── app.db                    → SQLite database
└── logs/                     → Log files
```

## Common Development Tasks

### Adding a New REST Endpoint

1. Create or modify a controller in `src/main/java/.../api/`
2. Follow existing pattern:
   ```java
   @RestController
   @RequestMapping("/api/newfeature")
   public class NewController {
       @PostMapping("/action")
       public ResponseEntity<Map<String, Object>> doAction(
               @RequestBody Map<String, String> body,
               Authentication auth) {
           String userLogin = auth.getName();
           // ... logic ...
           return ResponseEntity.ok(Map.of("success", true));
       }
   }
   ```
3. All endpoints under `/api/**` require authentication automatically

### Adding a New Action Type

Currently supported: `ssh.list_servers`, `ssh.execute`. To add a new action:

1. **Update `ActionExecutor.java`** — add a new `else if` branch:
   ```java
   } else if ("my_new_api".equals(api)) {
       // Handle the new action
       output = myService.doSomething(action.path("params"));
   }
   ```

2. **Update `data/system_prompt_part2_apis.md`** — document the new API so the AI knows about it:
   ```markdown
   ### my_new_api
   Description of what this API does.
   **Parameters:** ...
   **Example:** ...
   ```

3. **Add a service** if the action needs an external integration (like `SshAgentService`)

4. **Restart** the application to reload the prompt files

### Modifying the AI System Prompt

**Default prompt (all users):**
Edit `data/system_prompt_part1_default.txt`. Changes take effect on the next chat message (file is read on each request).

**API reference:**
Edit `data/system_prompt_part2_apis.md`. This defines what actions the AI can suggest.

**Per-user override:**
Users can customize their Part 1 prompt via the UI or API:
```bash
# Start prompt update
curl -X POST http://localhost:25003/api/prompt/start-update \
  -H "Cookie: JSESSIONID=..."

# Submit new prompt
curl -X POST http://localhost:25003/api/prompt/submit \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=..." \
  -d '{"text": "You are a helpful DevOps assistant..."}'
```

### Adding Users

**Via config (recommended for bootstrap):**
Add to `data/config.json`:
```json
{
  "bootstrapUsers": [
    {"login": "admin", "password": "AdminPass123"},
    {"login": "operator", "password": "OpPass456"}
  ]
}
```
Restart the application. Users are created/updated based on `bootstrapUsersMode`.

**Via database (manual):**
```bash
# Generate bcrypt hash (using any bcrypt tool)
# Then insert directly:
sqlite3 data/app.db "INSERT INTO users (login, password_hash) VALUES ('newuser', '\$2a\$10\$...');"
```

### Database Access

The project uses direct JDBC — no ORM. To add a new query:

1. Add a method to the appropriate repository in `src/main/java/.../db/`
2. Follow the existing pattern:
   ```java
   public ResultType myQuery(String param) {
       try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT ... WHERE col = ?")) {
           ps.setString(1, param);
           ResultSet rs = ps.executeQuery();
           // process results
       } catch (SQLException e) {
           log.error("Error: {}", e.getMessage());
       }
       return result;
   }
   ```
3. Always use `PreparedStatement` with parameter binding (never string concatenation)
4. Always use try-with-resources for `Connection` and `PreparedStatement`

### Adding a New Database Table

1. Add the `CREATE TABLE IF NOT EXISTS` statement to `DatabaseInitializer.init()`
2. Create a new repository class in `db/` package
3. Inject `DatabaseInitializer` for connection access

### Modifying the Frontend

Frontend files are in `src/main/resources/static/`:
- `login.html` — Login page
- `chat.html` — Main chat interface
- `chat.css` — Chat styles
- `chat.js` — Chat client-side logic
- `help.html` — Help page

Static files are served directly by Spring Boot. For development, edit the files and reload the browser (no build step needed for static files, but you may need to restart if Spring caches them).

### Adding a New Page

1. Create the HTML file in `src/main/resources/static/`
2. Add a route in `PageController.java`:
   ```java
   @GetMapping("/newpage")
   public String newpage() {
       return "forward:/newpage.html";
   }
   ```
3. If the page should be public, add the path to `SecurityConfig`:
   ```java
   .requestMatchers("/newpage", ...).permitAll()
   ```

## Logging

### Application Log
- File: `data/logs/app.log`
- Rotation: 100MB per file, 30 days, 3GB total
- Contains all application logs except audit

### Audit Log
- File: `data/logs/audit.log`
- Rotation: 200MB per file, 90 days, 18GB total
- Contains only SSH action execution records
- Separate Logback logger named `AUDIT`

### Console
- Same format as `app.log`
- Useful for development

### Log Levels
Set in `logback-spring.xml`. Root level is `INFO`. To enable debug logging:
```xml
<root level="DEBUG">
```

## Testing

Currently, no automated tests exist. To test manually:

1. Start the application
2. Login at `http://localhost:25003/login`
3. Send chat messages
4. Verify AI responses contain action buttons when appropriate
5. Check `data/logs/audit.log` after executing actions
6. Inspect `data/app.db` with `sqlite3` for data verification

### API Testing with curl

```bash
# Login (get session cookie)
curl -c cookies.txt -X POST http://localhost:25003/login \
  -d "username=admin&password=CHANGE_ME"

# Get CSRF token (from cookie)
CSRF=$(grep XSRF cookies.txt | awk '{print $NF}')

# Send chat message
curl -b cookies.txt -X POST http://localhost:25003/api/chat/send \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{"text": "Show me server status"}'

# Get chat state
curl -b cookies.txt http://localhost:25003/api/chat/state

# New chat
curl -b cookies.txt -X POST http://localhost:25003/api/chat/new \
  -H "X-XSRF-TOKEN: $CSRF"
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Config file not found` | Ensure `data/config.json` exists |
| `OpenAI API error 401` | Check `openaiApiKey` in config |
| `SSH agent connection refused` | Ensure ssh-executor-agent is running on the configured port |
| `Login fails` | Check bootstrap users in config, verify `bootstrapUsersMode` |
| `CSRF token invalid` | Ensure frontend sends `X-XSRF-TOKEN` header from cookie |
| `Database locked` | SQLite doesn't support high concurrency; restart if stuck |
