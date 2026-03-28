/**
 * SSE client — receives real-time events from the server and updates the UI.
 * Loaded only on sessions/detail.html.
 */
(function () {
    const meta = document.getElementById('session-meta');
    if (!meta) return;

    const sdkSessionId = meta.dataset.sdkSessionId;
    if (!sdkSessionId || sdkSessionId === 'null' || sdkSessionId === '') return;

    const streamOutput = document.getElementById('stream-output');
    const streamCursor = document.getElementById('stream-cursor');
    const streamEmptyState = document.getElementById('stream-empty-state');
    const eventConsole = document.getElementById('event-console');
    const toolTimeline = document.getElementById('tool-timeline');
    let lastStatus = normalizeStatus(meta.dataset.status);
    let lastStableStatus = lastStatus;

    // Track active tool cards by tool name
    const activeToolCards = new Map();

    let evtSource = null;
    let reconnectDelay = 1000;

    function connect() {
        evtSource = new EventSource(`/api/sessions/${sdkSessionId}/stream`);

        evtSource.onopen = () => {
            reconnectDelay = 1000;
            setStatus(lastStableStatus);
        };

        evtSource.onmessage = (e) => {
            let event;
            try {
                event = JSON.parse(e.data);
            } catch {
                return;
            }

            switch (event.type) {
                case 'ASSISTANT_DELTA':
                    hideStreamEmptyState();
                    if (streamCursor) streamCursor.style.display = 'inline';
                    streamOutput.insertAdjacentText('beforeend', event.content || '');
                    streamOutput.parentElement.scrollTop = streamOutput.parentElement.scrollHeight;
                    break;

                case 'ASSISTANT_MSG':
                    hideStreamEmptyState();
                    if (event.content && streamOutput.textContent === '') {
                        streamOutput.textContent = event.content;
                    }
                    if (streamCursor) streamCursor.style.display = 'none';
                    streamOutput.parentElement.scrollTop = streamOutput.parentElement.scrollHeight;
                    break;

                case 'IDLE':
                    setStatus('idle');
                    window.enableInput && window.enableInput();
                    if (streamCursor) streamCursor.style.display = 'none';
                    break;

                case 'TOOL_START':
                    addToolCard(event.toolCallId, event.toolName, event.args, 'running');
                    break;

                case 'TOOL_COMPLETE':
                    updateToolCard(event.toolCallId, event.result);
                    break;

                case 'SUBAGENT_START':
                    appendConsoleEntry(event, 'subagent-start');
                    break;

                case 'SUBAGENT_COMPLETE':
                    appendConsoleEntry(event, 'subagent-complete');
                    break;

                case 'SESSION_ERROR':
                    setStatus('error');
                    window.enableInput && window.enableInput();
                    appendErrorBanner(event.content);
                    if (streamCursor) streamCursor.style.display = 'none';
                    break;

                case 'ABORT':
                    setStatus('idle');
                    window.enableInput && window.enableInput();
                    if (streamCursor) streamCursor.style.display = 'none';
                    break;
            }

            appendConsoleEntry(event);
        };

        evtSource.onerror = () => {
            evtSource.close();
            setStatus('disconnected');
            if (streamCursor) streamCursor.style.display = 'none';
            // Attempt reconnect with backoff
            setTimeout(() => {
                reconnectDelay = Math.min(reconnectDelay * 2, 30000);
                connect();
            }, reconnectDelay);
        };
    }

    function hideStreamEmptyState() {
        if (streamEmptyState) {
            streamEmptyState.style.display = 'none';
        }
    }

    function addToolCard(toolCallId, toolName, args, state) {
        if (!toolTimeline || !toolCallId) return;
        removeToolEmptyState();
        const id = 'tool-' + toolCallId.replace(/\W/g, '_');
        const card = document.createElement('div');
        card.className = 'tool-card mb-1 p-2 rounded border border-warning bg-warning bg-opacity-10 small';
        card.id = id;
        card.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <span class="spinner-border spinner-border-sm text-warning tool-spinner" role="status"></span>
                <span class="fw-semibold text-warning">${escapeHtml(toolName || toolCallId)}</span>
                <span class="badge bg-warning text-dark tool-state">running</span>
            </div>
            ${args ? `<pre class="mt-1 mb-0 text-secondary" style="font-size:0.7rem;white-space:pre-wrap;max-height:60px;overflow:auto;">${escapeHtml(args)}</pre>` : ''}
        `;
        activeToolCards.set(toolCallId, id);
        toolTimeline.appendChild(card);
        toolTimeline.scrollTop = toolTimeline.scrollHeight;
    }

    function updateToolCard(toolCallId, result) {
        const id = activeToolCards.get(toolCallId);
        if (!id) return;
        const card = document.getElementById(id);
        if (!card) return;
        const spinner = card.querySelector('.tool-spinner');
        const badge = card.querySelector('.tool-state');
        if (spinner) spinner.remove();
        if (badge) { badge.textContent = 'done'; badge.className = 'badge bg-success tool-state'; }
        if (result) {
            const pre = document.createElement('pre');
            pre.className = 'mt-1 mb-0 text-light';
            pre.style = 'font-size:0.7rem;white-space:pre-wrap;max-height:80px;overflow:auto;';
            pre.textContent = result;
            card.appendChild(pre);
        }
        activeToolCards.delete(toolCallId);
    }

    function appendConsoleEntry(event, extraClass) {
        if (!eventConsole) return;
        removeEventConsoleEmptyState();
        const ts = new Date(event.timestamp).toLocaleTimeString();
        const typeClass = {
            'USER_MSG': 'text-info',
            'ASSISTANT_MSG': 'text-success',
            'ASSISTANT_DELTA': 'text-success opacity-50',
            'TOOL_START': 'text-warning',
            'TOOL_COMPLETE': 'text-warning',
            'SESSION_ERROR': 'text-danger',
            'SUBAGENT_START': 'text-primary',
            'SUBAGENT_COMPLETE': 'text-primary',
            'IDLE': 'text-secondary',
            'ABORT': 'text-danger',
        }[event.type] || 'text-secondary';

        const entry = document.createElement('div');
        entry.className = `event-entry mb-1 ${typeClass} ${extraClass || ''}`;

        let label = event.type;
        if (event.type === 'TOOL_START' || event.type === 'TOOL_COMPLETE') {
            label += ` [${event.toolName || ''}]`;
        }

        const snippet = event.content
            ? truncate(event.content, 80)
            : (event.toolName ? event.toolName : '');

        entry.innerHTML = `<span class="text-secondary me-1">${ts}</span><span class="badge bg-dark border border-secondary me-1">${escapeHtml(label)}</span>${escapeHtml(snippet)}`;
        eventConsole.appendChild(entry);
        eventConsole.scrollTop = eventConsole.scrollHeight;
    }

    function appendErrorBanner(message) {
        document.querySelectorAll('.session-stream-error-banner').forEach(el => el.remove());
        const banner = document.createElement('div');
        banner.className = 'alert alert-danger alert-dismissible mt-2 session-stream-error-banner';
        banner.innerHTML = `<i class="bi bi-exclamation-triangle me-2"></i>${escapeHtml(message || 'An error occurred.')}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        const streamCard = streamOutput?.closest('.card');
        if (streamCard) streamCard.after(banner);
    }

    function removeToolEmptyState() {
        const emptyState = document.getElementById('tool-empty-state');
        if (emptyState) emptyState.remove();
    }

    function removeEventConsoleEmptyState() {
        const emptyState = document.getElementById('event-console-empty');
        if (emptyState) emptyState.remove();
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function truncate(str, len) {
        return str.length > len ? str.slice(0, len) + '…' : str;
    }

    function normalizeStatus(status) {
        return ({
            'ACTIVE': 'active',
            'CREATING': 'running',
            'IDLE': 'idle',
            'ERROR': 'error',
            'CLOSED': 'closed',
        }[status] || 'idle');
    }

    function setStatus(state) {
        lastStatus = state;
        if (state !== 'disconnected') {
            lastStableStatus = state;
        }
        const dot = document.getElementById('status-dot');
        const text = document.getElementById('status-text');
        const badge = document.getElementById('status-badge');
        if (!dot || !text) return;

        dot.className = 'status-dot status-' + state;
        text.textContent = state;

        if (badge) {
            badge.className = 'badge fs-6 ' + ({
                'active': 'bg-success',
                'running': 'bg-warning text-dark',
                'idle': 'bg-primary',
                'error': 'bg-danger',
                'closed': 'bg-secondary',
                'disconnected': 'bg-secondary',
            }[state] || 'bg-secondary');
            badge.textContent = state.toUpperCase();
        }
    }

    // Expose setStatus globally for chat.js
    window.setStatus = setStatus;

    setStatus(lastStatus);
    connect();
})();
