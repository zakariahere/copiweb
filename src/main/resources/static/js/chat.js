/**
 * Chat interaction — send messages, abort, manage input state.
 * Loaded only on sessions/detail.html.
 */
(function () {
    const meta = document.getElementById('session-meta');
    if (!meta) return;

    const dbSessionId = meta.dataset.dbSessionId;
    const sendBtn = document.getElementById('send-btn');
    const abortBtn = document.getElementById('abort-btn');
    const messageInput = document.getElementById('message-input');
    const streamOutput = document.getElementById('stream-output');
    const streamEmptyState = document.getElementById('stream-empty-state');
    const toolTimeline = document.getElementById('tool-timeline');
    const eventConsole = document.getElementById('event-console');
    const defaultPlaceholder = messageInput.placeholder;
    const toolEmptyStateMarkup =
        '<div id="tool-empty-state" class="tool-empty-state text-secondary small text-center py-3">No tool activity yet for this turn.</div>';

    if (!sendBtn || !messageInput) return;

    // Disable input if session is already closed/error on page load
    const initialStatus = meta.dataset.status;
    if (initialStatus === 'CLOSED') {
        disableInput('Session is closed.');
    } else if (initialStatus === 'ERROR') {
        disableInput('Resume the session to continue.');
    }

    function sendMessage() {
        const text = messageInput.value.trim();
        if (!text) return;

        removeApiError();

        // Clear previous response
        if (streamOutput) streamOutput.textContent = '';
        if (streamEmptyState) {
            streamEmptyState.textContent = 'Waiting for the agent response...';
            streamEmptyState.style.display = '';
        }
        if (toolTimeline) toolTimeline.innerHTML = toolEmptyStateMarkup;

        disableInput();
        window.setStatus && window.setStatus('running');
        if (abortBtn) abortBtn.style.display = '';

        fetch(`/api/sessions/${dbSessionId}/message`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({message: text}),
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(err => { throw new Error(err.error || 'Send failed'); });
            }
            messageInput.value = '';
        })
        .catch(err => {
            window.setStatus && window.setStatus('error');
            enableInput();
            appendApiError(err.message);
        });
    }

    function abortSession() {
        fetch(`/api/sessions/${dbSessionId}/abort`, {method: 'POST'})
            .catch(err => console.warn('Abort request failed:', err));
        if (abortBtn) abortBtn.style.display = 'none';
    }

    function disableInput(placeholder) {
        messageInput.disabled = true;
        sendBtn.disabled = true;
        if (placeholder) messageInput.placeholder = placeholder;
    }

    window.enableInput = function () {
        messageInput.disabled = false;
        sendBtn.disabled = false;
        messageInput.placeholder = defaultPlaceholder;
        if (abortBtn) abortBtn.style.display = 'none';
        messageInput.focus();
    };

    function removeApiError() {
        const container = messageInput.closest('.card');
        if (!container) return;
        const existing = container.querySelector('.api-error-banner');
        if (existing) existing.remove();
    }

    function appendApiError(message) {
        const container = messageInput.closest('.card');
        if (!container) return;
        removeApiError();
        const banner = document.createElement('div');
        banner.className = 'alert alert-danger mt-2 small api-error-banner';
        banner.innerHTML = `<i class="bi bi-exclamation-triangle me-1"></i>${escapeHtml(message)}`;
        container.querySelector('.card-body').appendChild(banner);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    sendBtn.addEventListener('click', sendMessage);

    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    if (abortBtn) {
        abortBtn.addEventListener('click', abortSession);
    }

    const clearConsoleBtn = document.getElementById('clear-console-btn');
    if (clearConsoleBtn) {
        clearConsoleBtn.addEventListener('click', () => {
            if (eventConsole) {
                eventConsole.innerHTML = '<div id="event-console-empty" class="text-secondary small text-center py-3">Console cleared for this browser view.</div>';
            }
        });
    }
})();
