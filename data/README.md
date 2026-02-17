# Data Directory

This directory contains configuration files, system prompts, database, and logs for the DevOps Agent application.

## Directory Contents

```
data/
├── config.json                      # Main configuration file (required)
├── config.template.jsonc            # Configuration template with comments
├── system_prompt_part1_default.txt  # Core system prompt for the AI model
├── system_prompt_part2_apis.md      # API documentation for the AI model
├── app.db                           # SQLite database (created automatically)
└── logs/                            # Application logs directory
    ├── app.log                      # Application logs
    └── audit.log                    # Audit trail of user actions
```

## Configuration

### config.json

This is the main configuration file required to run the application. Create it by copying and editing `config.template.jsonc`.

#### Configuration Fields

- **openaiApiKey** (string, required)
  - Your OpenAI API key for accessing GPT models
  - Format: `sk-...`
  - Never share this key or commit it to version control

- **openaiBaseUrl** (string, default: `https://api.openai.com/v1`)
  - Base URL for the OpenAI API
  - Can be overridden for custom endpoints or proxy servers
  - Leave default unless using a custom endpoint

- **openaiModel** (string, default: `gpt-4o-mini`)
  - The AI model to use for the DevOps Agent
  - Available options:
    - `gpt-4o-mini`: Fastest and cheapest (recommended for testing)
    - `gpt-4o`: Balanced performance and cost
    - `gpt-4-turbo`: High performance, higher cost
    - `gpt-3.5-turbo`: Budget option, older model
    - `o1-mini`: Reasoning model, slower but more capable
    - `o1-preview`: Advanced reasoning, slowest but most capable

- **sshAgentBaseUrl** (string, default: `http://127.0.0.1:25005`)
  - URL where the SSH executor agent is accessible
  - Docker container environment: `http://ssh-executor-agent:25005`
  - Host network environment: `http://127.0.0.1:25005`
  - Remote server: `http://your-server-address:25005`

- **bootstrapUsersMode** (string, default: `UPSERT`)
  - Controls how bootstrap users are handled on startup
  - `UPSERT`: Create users if they don't exist, update existing users (recommended)
  - `CREATE_ONLY`: Only create new users, skip if they already exist

- **bootstrapUsers** (array of objects, optional)
  - Initial users to create on application startup
  - Each user object must contain:
    - `login`: Username
    - `password`: Plain text password (will be hashed)
  - Passwords are never stored in plain text; they are hashed using bcrypt
  - Can be omitted if no bootstrap users are needed

### config.template.jsonc

This file provides a template with comments explaining all available configuration options and model variants. Use it as a reference when creating your `config.json` file.

## Bootstrap Users

Bootstrap users are automatically created or updated when the application starts.

### UPSERT Mode

In `UPSERT` mode (recommended):
- New users are created with the provided credentials
- Existing users with the same login have their password updated
- Use this mode for consistent configuration across deployments

### CREATE_ONLY Mode

In `CREATE_ONLY` mode:
- New users are created with the provided credentials
- Existing users are skipped and not modified
- Use this mode if you want to preserve manually created users

## System Prompts

The system prompts guide the AI model's behavior and capabilities.

### system_prompt_part1_default.txt

The core system prompt that defines the DevOps Agent's role and behavior:
- Identity and purpose
- General guidelines and constraints
- Language preferences
- Safety guidelines for dangerous operations

### system_prompt_part2_apis.md

Documentation of available APIs that the AI model can use:
- `ssh.list_servers`: List all available servers
- `ssh.execute`: Execute commands on servers
- JSON action format with risk levels
- Guidelines for safe command execution

### Per-User Override Prompts

Users can have custom system prompts:
- Create a file: `system_prompt_<username>.txt`
- This prompt will be used instead of the default for that user
- Allows customizing behavior per user while keeping a default for others

## Database

### app.db

SQLite database file created automatically on first run.

**Contains:**
- User accounts and authentication data
- Server connections and SSH keys
- Audit logs and action history
- Application state

**Location:** `data/app.db`

**Backup:** Regularly backup this file for data safety

**Permissions:** File should have restricted read/write permissions (not world-readable)

## Logs Directory

Application logs are stored in the `logs/` subdirectory.

### app.log

General application logs:
- Startup/shutdown events
- Configuration loading
- API requests and responses
- Errors and exceptions
- Performance metrics

### audit.log

Audit trail of user actions:
- Who executed what commands
- When actions were executed
- Command results and outputs
- Failed attempts
- Security-relevant events

**Retention:** Audit logs should be retained for security and compliance purposes

## Security

### Password Storage

- Passwords are **never** stored in plain text
- All passwords are hashed using bcrypt with configurable cost factor
- Password hashes are one-way; passwords cannot be recovered from hashes
- Password changes create new hashes; old hashes are replaced

### API Key Security

- OpenAI API keys should be protected like passwords
- Never commit `config.json` to version control
- Never share your API key in logs or error messages
- Use environment variables or secrets management for production

### Database Security

- The `app.db` file contains sensitive information
- Restrict file permissions to application user only
- Never make the database world-readable
- Regular backups should also be secured

### SSH Configuration

- SSH executor agent must be network-accessible from this application
- Use firewalls to restrict access to the SSH agent
- Store SSH keys securely in the database
- Never commit SSH private keys to version control

## Getting Started

1. Copy `config.template.jsonc` to `config.json`
2. Edit `config.json` and provide:
   - Your OpenAI API key
   - SSH agent base URL (localhost for testing, Docker service name for containers)
   - Bootstrap user credentials
3. Start the application
4. Login with the bootstrap user credentials
5. Add servers and SSH connections through the application interface
