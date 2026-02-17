var lastMessageId = 0;
var showDebug = false;
var isSending = false;
var pollTimer = null;

var READ_ONLY_PREFIXES = [
    'ls', 'pwd', 'whoami', 'id', 'uname', 'date', 'uptime', 'df', 'du', 'free',
    'ip a', 'ip route', 'ss ', 'netstat ',
    'ping', 'curl ', 'wget ',
    'cat ', 'tail ', 'head ', 'grep ',
    'journalctl ', 'systemctl status',
    'docker ps', 'docker logs', 'docker inspect', 'docker compose ps'
];

function getCookie(name) {
    var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : null;
}

function csrfHeaders() {
    var token = getCookie('XSRF-TOKEN');
    var headers = { 'Content-Type': 'application/json' };
    if (token) {
        headers['X-XSRF-TOKEN'] = token;
    }
    return headers;
}

function apiGet(url) {
    return fetch(url, { credentials: 'same-origin' }).then(function(r) {
        if (r.status === 401 || r.status === 403) {
            window.location.href = '/login';
            throw new Error('Unauthorized');
        }
        return r.json();
    });
}

function apiPost(url, body) {
    return fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers: csrfHeaders(),
        body: body !== undefined ? JSON.stringify(body) : undefined
    }).then(function(r) {
        if (r.status === 401 || r.status === 403) {
            window.location.href = '/login';
            throw new Error('Unauthorized');
        }
        return r.json();
    });
}

function isReadOnly(cmd) {
    var trimmed = cmd.trim();
    for (var i = 0; i < READ_ONLY_PREFIXES.length; i++) {
        var prefix = READ_ONLY_PREFIXES[i];
        if (trimmed === prefix || trimmed.indexOf(prefix) === 0) {
            if (prefix.charAt(prefix.length - 1) === ' ' || trimmed.length === prefix.length || trimmed.charAt(prefix.length) === ' ') {
                return true;
            }
        }
    }
    return false;
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function renderMarkdown(text) {
    var escaped = escapeHtml(text);
    var html = escaped.replace(/```(\w*)\n?([\s\S]*?)```/g, function(m, lang, code) {
        return '<pre><code>' + code.trim() + '</code></pre>';
    });
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    var lines = html.split('\n');
    var result = [];
    var inPre = false;
    for (var i = 0; i < lines.length; i++) {
        if (lines[i].indexOf('<pre>') !== -1) inPre = true;
        if (inPre) {
            result.push(lines[i]);
        } else if (lines[i].trim() === '') {
            result.push('</p><p>');
        } else {
            result.push(lines[i]);
        }
        if (lines[i].indexOf('</pre>') !== -1) inPre = false;
    }
    return '<p>' + result.join('\n') + '</p>';
}

function renderActions(actionsJson) {
    if (!actionsJson) return '';
    var actions;
    try {
        actions = JSON.parse(actionsJson);
    } catch (e) {
        return '';
    }
    if (!Array.isArray(actions)) {
        if (actions.actions) actions = actions.actions;
        else return '';
    }
    var html = '';
    for (var i = 0; i < actions.length; i++) {
        var a = actions[i];
        var id = a.id || (i + 1);
        var cmd = a.command || a.cmd || '';
        var api = a.api || 'ssh';
        var risk = (a.risk_level || a.riskLevel || 'medium').toLowerCase();
        var desc = a.description || a.desc || '';
        var riskClass = 'risk-medium';
        if (risk === 'low') riskClass = 'risk-low';
        else if (risk === 'high') riskClass = 'risk-high';

        var readOnly = isReadOnly(cmd);

        html += '<div class="action-card">';
        html += '<div class="action-header">';
        html += '<span class="action-api">' + escapeHtml(api) + '</span>';
        html += '<span class="risk-badge ' + riskClass + '">' + escapeHtml(risk) + '</span>';
        html += '</div>';
        if (desc) {
            html += '<div class="action-desc">' + escapeHtml(desc) + '</div>';
        }
        html += '<div class="action-command">' + escapeHtml(cmd) + '</div>';
        if (!readOnly) {
            html += '<div class="warning-msg">⚠ This is not a read-only command and may modify the system.</div>';
        }
        html += '<button class="btn-execute" onclick="executeAction(\'' + escapeHtml(String(id)) + '\')" id="btnAction' + id + '">Execute ' + id + '</button>';
        html += '</div>';
    }
    return html;
}

function addMessageToUI(role, text, debugData) {
    var container = document.getElementById('messages');
    var div = document.createElement('div');
    div.className = 'message message-' + role;

    if (role === 'assistant') {
        div.innerHTML = renderMarkdown(text);
    } else {
        div.textContent = text;
    }

    if (debugData && showDebug) {
        var details = document.createElement('details');
        var summary = document.createElement('summary');
        summary.textContent = 'Debug info';
        var pre = document.createElement('pre');
        pre.textContent = JSON.stringify(debugData, null, 2);
        details.appendChild(summary);
        details.appendChild(pre);
        div.appendChild(details);
    }

    container.appendChild(div);
    scrollToBottom();
}

function addActionsToUI(actionsJson) {
    var html = renderActions(actionsJson);
    if (!html) return;
    var container = document.getElementById('messages');
    var div = document.createElement('div');
    div.className = 'message message-assistant';
    div.innerHTML = html;
    container.appendChild(div);
    scrollToBottom();
}

function addSystemMessage(text, className) {
    var container = document.getElementById('messages');
    var div = document.createElement('div');
    div.className = className || 'prompt-updated-msg';
    div.textContent = text;
    container.appendChild(div);
    scrollToBottom();
}

function scrollToBottom() {
    var area = document.getElementById('chatArea');
    setTimeout(function() {
        area.scrollTop = area.scrollHeight;
    }, 50);
}

function showTyping() {
    document.getElementById('typing').classList.remove('hidden');
    scrollToBottom();
}

function hideTyping() {
    document.getElementById('typing').classList.add('hidden');
}

function setInputEnabled(enabled) {
    document.getElementById('userInput').disabled = !enabled;
    document.getElementById('btnSend').disabled = !enabled;
    isSending = !enabled;
}

function loadState() {
    apiGet('/api/chat/state?since=' + lastMessageId).then(function(data) {
        var msgs = data.messages || [];
        for (var i = 0; i < msgs.length; i++) {
            var m = msgs[i];
            var msgId = m.id || 0;
            if (msgId > lastMessageId) {
                lastMessageId = msgId;
            }
            addMessageToUI(m.role, m.content || m.text || '', showDebug ? m : null);
        }
        if (data.hasActions && data.actionsJson && msgs.length > 0) {
            addActionsToUI(data.actionsJson);
        }
    }).catch(function(e) {
        if (e.message !== 'Unauthorized') {
            console.error('loadState error:', e);
        }
    });
}

function sendMessage() {
    var input = document.getElementById('userInput');
    var text = input.value.trim();
    if (!text || isSending) return;

    addMessageToUI('user', text);
    input.value = '';
    autoResizeInput();
    setInputEnabled(false);
    showTyping();

    apiPost('/api/chat/send', { text: text }).then(function(data) {
        hideTyping();
        setInputEnabled(true);

        if (data.promptUpdated) {
            addSystemMessage(data.message || 'System prompt updated.');
            return;
        }

        if (data.error) {
            addMessageToUI('assistant', 'Error: ' + data.error);
            return;
        }

        addMessageToUI('assistant', data.text || '', showDebug ? data : null);

        if (data.hasActions && data.actionsJson) {
            addActionsToUI(data.actionsJson);
        }

        loadState();
    }).catch(function(e) {
        hideTyping();
        setInputEnabled(true);
        if (e.message !== 'Unauthorized') {
            addMessageToUI('assistant', 'Error sending message. Please try again.');
        }
    });
}

function newChat() {
    apiPost('/api/chat/new', {}).then(function() {
        document.getElementById('messages').innerHTML = '';
        lastMessageId = 0;
        addSystemMessage('New conversation started.');
    }).catch(function(e) {
        if (e.message !== 'Unauthorized') {
            alert('Error starting new chat.');
        }
    });
}

function executeAction(id) {
    var btn = document.getElementById('btnAction' + id);
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Executing...';
    }

    apiPost('/api/chat/action/' + id, {}).then(function(data) {
        if (btn) {
            btn.textContent = 'Done';
        }
        if (data.error) {
            addMessageToUI('assistant', 'Action error: ' + data.error);
        } else {
            loadState();
        }
    }).catch(function(e) {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Execute ' + id;
        }
        if (e.message !== 'Unauthorized') {
            addMessageToUI('assistant', 'Error executing action. Please try again.');
        }
    });
}

function startPromptUpdate() {
    apiPost('/api/prompt/start-update', {}).then(function(data) {
        document.getElementById('promptText').value = data.currentPrompt || '';
        document.getElementById('promptOverlay').classList.remove('hidden');
    }).catch(function(e) {
        if (e.message !== 'Unauthorized') {
            alert('Error loading prompt.');
        }
    });
}

function submitPrompt() {
    var text = document.getElementById('promptText').value.trim();
    if (!text) {
        alert('Prompt text cannot be empty.');
        return;
    }
    apiPost('/api/prompt/submit', { text: text }).then(function() {
        closePromptModal();
        addSystemMessage('System prompt updated successfully.');
    }).catch(function(e) {
        if (e.message !== 'Unauthorized') {
            alert('Error saving prompt.');
        }
    });
}

function closePromptModal() {
    document.getElementById('promptOverlay').classList.add('hidden');
}

function toggleDebug() {
    showDebug = document.getElementById('chkDebug').checked;
    apiPost('/api/user/settings/debug', { showDebug: showDebug }).catch(function(e) {
        console.error('toggleDebug error:', e);
    });
}

function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
}

function autoResizeInput() {
    var el = document.getElementById('userInput');
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

document.addEventListener('DOMContentLoaded', function() {
    var input = document.getElementById('userInput');
    input.addEventListener('input', autoResizeInput);

    apiGet('/api/user/id').then(function(data) {
        showDebug = data.showDebug || false;
        document.getElementById('chkDebug').checked = showDebug;
        loadState();
    }).catch(function(e) {
        if (e.message !== 'Unauthorized') {
            console.error('init error:', e);
            loadState();
        }
    });

    pollTimer = setInterval(loadState, 2000);
});
