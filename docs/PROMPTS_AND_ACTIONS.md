# Prompts and Actions — dev-ops-agent

## System Prompt Architecture

The system prompt sent to OpenAI is composed of two parts concatenated together:

```
[Part 1: Behavior Instructions] + "\n\n" + [Part 2: API Reference]
```

### Part 1: Behavior Instructions

**Source file:** `data/system_prompt_part1_default.txt`

Defines the AI assistant's personality, guidelines, and constraints:

```text
You are DevOps Agent — an AI assistant for server infrastructure management.
You help users monitor, diagnose, and manage Linux servers via SSH.
You can suggest commands to execute on remote servers.
Always explain what a command does before suggesting it.
For dangerous operations (rm -rf, reboot, shutdown, dd, mkfs, etc.), always warn the user about risks.
Prefer read-only diagnostic commands first.
Respond in the same language the user writes in.
```

**Per-user override:** Each user can replace Part 1 with their own version. The override is stored in `users.prompt_part1_override`. If `NULL` or blank, the default file is used.

**Override workflow:**
1. User calls `POST /api/prompt/start-update` — sets `pending_prompt_update = 1`, returns current prompt
2. User submits new prompt via `POST /api/prompt/submit` — saves to `prompt_part1_override`, clears pending flag
3. Alternative: if `pending_prompt_update` is set, the next chat message is treated as the new prompt text

### Part 2: API Reference

**Source file:** `data/system_prompt_part2_apis.md`

Defines the available APIs and the action JSON format the AI should use. This part is always appended and is not user-editable. It includes:

- Available API endpoints (`ssh.list_servers`, `ssh.execute`)
- Action JSON schema
- Marker format
- Risk level definitions
- Guidelines (max 3 actions per response, explain before executing, etc.)

### Prompt Assembly (in ChatService)

```java
String promptOverride = userRepository.getPromptOverride(userLogin);
String part1 = (promptOverride != null && !promptOverride.isBlank())
        ? promptOverride : configLoader.loadSystemPromptPart1Default();
String part2 = configLoader.loadSystemPromptPart2();
String systemPrompt = part1 + "\n\n" + part2;
```

## Action JSON Format

### Markers

The AI embeds action JSON between two text markers:

```
---ACTIONS_JSON_START---
{JSON content}
---ACTIONS_JSON_END---
```

The `ActionParser` splits the AI response:
- **Text before markers** + **text after markers** → displayed to user
- **JSON between markers** → parsed and stored as pending actions

### JSON Schema

The action JSON can be either an array of actions or an object with an `actions` array:

```json
{
  "actions": [
    {
      "id": "1",
      "api": "ssh.list_servers",
      "title": "List Available Servers",
      "description": "Retrieve all configured servers"
    },
    {
      "id": "2",
      "api": "ssh.execute",
      "title": "Check Disk Usage",
      "description": "Run df -h to check disk space on prod-web-01",
      "risk": "low",
      "params": {
        "server": "prod-web-01",
        "command": "df -h"
      }
    }
  ]
}
```

### Action Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique identifier within this action set |
| `api` | string | Yes | API to call: `ssh.list_servers` or `ssh.execute` |
| `title` | string | Yes | Human-readable title shown in UI |
| `description` | string | Yes | Explanation of what the action does |
| `risk` | string | No | Risk level: `low`, `medium`, or `high` |
| `params` | object | For `ssh.execute` | Parameters for the API call |
| `params.server` | string | For `ssh.execute` | Target server identifier |
| `params.command` | string | For `ssh.execute` | Shell command to execute |

### Available APIs

| API | Description | Params Required |
|-----|-------------|-----------------|
| `ssh.list_servers` | Lists all configured SSH servers | None |
| `ssh.execute` | Executes a command on a remote server | `server`, `command` |

## Risk Levels

The AI is instructed to assess risk for each suggested command:

| Level | Examples | Description |
|-------|----------|-------------|
| **low** | `uptime`, `ps aux`, `df -h`, `ls`, `cat`, `grep`, `ifconfig`, `netstat`, `systemctl status` | Read-only diagnostic commands |
| **medium** | `apt update`, `systemctl restart`, `chmod`, `chown`, `sed`, `yum install` | Configuration changes, service restarts, package operations |
| **high** | `rm -rf`, `dd if=/dev/zero`, `mkfs`, `reboot`, `shutdown`, `kill -9`, file overwrites | Destructive operations that can cause data loss or downtime |

## Read-Only Detection

The system prompt instructs the AI to:
1. **Prefer diagnostic commands first** — gather information before making changes
2. **Always explain commands** — describe what a command does before suggesting it
3. **Warn about dangerous operations** — explicitly flag `rm -rf`, `reboot`, `shutdown`, `dd`, `mkfs` etc.
4. **Respond in user's language** — match the language the user writes in

## Action Execution Flow

```
1. AI response contains ---ACTIONS_JSON_START--- ... ---ACTIONS_JSON_END---
2. ActionParser.parse() extracts:
   - textContent: everything outside the markers
   - actionsJson: the JSON between markers (validated as valid JSON)
3. If valid actions found:
   - Stored in pending_actions table (replacing previous)
   - UI renders action buttons
4. User clicks an action button:
   - POST /api/chat/action/{id}
   - ActionExecutor finds action by ID in the pending JSON
   - Dispatches to SshAgentService.listServers() or SshAgentService.execute()
   - AuditService logs the execution
   - Result saved as assistant message
5. If no actions in response:
   - pending_actions cleared for the user
```

## Guidelines for AI Responses

From the system prompt Part 2:
- Maximum **3 actions per response**
- Only include action markers when actions are needed
- Always assess risk level
- Prefer diagnostic (read-only) commands first
- Explain what a command does and its potential impact before including it
