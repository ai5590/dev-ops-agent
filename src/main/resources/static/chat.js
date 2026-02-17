let lastMessageId = 0;
let showDebug = false;
let pollingInterval = null;
let isSending = false;

const READ_ONLY_PREFIXES = [
    'ls', 'pwd', 'whoami', 'id', 'uname', 'date', 'uptime', 'df', 'du', 'free',
    'ip a', 'ip route', 'ss ', 'netstat ',
    'ping', 'curl ', 'wget ',
    'cat ', 'tail ', 'head ', 'grep ',
    'journalctl ', 'systemctl status',
    'docker ps', 'docker logs', 'docker inspect', 'docker compose ps'
];

function getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

async function apiFetch(url, options = {}) {
    const defaults = {
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        credentials: 'same-origin'
    };
    const merged = { ...defaults, ...options, headers: { ...defaults.headers, ...(options.headers || {}) } };
    const resp = await fetch(url, merged);
    if (resp.status === 401 || resp.status === 403) {
        window.location.href = '/login';
        throw new Error('Сессия истекла');
    }
    return resp;
}

async function init() {
    try {
        const resp = await apiFetch('/api/user/id');
        const data = await resp.json();
        showDebug = data.showDebug || false;
        const cb = document.getElementById('showDebugCheckbox');
        if (cb) cb.checked = showDebug;
    } catch (e) { console.error(e); }
    await loadState();
    pollingInterval = setInterval(loadState, 2000);
}

async function loadState() {
    try {
        const resp = await apiFetch('/api/chat/state?since=' + lastMessageId);
        const data = await resp.json();
        if (data.messages && data.messages.length > 0) {
            renderMessages(data.messages, data.hasActions, data.actionsJson);
        }
    } catch (e) {}
}

function renderMessages(messages, hasActions, actionsJson) {
    const container = document.getElementById('chatMessages');
    for (const msg of messages) {
        const id = msg.id;
        if (id > lastMessageId) lastMessageId = id;
        if (document.getElementById('msg-' + id)) continue;
        const div = document.createElement('div');
        div.id = 'msg-' + id;
        div.className = 'message msg-' + msg.role;
        div.innerHTML = '<div class="msg-content">' + formatContent(msg.content) + '</div>';
        container.appendChild(div);
    }
    if (hasActions && actionsJson) {
        renderActions(actionsJson);
    }
    scrollToBottom();
}

function formatContent(text) {
    if (!text) return '';
    text = text.replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
    text = text.replace(/`([^`]+)`/g, '<code>$1</code>');
    text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/\n/g, '<br>');
    return text;
}

function renderActions(actionsJson) {
    const existing = document.getElementById('actionsContainer');
    if (existing) existing.remove();
    try {
        const parsed = JSON.parse(actionsJson);
        if (!parsed.actions || parsed.actions.length === 0) return;
        const container = document.createElement('div');
        container.id = 'actionsContainer';
        container.className = 'actions-container';
        let html = '<div class="actions-header">Предложенные действия:</div>';
        for (const action of parsed.actions) {
            const isReadOnly = isCommandReadOnly(action.params?.command || '');
            const riskClass = 'risk-' + (action.risk || 'low');
            html += '<div class="action-card ' + riskClass + '">';
            html += '<div class="action-title">' + escapeHtml(action.title) + '</div>';
            html += '<div class="action-desc">' + escapeHtml(action.description) + '</div>';
            if (action.params?.server) html += '<div class="action-detail">Сервер: ' + escapeHtml(action.params.server) + '</div>';
            if (action.params?.command) html += '<div class="action-detail">Команда: <code>' + escapeHtml(action.params.command) + '</code></div>';
            html += '<div class="action-risk">Риск: <span class="' + riskClass + '">' + escapeHtml(action.risk || 'low') + '</span></div>';
            if (!isReadOnly) html += '<div class="warning-text">⚠️ Это не read-only команда и может изменить систему.</div>';
            html += '<button class="btn-primary action-btn" onclick="executeAction(\'' + action.id + '\')" id="actionBtn' + action.id + '">Выполнить ' + action.id + '</button>';
            html += '</div>';
        }
        if (showDebug) {
            html += '<details class="debug-details"><summary>JSON действий</summary><pre>' + escapeHtml(actionsJson) + '</pre></details>';
        }
        container.innerHTML = html;
        document.getElementById('chatMessages').appendChild(container);
        scrollToBottom();
    } catch (e) { console.error('Error rendering actions', e); }
}

function isCommandReadOnly(command) {
    const cmd = command.trim();
    return READ_ONLY_PREFIXES.some(prefix => cmd === prefix.trim() || cmd.startsWith(prefix));
}

async function sendMessage() {
    if (isSending) return;
    const input = document.getElementById('messageInput');
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    isSending = true;
    updateSendButton(true);
    showTypingIndicator(true);
    try {
        const resp = await apiFetch('/api/chat/send', {
            method: 'POST',
            body: JSON.stringify({ text: text })
        });
        const data = await resp.json();
        if (data.promptUpdated) {
            showNotification(data.message || 'Промпт обновлён');
        }
        await loadState();
    } catch (e) {
        showNotification('Ошибка отправки: ' + e.message, true);
    } finally {
        isSending = false;
        updateSendButton(false);
        showTypingIndicator(false);
    }
}

async function executeAction(actionId) {
    const btn = document.getElementById('actionBtn' + actionId);
    if (btn) { btn.disabled = true; btn.textContent = 'Выполняется...'; }
    try {
        const resp = await apiFetch('/api/chat/action/' + actionId, { method: 'POST' });
        const data = await resp.json();
        if (data.success) {
            const container = document.getElementById('chatMessages');
            const div = document.createElement('div');
            div.className = 'message msg-assistant';
            let content = '<div class="msg-content"><strong>Результат (' + escapeHtml(data.api || '') + '):</strong><pre><code>' + escapeHtml(data.output || '') + '</code></pre>';
            if (showDebug) {
                content += '<details class="debug-details"><summary>Детали</summary>';
                content += '<p>Сервер: ' + escapeHtml(data.server || '-') + '</p>';
                content += '<p>Команда: ' + escapeHtml(data.command || '-') + '</p>';
                content += '<p>Время: ' + (data.duration_ms || 0) + ' мс</p>';
                content += '</details>';
            }
            content += '</div>';
            div.innerHTML = content;
            container.appendChild(div);
            scrollToBottom();
        } else {
            showNotification('Ошибка: ' + (data.error || 'неизвестная ошибка'), true);
        }
    } catch (e) {
        showNotification('Ошибка выполнения: ' + e.message, true);
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Выполнить ' + actionId; }
    }
}

async function newChat() {
    if (!confirm('Начать новый диалог? История будет удалена.')) return;
    try {
        await apiFetch('/api/chat/new', { method: 'POST' });
        document.getElementById('chatMessages').innerHTML = '';
        lastMessageId = 0;
        const ac = document.getElementById('actionsContainer');
        if (ac) ac.remove();
    } catch (e) {
        showNotification('Ошибка: ' + e.message, true);
    }
}

async function startPromptUpdate() {
    try {
        const resp = await apiFetch('/api/prompt/start-update', { method: 'POST' });
        const data = await resp.json();
        document.getElementById('promptText').value = data.currentPrompt || '';
        document.getElementById('promptModal').style.display = 'flex';
    } catch (e) {
        showNotification('Ошибка: ' + e.message, true);
    }
}

async function submitPrompt() {
    const text = document.getElementById('promptText').value.trim();
    if (!text) return;
    try {
        await apiFetch('/api/prompt/submit', {
            method: 'POST',
            body: JSON.stringify({ text: text })
        });
        document.getElementById('promptModal').style.display = 'none';
        showNotification('Промпт обновлён');
    } catch (e) {
        showNotification('Ошибка: ' + e.message, true);
    }
}

function closePromptModal() {
    document.getElementById('promptModal').style.display = 'none';
}

function toggleDebug() {
    const cb = document.getElementById('showDebugCheckbox');
    showDebug = cb.checked;
    apiFetch('/api/user/settings/debug', {
        method: 'POST',
        body: JSON.stringify({ showDebug: showDebug })
    }).catch(e => console.error(e));
}

function scrollToBottom() {
    const container = document.getElementById('chatMessages');
    if (container) container.scrollTop = container.scrollHeight;
}

function showTypingIndicator(show) {
    const el = document.getElementById('typingIndicator');
    if (el) el.style.display = show ? 'flex' : 'none';
}

function updateSendButton(disabled) {
    const btn = document.getElementById('sendBtn');
    if (btn) {
        btn.disabled = disabled;
        btn.textContent = disabled ? 'Отправка...' : 'Отправить';
    }
}

function showNotification(msg, isError) {
    const existing = document.getElementById('notification');
    if (existing) existing.remove();
    const div = document.createElement('div');
    div.id = 'notification';
    div.className = 'notification ' + (isError ? 'status-error' : 'status-success');
    div.textContent = msg;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 3000);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', () => {
    init();
    const input = document.getElementById('messageInput');
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
        });
        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 120) + 'px';
        });
    }
});
