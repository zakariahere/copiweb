/**
 * Command Palette — type "/" in the message input to browse and launch workflow commands.
 * Loaded on sessions/detail.html alongside chat.js and sse-client.js.
 */
(function () {
    const messageInput = document.getElementById('message-input');
    if (!messageInput) return;

    let commands = [];
    let paletteVisible = false;
    let filtered = [];
    let selectedIdx = 0;

    // ── DOM ──────────────────────────────────────────────────────────────────
    const palette = document.createElement('div');
    palette.id = 'command-palette';
    palette.className = 'position-absolute bg-dark border border-secondary rounded shadow-lg';
    palette.style.cssText = 'bottom: 100%; left: 0; right: 0; max-height: 280px; overflow-y: auto; z-index: 1050; display: none;';
    messageInput.parentElement.style.position = 'relative';
    messageInput.parentElement.appendChild(palette);

    // Param modal
    const modal = document.createElement('div');
    modal.innerHTML = `
    <div class="modal fade" id="cmdParamModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content bg-dark border-secondary">
          <div class="modal-header border-secondary">
            <h6 class="modal-title" id="cmdModalTitle"></h6>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body" id="cmdModalBody"></div>
          <div class="modal-footer border-secondary">
            <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary btn-sm" id="cmdModalRun">
              <i class="bi bi-send me-1"></i>Use Prompt
            </button>
          </div>
        </div>
      </div>
    </div>`;
    document.body.appendChild(modal);
    const bsModal = new bootstrap.Modal(document.getElementById('cmdParamModal'));

    // ── Fetch commands ───────────────────────────────────────────────────────
    fetch('/api/commands')
        .then(r => r.json())
        .then(data => { commands = data; })
        .catch(() => {});

    // ── Input handler ────────────────────────────────────────────────────────
    messageInput.addEventListener('input', () => {
        const val = messageInput.value;
        if (val.startsWith('/')) {
            const query = val.slice(1).toLowerCase();
            filtered = commands.filter(c =>
                c.name.toLowerCase().includes(query) ||
                c.label.toLowerCase().includes(query)
            );
            selectedIdx = 0;
            showPalette();
        } else {
            hidePalette();
        }
    });

    messageInput.addEventListener('keydown', (e) => {
        if (!paletteVisible) return;
        if (e.key === 'ArrowDown') { e.preventDefault(); selectedIdx = Math.min(selectedIdx + 1, filtered.length - 1); renderPalette(); }
        else if (e.key === 'ArrowUp') { e.preventDefault(); selectedIdx = Math.max(selectedIdx - 1, 0); renderPalette(); }
        else if (e.key === 'Enter' && !e.ctrlKey) { e.preventDefault(); if (filtered[selectedIdx]) selectCommand(filtered[selectedIdx]); }
        else if (e.key === 'Escape') { hidePalette(); }
    });

    document.addEventListener('click', (e) => {
        if (!palette.contains(e.target) && e.target !== messageInput) hidePalette();
    });

    // ── Palette rendering ────────────────────────────────────────────────────
    function showPalette() {
        paletteVisible = true;
        palette.style.display = '';
        renderPalette();
    }

    function hidePalette() {
        paletteVisible = false;
        palette.style.display = 'none';
    }

    function renderPalette() {
        if (filtered.length === 0) {
            palette.innerHTML = '<div class="px-3 py-2 text-secondary small">No commands match</div>';
            return;
        }
        palette.innerHTML = filtered.map((cmd, i) => {
            const isSelected = i === selectedIdx;
            return `
            <div class="palette-item d-flex align-items-center gap-2 px-3 py-2 cursor-pointer ${isSelected ? 'bg-primary bg-opacity-25' : ''}"
                 style="cursor:pointer;" data-idx="${i}">
                <i class="${cmd.icon} text-warning" style="width:1rem;"></i>
                <div class="flex-grow-1 min-w-0">
                    <div class="d-flex align-items-center gap-1">
                        <span class="fw-semibold small" style="white-space:nowrap;">${escHtml(cmd.label)}</span>
                        <code class="text-secondary" style="font-size:0.7rem;">/${escHtml(cmd.name)}</code>
                    </div>
                    <div class="text-secondary" style="font-size:0.72rem; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escHtml(cmd.description || '')}</div>
                </div>
            </div>`;
        }).join('');

        palette.querySelectorAll('.palette-item').forEach(item => {
            item.addEventListener('mouseenter', () => {
                selectedIdx = parseInt(item.dataset.idx);
                renderPalette();
            });
            item.addEventListener('click', () => {
                selectCommand(filtered[parseInt(item.dataset.idx)]);
            });
        });
    }

    // ── Command selection ────────────────────────────────────────────────────
    function selectCommand(cmd) {
        hidePalette();
        messageInput.value = '';

        const schema = parseSchema(cmd.paramSchema);

        if (!schema || schema.length === 0) {
            // No params — just insert the template directly
            messageInput.value = cmd.promptTemplate || '';
            messageInput.focus();
            return;
        }

        // Has params — show the modal
        document.getElementById('cmdModalTitle').innerHTML =
            `<i class="${cmd.icon} me-1 text-warning"></i>${escHtml(cmd.label)}`;

        const body = document.getElementById('cmdModalBody');
        body.innerHTML = schema.map(p => `
            <div class="mb-3">
                <label class="form-label fw-semibold small">
                    ${escHtml(p.label || p.name)}
                    ${p.required ? '<span class="text-danger">*</span>' : '<span class="text-secondary fw-normal">(optional)</span>'}
                </label>
                ${p.multiline
                    ? `<textarea class="form-control bg-black text-light border-secondary font-monospace small param-input"
                                 id="param-${escHtml(p.name)}" rows="4"
                                 placeholder="${escHtml(p.placeholder || '')}"
                                 ${p.required ? 'required' : ''}></textarea>`
                    : `<input type="text" class="form-control bg-black text-light border-secondary param-input"
                              id="param-${escHtml(p.name)}"
                              placeholder="${escHtml(p.placeholder || '')}"
                              ${p.required ? 'required' : ''}>`
                }
            </div>`).join('');

        const runBtn = document.getElementById('cmdModalRun');
        const newRunBtn = runBtn.cloneNode(true);
        runBtn.parentNode.replaceChild(newRunBtn, runBtn);

        newRunBtn.addEventListener('click', async () => {
            const params = {};
            let valid = true;
            schema.forEach(p => {
                const el = document.getElementById('param-' + p.name);
                if (el) {
                    params[p.name] = el.value.trim();
                    if (p.required && !params[p.name]) {
                        el.classList.add('is-invalid');
                        valid = false;
                    } else {
                        el.classList.remove('is-invalid');
                    }
                }
            });
            if (!valid) return;

            try {
                const res = await fetch(`/api/commands/${cmd.id}/assemble`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(params)
                });
                const data = await res.json();
                messageInput.value = data.prompt || '';
                bsModal.hide();
                messageInput.focus();
            } catch {
                messageInput.value = cmd.promptTemplate || '';
                bsModal.hide();
            }
        });

        bsModal.show();
        setTimeout(() => {
            const first = body.querySelector('.param-input');
            if (first) first.focus();
        }, 300);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    function parseSchema(schemaStr) {
        if (!schemaStr) return [];
        try { return JSON.parse(schemaStr); } catch { return []; }
    }

    function escHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
})();
