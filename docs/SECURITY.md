# Security — dev-ops-agent

## Overview

Security is implemented using **Spring Security 6** with form-based authentication, CSRF protection, bcrypt password hashing, and comprehensive audit logging.

## Authentication

### Form-Based Login
- Login page: `GET /login` (served from `static/login.html`)
- Login processing: `POST /login` (Spring Security form login)
- Successful login redirects to `/chat`
- Failed login redirects to `/login?error=true`
- Logout: `POST /logout` → redirects to `/login?logout=true`

### User Storage
- Users stored in SQLite `users` table
- Passwords hashed with **BCrypt** (`BCryptPasswordEncoder`)
- No roles or authorities — all authenticated users have equal access
- `CustomUserDetailsService` loads credentials from `UserRepository`

### Bootstrap Users
- Initial users created from `data/config.json` on application startup
- `BootstrapService` runs on `ApplicationReadyEvent`
- Passwords in config are plaintext — hashed with bcrypt before storage
- **UPSERT mode**: Creates or updates users (password re-hashed each startup)
- **CREATE_ONLY mode**: Only creates new users, existing users untouched

## Authorization

### URL Protection Rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
    .anyRequest().authenticated()
)
```

| Path Pattern | Access |
|-------------|--------|
| `/login`, `/login.html` | Public |
| `/css/**`, `/js/**` | Public |
| `/favicon.ico` | Public |
| Everything else | Authenticated only |

## CSRF Protection

### Configuration
```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(requestHandler)
)
```

- Uses `CookieCsrfTokenRepository` — CSRF token stored in a cookie named `XSRF-TOKEN`
- `HttpOnly` set to `false` — allows JavaScript to read the token
- `CsrfTokenRequestAttributeName` set to `null` — makes token available immediately (no deferred loading)
- Frontend JavaScript must read the `XSRF-TOKEN` cookie and include it as `X-XSRF-TOKEN` header in POST requests

### How It Works
1. Spring Security sets `XSRF-TOKEN` cookie on first response
2. Frontend JavaScript reads the cookie value
3. Each POST/PUT/DELETE request includes `X-XSRF-TOKEN` header with the token
4. Spring Security validates the header against the cookie

## Session Management

- Standard HTTP session with `JSESSIONID` cookie
- `SameSite=Lax` configured in `application.properties`:
  ```properties
  server.servlet.session.cookie.same-site=lax
  ```
- Session created on successful login
- Session invalidated on logout

## Password Security

- **Algorithm**: bcrypt (via `BCryptPasswordEncoder`)
- **Salt**: Automatically generated per-hash by bcrypt
- **Work factor**: Default (10 rounds)
- **Storage**: `users.password_hash` column in SQLite
- **Bootstrap**: Plaintext passwords in `config.json` are hashed before storage; the plaintext is never stored in the database

## Audit Logging

### What Is Logged
Every SSH action execution is recorded with:
- **User**: Who initiated the action
- **Action**: API called (`ssh.list_servers` or `ssh.execute`)
- **Server**: Target server (if applicable)
- **Command**: Command executed (if applicable)
- **Duration**: Execution time in milliseconds
- **Result**: First 200–500 characters of the output

### Where Logs Are Written
1. **SQLite `audit` table** — structured records for programmatic access
2. **`data/logs/audit.log`** — dedicated Logback file appender
   - Rolling policy: 200MB per file, 90 days retention, 18GB total cap
3. **Console/app.log** — audit events are NOT written to the general log (separate logger with `additivity="false"`)

### Audit Log Format (file)
```
HH:mm:ss.SSS [thread] INFO  AUDIT - login=admin action=ssh.execute server=prod-web-01 command=df -h duration_ms=1234 result=...
```

## Secrets Management

### Sensitive Data
- **OpenAI API key** — stored in `data/config.json`
- **User passwords** — stored as bcrypt hashes in SQLite
- **Bootstrap passwords** — plaintext in `data/config.json` (hashed on startup)

### Best Practices
- `data/config.json` should be in `.gitignore` — never commit API keys
- `data/app.db` should be in `.gitignore` — contains password hashes
- Use `data/config.template.jsonc` as a reference (no real keys)
- In Docker, mount `data/` as a volume so secrets stay on the host

## HTTPS / TLS

This application does **not** handle HTTPS. It listens on plain HTTP (port 25003).

For production deployment, use a reverse proxy:

```
Internet → Caddy/nginx/Traefik (HTTPS:443) → dev-ops-agent (HTTP:25003)
```

Recommended:
- **Caddy**: Automatic HTTPS with Let's Encrypt
- **nginx**: Manual certificate configuration
- **Traefik**: Docker-native with automatic HTTPS

## Security Checklist

- [x] Passwords hashed with bcrypt
- [x] CSRF protection enabled (cookie-based)
- [x] Session cookies with SameSite=Lax
- [x] All endpoints require authentication (except login page)
- [x] Audit logging for all SSH actions
- [x] SQL injection prevented (PreparedStatement throughout)
- [x] No secrets in source code
- [ ] HTTPS (must be configured externally)
- [ ] Rate limiting (not implemented)
- [ ] Account lockout (not implemented)
- [ ] Role-based access control (not implemented — all users are equal)
