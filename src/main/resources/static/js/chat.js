document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chat-form');
    const messageInput = document.getElementById('message-input');
    const chatHistory = document.getElementById('chat-history');
    const sendButton = document.getElementById('send-button');
    const clearButton = document.getElementById('clear-button');
    const regenerateButton = document.getElementById('regenerate-button');
    const stopButton = document.getElementById('stop-generation');
    const configButton = document.getElementById('config-button');
    const configPanel = document.getElementById('config-panel');
    const saveConfigButton = document.getElementById('save-config');
    const cancelConfigButton = document.getElementById('cancel-config');
    const ollamaUrlInput = document.getElementById('ollama-url');
    const modelSelect = document.getElementById('model-select');
    const refreshModelsButton = document.getElementById('refresh-models');

    // New: Agent Selector
    const agentSelect = document.getElementById('agent-select');

    // Search elements
    const searchButton = document.getElementById('search-button');
    const searchPanel = document.getElementById('search-panel');
    const closeSearch = document.getElementById('close-search');
    const searchInput = document.getElementById('search-input');
    const searchResults = document.getElementById('search-results');

    // Advanced parameters
    const streamingMode = document.getElementById('streaming-mode');
    const temperatureInput = document.getElementById('temperature');
    const tempValue = document.getElementById('temp-value');
    const topPInput = document.getElementById('top-p');
    const topPValue = document.getElementById('top-p-value');
    const maxTokensInput = document.getElementById('max-tokens');
    const maxTokensValue = document.getElementById('max-tokens-value');

    // Feature buttons
    const themeToggle = document.getElementById('theme-toggle');
    const historyButton = document.getElementById('history-button');
    const promptButton = document.getElementById('prompt-button');
    const statsButton = document.getElementById('stats-button');
    const voiceButton = document.getElementById('voice-button');
    const sessionsButton = document.getElementById('sessions-button');

    // Panels
    const historyPanel = document.getElementById('history-panel');
    const promptPanel = document.getElementById('prompt-panel');
    const statsPanel = document.getElementById('stats-panel');
    const sessionsPanel = document.getElementById('sessions-panel');

    // Panel close buttons
    const closeHistory = document.getElementById('close-history');
    const closePrompt = document.getElementById('close-prompt');
    const closeStats = document.getElementById('close-stats');
    const closeSessions = document.getElementById('close-sessions');

    // Other elements
    const saveConversation = document.getElementById('save-conversation');
    const exportConversation = document.getElementById('export-conversation');
    const attachButton = document.getElementById('attach-button');
    const fileInput = document.getElementById('file-input');
    const filePreview = document.getElementById('file-preview');
    const previewImage = document.getElementById('preview-image');
    const removeFile = document.getElementById('remove-file');
    const voiceInputBtn = document.getElementById('voice-input-btn');
    const ttsToggle = document.getElementById('tts-toggle');
    const networkStatus = document.getElementById('network-status');
    const webSearchToggle = document.getElementById('web-search-toggle');

    // State variables
    let lastUserMessage = '';
    let currentTheme = localStorage.getItem('theme') || 'dark';
    let currentSession = 'default';
    let sessions = JSON.parse(localStorage.getItem('sessions') || '{}');
    let stats = JSON.parse(localStorage.getItem('stats') || '{"messages": 0, "sessions": 1, "totalTime": 0, "tokens": 0}');
    let uploadedFile = null;
    let isGenerating = false;
    let currentController = null;
    let recognition = null;
    let synthesis = window.speechSynthesis;
    let ttsEnabled = false;
    let streamingEnabled = localStorage.getItem('streaming') !== 'false';
    let currentEventSource = null;
    let advancedParams = JSON.parse(localStorage.getItem('advancedParams') || '{"temperature": 0.7, "topP": 0.9, "maxTokens": 2048}');
    let webSearchEnabled = localStorage.getItem('webSearch') === 'true';

    // Initialize
    applyTheme(currentTheme);
    loadConfig();
    updateStats();
    checkNetworkStatus();
    loadAgents(); // Load Agent List on startup

    // Initialize advanced params
    streamingMode.checked = streamingEnabled;
    temperatureInput.value = advancedParams.temperature;
    tempValue.textContent = advancedParams.temperature;
    topPInput.value = advancedParams.topP;
    topPValue.textContent = advancedParams.topP;
    maxTokensInput.value = advancedParams.maxTokens;
    maxTokensValue.textContent = advancedParams.maxTokens;
    webSearchToggle.checked = webSearchEnabled;

    // ===== AGENT FUNCTIONS =====

    // Listener for Agent Switching
    if (agentSelect) {
        console.log('Agent select found, adding event listener');
        agentSelect.addEventListener('change', switchAgent);
    } else {
        console.error('Agent select element not found!');
    }

    function loadAgents() {
        console.log('Loading agents...');
        if (!agentSelect) {
            console.error('Agent select not found in loadAgents');
            return;
        }

        fetch('/api/agents')
            .then(response => response.json())
            .then(data => {
                console.log('Agents loaded:', data);
                if (data.status === 'success') {
                    // Keep the first default option
                    while (agentSelect.options.length > 1) {
                        agentSelect.remove(1);
                    }

                    let currentDept = '';
                    let group = null;

                    data.agents.forEach(agent => {
                        // Create OptGroup for departments
                        if (agent.department !== currentDept) {
                            currentDept = agent.department;
                            group = document.createElement('optgroup');
                            group.label = currentDept;
                            agentSelect.appendChild(group);
                        }

                        const option = document.createElement('option');
                        option.value = agent.fullPath;
                        // Show name and short description
                        option.text = `${agent.name} - ${agent.description.substring(0, 20)}${agent.description.length > 20 ? '...' : ''}`;
                        option.title = agent.description; // Full description on hover

                        if (group) {
                            group.appendChild(option);
                        } else {
                            agentSelect.appendChild(option);
                        }
                    });
                }
            })
            .catch(error => console.error('Error loading agents:', error));
    }

    function switchAgent() {
        console.log('switchAgent called!');
        const filePath = agentSelect.value;
        console.log('Selected file path:', filePath);
        const formData = new URLSearchParams();
        formData.append('filePath', filePath);

        // Disable input while switching
        messageInput.disabled = true;
        chatHistory.style.opacity = '0.5';

        fetch('/api/chat/agent', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                // Clear chat UI to indicate new context
                chatHistory.innerHTML = `<div class="message system-message"><p>🔄 ${data.message}</p></div>`;

                // If it's a specific agent, maybe show its description as a welcome message
                if (filePath !== 'default') {
                     const selectedOption = agentSelect.options[agentSelect.selectedIndex];
                     if (selectedOption) {
                         const desc = selectedOption.title;
                         chatHistory.innerHTML += `<div class="message ai-message"><p>你好！我是<strong>${selectedOption.text.split('-')[0]}</strong>。<br>${desc}<br>有什麼我可以幫你的嗎？</p></div>`;
                     }
                }
            } else {
                alert('切換失敗: ' + data.message);
                agentSelect.value = 'default'; // Revert to default
            }
        })
        .catch(error => {
            console.error('Error switching agent:', error);
            alert('切換發生錯誤');
        })
        .finally(() => {
            messageInput.disabled = false;
            chatHistory.style.opacity = '1';
            messageInput.focus();
        });
    }

    // Parameter change listeners
    temperatureInput.addEventListener('input', (e) => {
        tempValue.textContent = e.target.value;
        advancedParams.temperature = parseFloat(e.target.value);
        localStorage.setItem('advancedParams', JSON.stringify(advancedParams));
    });

    topPInput.addEventListener('input', (e) => {
        topPValue.textContent = e.target.value;
        advancedParams.topP = parseFloat(e.target.value);
        localStorage.setItem('advancedParams', JSON.stringify(advancedParams));
    });

    maxTokensInput.addEventListener('input', (e) => {
        maxTokensValue.textContent = e.target.value;
        advancedParams.maxTokens = parseInt(e.target.value);
        localStorage.setItem('advancedParams', JSON.stringify(advancedParams));
    });

    streamingMode.addEventListener('change', (e) => {
        streamingEnabled = e.target.checked;
        localStorage.setItem('streaming', streamingEnabled);
    });

    webSearchToggle.addEventListener('change', (e) => {
        webSearchEnabled = e.target.checked;
        localStorage.setItem('webSearch', webSearchEnabled);
        if (webSearchEnabled) {
            chatHistory.innerHTML += '<div class="message system-message"><p>🌐 網頁搜尋已啟用，AI 將會搜尋網路資訊來回答問題。</p></div>';
        } else {
            chatHistory.innerHTML += '<div class="message system-message"><p>網頁搜尋已關閉。</p></div>';
        }
        chatHistory.scrollTop = chatHistory.scrollHeight;
    });

    // Refresh models button
    refreshModelsButton.addEventListener('click', async () => {
        const ollamaUrl = ollamaUrlInput.value.trim();

        if (!ollamaUrl) {
            alert('請先輸入 Ollama URL');
            return;
        }

        refreshModelsButton.disabled = true;
        refreshModelsButton.textContent = '⌛ 抓取中...';

        try {
            const response = await fetch('/api/models?ollamaUrl=' + encodeURIComponent(ollamaUrl));
            const data = await response.json();

            if (data.status === 'success' && data.models && data.models.length > 0) {
                // Clear existing options
                modelSelect.innerHTML = '';

                // Add fetched models
                data.models.forEach(model => {
                    const option = document.createElement('option');
                    option.value = model;
                    option.textContent = model;
                    modelSelect.appendChild(option);
                });

                alert('成功抓取 ' + data.models.length + ' 個模型！');
            } else {
                alert('錯誤：' + (data.message || '無法取得模型列表'));
            }
        } catch (error) {
            console.error('Error fetching models:', error);
            alert('抓取模型時發生錯誤：' + error.message);
        } finally {
            refreshModelsButton.disabled = false;
            refreshModelsButton.textContent = '🔄 抓取模型';
        }
    });

    // ===== SEARCH FUNCTIONALITY =====
    searchButton.addEventListener('click', () => {
        searchPanel.style.display = searchPanel.style.display === 'none' ? 'flex' : 'none';
        if (searchPanel.style.display === 'flex') {
            historyPanel.style.display = 'none';
            promptPanel.style.display = 'none';
            statsPanel.style.display = 'none';
            sessionsPanel.style.display = 'none';
            searchInput.focus();
        }
    });

    closeSearch.addEventListener('click', () => {
        searchPanel.style.display = 'none';
    });

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim().toLowerCase();

        if (!query) {
            searchResults.innerHTML = '<p class="empty-message">輸入關鍵字開始搜尋</p>';
            return;
        }

        const messages = Array.from(chatHistory.querySelectorAll('.message:not(.system-message)'));
        const results = [];

        messages.forEach((msg, index) => {
            const text = msg.textContent.toLowerCase();
            if (text.includes(query)) {
                const type = msg.classList.contains('user-message') ? '👤 使用者' : '🤖 AI';
                const preview = msg.textContent.substring(0, 100).replace('複製', '').replace('已複製!', '');
                results.push({ type, preview, element: msg, index });
            }
        });

        if (results.length === 0) {
            searchResults.innerHTML = '<p class="empty-message">沒有找到相關結果</p>';
            return;
        }

        searchResults.innerHTML = '<div class="search-count">找到 ' + results.length + ' 筆結果</div>';
        results.forEach(result => {
            const resultDiv = document.createElement('div');
            resultDiv.className = 'search-result-item';
            resultDiv.innerHTML = `
                <div style="font-weight: bold; margin-bottom: 5px;">${result.type}</div>
                <div style="color: #666; font-size: 0.9em;">${result.preview}...</div>
            `;
            resultDiv.style.cssText = 'padding: 10px; margin: 5px 0; background: var(--ai-msg-bg); border-radius: 5px; cursor: pointer;';
            resultDiv.addEventListener('click', () => {
                result.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                result.element.style.backgroundColor = '#ffeb3b';
                setTimeout(() => {
                    result.element.style.backgroundColor = '';
                }, 2000);
            });
            searchResults.appendChild(resultDiv);
        });
    });

    // ===== KEYBOARD SHORTCUTS =====
    document.addEventListener('keydown', (e) => {
        // Ctrl+K: Quick prompts
        if (e.ctrlKey && e.key === 'k') {
            e.preventDefault();
            promptButton.click();
        }
        // Ctrl+/: Search
        if (e.ctrlKey && e.key === '/') {
            e.preventDefault();
            searchButton.click();
        }
        // Ctrl+N: New session
        if (e.ctrlKey && e.key === 'n') {
            e.preventDefault();
            document.getElementById('new-session')?.click();
        }
        // Esc: Close all panels
        if (e.key === 'Escape') {
            historyPanel.style.display = 'none';
            promptPanel.style.display = 'none';
            statsPanel.style.display = 'none';
            sessionsPanel.style.display = 'none';
            searchPanel.style.display = 'none';
            configPanel.style.display = 'none';
        }
    });

    // Config panel toggle
    configButton.addEventListener('click', () => {
        configPanel.style.display = configPanel.style.display === 'none' ? 'block' : 'none';
    });

    // Cancel config
    cancelConfigButton.addEventListener('click', () => {
        configPanel.style.display = 'none';
        loadConfig();
    });

    // Save config
    saveConfigButton.addEventListener('click', async () => {
        const ollamaUrl = ollamaUrlInput.value.trim();
        const model = modelSelect.value;

        if (!ollamaUrl) {
            alert('請輸入 Ollama URL');
            return;
        }

        try {
            const response = await fetch('/api/config', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    'ollamaUrl': ollamaUrl,
                    'model': model
                })
            });

            const data = await response.json();

            if (data.status === 'success') {
                // Clear chat history when switching models
                const clearResponse = await fetch('/api/chat/clear', {
                    method: 'POST'
                });

                if (clearResponse.ok) {
                    chatHistory.innerHTML = '<div class="message system-message"><p>✅ 設定已更新！<br>已切換到模型：' + model + '<br>URL：' + ollamaUrl + '</p></div>';
                }

                alert('設定已保存並清除舊對話！');
                configPanel.style.display = 'none';
            } else {
                alert('設定失敗：' + data.message);
            }
        } catch (error) {
            console.error('Error saving config:', error);
            alert('保存設定時發生錯誤：' + error.message);
        }
    });

    // Load config from server
    async function loadConfig() {
        try {
            const response = await fetch('/api/config');
            const data = await response.json();
            ollamaUrlInput.value = data.ollamaUrl || 'http://localhost:11434';

            // Auto-fetch models when loading config
            if (data.ollamaUrl) {
                const modelsResponse = await fetch('/api/models?ollamaUrl=' + encodeURIComponent(data.ollamaUrl));
                const modelsData = await modelsResponse.json();

                if (modelsData.status === 'success' && modelsData.models && modelsData.models.length > 0) {
                    modelSelect.innerHTML = '';
                    modelsData.models.forEach(model => {
                        const option = document.createElement('option');
                        option.value = model;
                        option.textContent = model;
                        modelSelect.appendChild(option);
                    });

                    // Select current model if available
                    if (data.model) {
                        modelSelect.value = data.model;
                    }
                } else {
                    // Fallback to default option if fetch fails
                    modelSelect.innerHTML = '<option value="">無法連線到 Ollama 伺服器</option>';
                }
            }
        } catch (error) {
            console.error('Error loading config:', error);
            modelSelect.innerHTML = '<option value="">載入失敗，請手動抓取模型</option>';
        }
    }

    // Theme toggle
    themeToggle.addEventListener('click', () => {
        currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
        applyTheme(currentTheme);
        localStorage.setItem('theme', currentTheme);
    });

    function applyTheme(theme) {
        if (theme === 'light') {
            document.documentElement.style.setProperty('--background-color', '#f5f5f5');
            document.documentElement.style.setProperty('--chat-bg', '#ffffff');
            document.documentElement.style.setProperty('--text-color', '#000000');
            document.documentElement.style.setProperty('--user-msg-bg', '#007bff');
            document.documentElement.style.setProperty('--ai-msg-bg', '#e9ecef');
            themeToggle.textContent = '🌞';
        } else {
            document.documentElement.style.setProperty('--background-color', '#1e1e1e');
            document.documentElement.style.setProperty('--chat-bg', '#2d2d2d');
            document.documentElement.style.setProperty('--text-color', '#ffffff');
            document.documentElement.style.setProperty('--user-msg-bg', '#0056b3');
            document.documentElement.style.setProperty('--ai-msg-bg', '#444444');
            themeToggle.textContent = '🌙';
        }
    }

    // History panel toggle
    historyButton.addEventListener('click', () => {
        historyPanel.style.display = historyPanel.style.display === 'none' ? 'flex' : 'none';
        promptPanel.style.display = 'none';
        loadHistoryList();
    });

    closeHistory.addEventListener('click', () => {
        historyPanel.style.display = 'none';
    });

    // Prompt panel toggle
    promptButton.addEventListener('click', () => {
        promptPanel.style.display = promptPanel.style.display === 'none' ? 'flex' : 'none';
        historyPanel.style.display = 'none';
    });

    closePrompt.addEventListener('click', () => {
        promptPanel.style.display = 'none';
    });

    // Prompt cards click
    document.querySelectorAll('.prompt-card').forEach(card => {
        card.addEventListener('click', () => {
            const prompt = card.getAttribute('data-prompt');
            messageInput.value = prompt;
            messageInput.focus();
            promptPanel.style.display = 'none';
            autoResize(messageInput);
        });
    });

    // Save conversation
    saveConversation.addEventListener('click', () => {
        const messages = Array.from(chatHistory.querySelectorAll('.message:not(.system-message)'));
        if (messages.length === 0) {
            alert('沒有對話可以保存');
            return;
        }

        const conversation = {
            id: Date.now(),
            date: new Date().toLocaleString('zh-TW'),
            messages: messages.map(msg => ({
                type: msg.classList.contains('user-message') ? 'user' : 'ai',
                content: msg.textContent.replace('複製', '').replace('已複製!', '').trim()
            }))
        };

        const saved = JSON.parse(localStorage.getItem('conversations') || '[]');
        saved.unshift(conversation);
        localStorage.setItem('conversations', JSON.stringify(saved.slice(0, 20))); // Keep last 20

        alert('對話已保存！');
        loadHistoryList();
    });

    // Export conversation
    exportConversation.addEventListener('click', () => {
        const messages = Array.from(chatHistory.querySelectorAll('.message:not(.system-message)'));
        if (messages.length === 0) {
            alert('沒有對話可以匯出');
            return;
        }

        let content = `Ollama Chat 對話記錄\\n匯出時間：${new Date().toLocaleString('zh-TW')}\\n\\n`;
        content += '='.repeat(50) + '\\n\\n';

        messages.forEach(msg => {
            const type = msg.classList.contains('user-message') ? '👤 使用者' : '🤖 AI';
            const text = msg.textContent.replace('複製', '').replace('已複製!', '').trim();
            content += `${type}:\\n${text}\\n\\n${'='.repeat(50)}\\n\\n`;
        });

        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `chat_${Date.now()}.txt`;
        a.click();
        URL.revokeObjectURL(url);
    });

    // Load history list
    function loadHistoryList() {
        const saved = JSON.parse(localStorage.getItem('conversations') || '[]');
        const historyList = document.getElementById('history-list');

        if (saved.length === 0) {
            historyList.innerHTML = '<p class=\"empty-message\">尚無保存的對話記錄</p>';
            return;
        }

        historyList.innerHTML = saved.map((conv, index) => `
            <div class=\"history-item\" data-index=\"${index}\">
                <div class=\"history-header\">
                    <span class=\"history-date\">📅 ${conv.date}</span>
                    <button class=\"delete-history\" data-index=\"${index}\">🗑️</button>
                </div>
                <div class=\"history-preview\">${conv.messages[0].content.substring(0, 50)}...</div>
                <button class=\"load-history\" data-index=\"${index}\">載入對話</button>
            </div>
        `).join('');

        // Load conversation click
        historyList.querySelectorAll('.load-history').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const index = e.target.getAttribute('data-index');
                loadConversation(saved[index]);
                historyPanel.style.display = 'none';
            });
        });

        // Delete conversation click
        historyList.querySelectorAll('.delete-history').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const index = e.target.getAttribute('data-index');
                if (confirm('確定要刪除這個對話記錄嗎？')) {
                    saved.splice(index, 1);
                    localStorage.setItem('conversations', JSON.stringify(saved));
                    loadHistoryList();
                }
            });
        });
    }

    function loadConversation(conversation) {
        chatHistory.innerHTML = '<div class=\"message system-message\"><p>已載入歷史對話</p></div>';
        conversation.messages.forEach(msg => {
            appendMessage(msg.content, msg.type === 'user' ? 'user-message' : 'ai-message');
        });
    }

    // Textarea auto resize
    messageInput.addEventListener('input', () => autoResize(messageInput));

    function autoResize(textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px';
    }

    // Support Shift+Enter for new line
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event('submit'));
        }
    });

    // Stop generation
    stopButton.addEventListener('click', () => {
        if (currentController) {
            currentController.abort();
        }
        if (currentEventSource) {
            currentEventSource.close();
            currentEventSource = null;
        }
        isGenerating = false;
        stopButton.style.display = 'none';
        sendButton.disabled = false;
    });

    // Regenerate last response
    regenerateButton.addEventListener('click', () => {
        if (lastUserMessage) {
            // Remove last AI message
            const messages = chatHistory.querySelectorAll('.message');
            const lastMsg = messages[messages.length - 1];
            if (lastMsg && lastMsg.classList.contains('ai-message')) {
                lastMsg.remove();
            }

            // Resend last message
            sendMessage(lastUserMessage);
        }
    });

    async function sendMessage(message) {
        sendButton.disabled = true;
        regenerateButton.style.display = 'none';
        stopButton.style.display = 'inline-block';
        isGenerating = true;

        const startTime = Date.now();

        try {
            // Check if web search is enabled
            if (webSearchEnabled && !uploadedFile) {
                // Use web search + AI mode
                currentController = new AbortController();

                // Show searching indicator
                const searchingId = appendMessage('🌐 正在搜尋網路資料...', 'system-message', true);

                const response = await fetch('/api/chat/search', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams({
                        'message': message,
                        'searchResults': '5'
                    }),
                    signal: currentController.signal
                });

                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }

                const data = await response.json();
                const responseTime = Date.now() - startTime;

                const searchingMsg = document.getElementById(searchingId);
                if (searchingMsg) searchingMsg.remove();

                // Show search results with better formatting
                if (data.searchContext) {
                    const searchResultDiv = appendMessage(data.searchContext, 'system-message');
                    searchResultDiv.style.backgroundColor = 'rgba(33, 150, 243, 0.1)';
                    searchResultDiv.style.borderLeft = '4px solid #2196F3';
                    searchResultDiv.style.padding = '10px';
                }

                // Show thinking indicator
                const thinkingId = appendMessage('<span class="loading-dots">💭 分析資料中<span>.</span><span>.</span><span>.</span></span>', 'ai-message', true);

                // Delay to show the thinking process
                setTimeout(() => {
                    const thinkingMsg = document.getElementById(thinkingId);
                    if (thinkingMsg) thinkingMsg.remove();

                    appendMessage(data.response, 'ai-message');
                    regenerateButton.style.display = 'inline-block';
                    incrementStats(responseTime);

                    isGenerating = false;
                    stopButton.style.display = 'none';
                    sendButton.disabled = false;
                    messageInput.focus();
                }, 500);

            } else if (streamingEnabled) {
                // Use streaming mode
                const loadingId = appendMessage('<span class="loading-dots">思考中<span>.</span><span>.</span><span>.</span></span>', 'ai-message', true);

                let params = new URLSearchParams({ 'message': message });
                if (uploadedFile) {
                    params.append('image', previewImage.src);
                }

                currentEventSource = new EventSource('/api/chat/stream?' + params.toString());
                let fullResponse = '';
                let messageDiv = null;

                currentEventSource.onmessage = (event) => {
                    const chunk = event.data;
                    fullResponse += chunk;

                    if (!messageDiv) {
                        const loadingMsg = document.getElementById(loadingId);
                        if (loadingMsg) loadingMsg.remove();
                        messageDiv = document.createElement('div');
                        messageDiv.classList.add('message', 'ai-message');
                        chatHistory.appendChild(messageDiv);
                    }

                    // Render markdown
                    if (typeof marked !== 'undefined') {
                        const rawHtml = marked.parse(fullResponse);
                        messageDiv.innerHTML = DOMPurify.sanitize(rawHtml);
                    } else {
                        messageDiv.textContent = fullResponse;
                    }

                    chatHistory.scrollTop = chatHistory.scrollHeight;
                };

                currentEventSource.onerror = (error) => {
                    currentEventSource.close();
                    const responseTime = Date.now() - startTime;

                    if (messageDiv) {
                        addCopyButton(messageDiv);
                    }

                    regenerateButton.style.display = 'inline-block';

                    if (uploadedFile) {
                        uploadedFile = null;
                        fileInput.value = '';
                        filePreview.style.display = 'none';
                    }

                    incrementStats(responseTime);
                    isGenerating = false;
                    stopButton.style.display = 'none';
                    sendButton.disabled = false;
                    messageInput.focus();
                };

                currentEventSource.addEventListener('close', () => {
                    currentEventSource.close();
                });

            } else {
                // Use non-streaming mode (original)
                currentController = new AbortController();
                const loadingId = appendMessage('<span class="loading-dots">思考中<span>.</span><span>.</span><span>.</span></span>', 'ai-message', true);

                // Prepare request body
                const formData = new URLSearchParams({
                    'message': message
                });

                // Add image if uploaded
                if (uploadedFile) {
                    const base64Image = previewImage.src;
                    formData.append('image', base64Image);
                }

                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: formData,
                    signal: currentController.signal
                });

                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }

                const data = await response.json();
                const responseTime = Date.now() - startTime;

                const loadingMsg = document.getElementById(loadingId);
                if (loadingMsg) loadingMsg.remove();

                appendMessage(data.response, 'ai-message');
                regenerateButton.style.display = 'inline-block';

                // Clear uploaded file after successful send
                if (uploadedFile) {
                    uploadedFile = null;
                    fileInput.value = '';
                    filePreview.style.display = 'none';
                }

                // Update stats
                incrementStats(responseTime);
            }

        } catch (error) {
            if (error.name === 'AbortError') {
                appendMessage('生成已停止。', 'system-message');
            } else {
                console.error('Error:', error);
                appendMessage('抱歉，發生錯誤。', 'system-message');
            }
        } finally {
            if (!streamingEnabled) {
                isGenerating = false;
                stopButton.style.display = 'none';
                sendButton.disabled = false;
                messageInput.focus();
            }
        }
    }

    function addCopyButton(messageDiv) {
        const copyBtn = document.createElement('button');
        copyBtn.className = 'copy-button';
        copyBtn.textContent = '複製';
        copyBtn.addEventListener('click', () => {
            const textContent = messageDiv.textContent || messageDiv.innerText;
            navigator.clipboard.writeText(textContent).then(() => {
                copyBtn.textContent = '已複製!';
                setTimeout(() => copyBtn.textContent = '複製', 2000);
            });
        });
        messageDiv.appendChild(copyBtn);
    }

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = messageInput.value.trim();
        if (!message) return;

        lastUserMessage = message;

        // Add user message with image if exists
        if (uploadedFile) {
            const messageWithImage = message + '<br><img src="' + previewImage.src + '" style="max-width: 200px; max-height: 200px; border-radius: 8px; margin-top: 8px;">';
            appendMessage(messageWithImage, 'user-message', true);
        } else {
            appendMessage(message, 'user-message');
        }
        messageInput.value = '';
        autoResize(messageInput);

        sendMessage(message);
    });

    // Clear chat history
    clearButton.addEventListener('click', async () => {
        if (confirm('確定要清除所有聊天記錄嗎？')) {
            try {
                const response = await fetch('/api/chat/clear', {
                    method: 'POST'
                });
                if (response.ok) {
                    chatHistory.innerHTML = '<div class="message system-message"><p>聊天記錄已清除，開始新的對話吧！</p></div>';
                }
            } catch (error) {
                console.error('Error clearing chat:', error);
                alert('清除失敗，請稍後再試。');
            }
        }
    });

    function appendMessage(text, className, isId = false) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('message', className);

        // Render markdown for AI and system messages
        if ((className === 'ai-message' || className === 'system-message') && typeof marked !== 'undefined' && !isId) {
            const rawHtml = marked.parse(text);
            messageDiv.innerHTML = DOMPurify.sanitize(rawHtml);
        } else if (isId) {
            messageDiv.innerHTML = text; // Loading message with HTML
        } else {
            messageDiv.textContent = text;
        }

        // Add copy button for AI and user messages
        if (!isId && className !== 'system-message') {
            const copyBtn = document.createElement('button');
            copyBtn.className = 'copy-button';
            copyBtn.textContent = '複製';
            copyBtn.addEventListener('click', () => {
                const textContent = messageDiv.textContent || messageDiv.innerText;
                navigator.clipboard.writeText(textContent).then(() => {
                    copyBtn.textContent = '已複製!';
                    setTimeout(() => copyBtn.textContent = '複製', 2000);
                });
            });
            messageDiv.appendChild(copyBtn);

            // Add TTS button for AI messages
            if (className === 'ai-message') {
                const ttsBtn = document.createElement('button');
                ttsBtn.className = 'tts-button';
                ttsBtn.textContent = '🔊';
                ttsBtn.addEventListener('click', () => speakText(text));
                messageDiv.appendChild(ttsBtn);
            }
        }

        const id = 'msg-' + Date.now();
        if (isId) messageDiv.id = id;

        chatHistory.appendChild(messageDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;

        // Auto speak if TTS enabled
        if (ttsEnabled && className === 'ai-message' && !isId) {
            speakText(text);
        }

        return isId ? id : messageDiv;
    }

    // ===== STATS FUNCTIONS =====
    function updateStats() {
        document.getElementById('total-messages').textContent = stats.messages;
        document.getElementById('total-sessions').textContent = stats.sessions;
        document.getElementById('avg-response-time').textContent =
            stats.messages > 0 ? Math.round(stats.totalTime / stats.messages) + 'ms' : '0ms';
        document.getElementById('total-tokens').textContent = '~' + stats.tokens;
    }

    function incrementStats(responseTime) {
        stats.messages++;
        stats.totalTime += responseTime;
        stats.tokens += Math.round(lastUserMessage.length / 4); // Rough estimate
        localStorage.setItem('stats', JSON.stringify(stats));
        updateStats();
    }

    statsButton.addEventListener('click', () => {
        statsPanel.style.display = statsPanel.style.display === 'none' ? 'flex' : 'none';
        closeAllPanelsExcept(statsPanel);
    });

    closeStats.addEventListener('click', () => {
        statsPanel.style.display = 'none';
    });

    document.getElementById('reset-stats').addEventListener('click', () => {
        if (confirm('確定要重置所有統計資料嗎？')) {
            stats = {messages: 0, sessions: 1, totalTime: 0, tokens: 0};
            localStorage.setItem('stats', JSON.stringify(stats));
            updateStats();
            alert('統計已重置！');
        }
    });

    // ===== VOICE FUNCTIONS =====
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        recognition = new SpeechRecognition();
        recognition.lang = 'zh-TW';
        recognition.continuous = false;
        recognition.interimResults = false;

        recognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript;
            messageInput.value = transcript;
            autoResize(messageInput);
            voiceInputBtn.textContent = '🎤';
        };

        recognition.onerror = (event) => {
            console.error('Speech recognition error:', event.error);
            voiceInputBtn.textContent = '🎤';
            alert('語音識別失敗：' + event.error);
        };

        recognition.onend = () => {
            voiceInputBtn.textContent = '🎤';
        };
    }

    voiceButton.addEventListener('click', () => {
        alert('語音功能:\\n\\n🎤 點擊輸入框旁的麥克風按鈕進行語音輸入\\n🔊 點擊AI回答旁的喇叭按鈕朗讀\\n🔇 點擊輸入區的喇叭圖標切換自動朗讀');
    });

    voiceInputBtn.addEventListener('click', () => {
        if (!recognition) {
            alert('您的瀏覽器不支援語音識別功能');
            return;
        }

        if (voiceInputBtn.textContent === '🎤') {
            recognition.start();
            voiceInputBtn.textContent = '⏺️';
        } else {
            recognition.stop();
            voiceInputBtn.textContent = '🎤';
        }
    });

    ttsToggle.addEventListener('click', () => {
        ttsEnabled = !ttsEnabled;
        ttsToggle.textContent = ttsEnabled ? '🔊' : '🔇';
        ttsToggle.setAttribute('data-enabled', ttsEnabled);
        localStorage.setItem('ttsEnabled', ttsEnabled);
    });

    function speakText(text) {
        if (!synthesis) return;

        synthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'zh-TW';
        utterance.rate = 1.0;
        utterance.pitch = 1.0;
        synthesis.speak(utterance);
    }

    // ===== FILE UPLOAD FUNCTIONS =====
    attachButton.addEventListener('click', () => {
        fileInput.click();
    });

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            uploadedFile = file;
            const reader = new FileReader();
            reader.onload = (event) => {
                previewImage.src = event.target.result;
                filePreview.style.display = 'flex';
            };
            reader.readAsDataURL(file);
        }
    });

    removeFile.addEventListener('click', () => {
        uploadedFile = null;
        fileInput.value = '';
        filePreview.style.display = 'none';
    });

    // ===== SESSION MANAGEMENT =====
    sessionsButton.addEventListener('click', () => {
        sessionsPanel.style.display = sessionsPanel.style.display === 'none' ? 'flex' : 'none';
        closeAllPanelsExcept(sessionsPanel);
        loadSessionsList();
    });

    closeSessions.addEventListener('click', () => {
        sessionsPanel.style.display = 'none';
    });

    document.getElementById('new-session').addEventListener('click', () => {
        const sessionId = 'session-' + Date.now();
        sessions[sessionId] = {
            id: sessionId,
            name: '新對話 ' + (Object.keys(sessions).length + 1),
            messages: [],
            created: new Date().toISOString(),
            pinned: false
        };
        localStorage.setItem('sessions', JSON.stringify(sessions));
        switchSession(sessionId);
        loadSessionsList();
        sessionsPanel.style.display = 'none';
    });

    function loadSessionsList() {
        const sessionsList = document.getElementById('sessions-list');
        const sortedSessions = Object.values(sessions).sort((a, b) => {
            if (a.pinned !== b.pinned) return b.pinned ? 1 : -1;
            return new Date(b.created) - new Date(a.created);
        });

        sessionsList.innerHTML = sortedSessions.map(session => `
            <div class=\"session-item ${session.id === currentSession ? 'active' : ''}\" data-id=\"${session.id}\">
                <div class=\"session-info\">
                    <div class=\"session-name\">${session.pinned ? '📌 ' : ''}${session.name}</div>
                    <div class=\"session-time\">${getTimeAgo(session.created)}</div>
                </div>
                <div class=\"session-actions\">
                    <button class=\"session-rename\" data-id=\"${session.id}\">✏️</button>
                    <button class=\"session-pin\" data-id=\"${session.id}\">${session.pinned ? '📌' : '📍'}</button>
                    ${session.id !== 'default' ? `<button class=\"session-delete\" data-id=\"${session.id}\">🗑️</button>` : ''}
                </div>
            </div>
        `).join('');

        // Add event listeners
        sessionsList.querySelectorAll('.session-item').forEach(item => {
            item.addEventListener('click', (e) => {
                if (!e.target.classList.contains('session-rename') &&
                    !e.target.classList.contains('session-pin') &&
                    !e.target.classList.contains('session-delete')) {
                    switchSession(item.getAttribute('data-id'));
                    sessionsPanel.style.display = 'none';
                }
            });
        });

        sessionsList.querySelectorAll('.session-rename').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sessionId = btn.getAttribute('data-id');
                const newName = prompt('輸入新名稱：', sessions[sessionId].name);
                if (newName) {
                    sessions[sessionId].name = newName;
                    localStorage.setItem('sessions', JSON.stringify(sessions));
                    loadSessionsList();
                }
            });
        });

        sessionsList.querySelectorAll('.session-pin').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sessionId = btn.getAttribute('data-id');
                sessions[sessionId].pinned = !sessions[sessionId].pinned;
                localStorage.setItem('sessions', JSON.stringify(sessions));
                loadSessionsList();
            });
        });

        sessionsList.querySelectorAll('.session-delete').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sessionId = btn.getAttribute('data-id');
                if (confirm('確定要刪除這個對話嗎？')) {
                    delete sessions[sessionId];
                    localStorage.setItem('sessions', JSON.stringify(sessions));
                    if (currentSession === sessionId) {
                        switchSession('default');
                    }
                    loadSessionsList();
                }
            });
        });
    }

    function switchSession(sessionId) {
        currentSession = sessionId;
        chatHistory.innerHTML = '<div class=\"message system-message\"><p>已切換到：' + sessions[sessionId].name + '</p></div>';
        // TODO: Load session messages
    }

    function getTimeAgo(dateString) {
        const now = new Date();
        const date = new Date(dateString);
        const seconds = Math.floor((now - date) / 1000);

        if (seconds < 60) return '剛剛';
        if (seconds < 3600) return Math.floor(seconds / 60) + '分鐘前';
        if (seconds < 86400) return Math.floor(seconds / 3600) + '小時前';
        return Math.floor(seconds / 86400) + '天前';
    }

    // ===== NETWORK STATUS =====
    function checkNetworkStatus() {
        if (navigator.onLine) {
            networkStatus.textContent = '🟢 已連線';
            networkStatus.style.color = '#28a745';
        } else {
            networkStatus.textContent = '🔴 離線';
            networkStatus.style.color = '#dc3545';
        }
    }

    window.addEventListener('online', checkNetworkStatus);
    window.addEventListener('offline', checkNetworkStatus);

    // ===== HELPER FUNCTIONS =====
    function closeAllPanelsExcept(keepOpen) {
        [historyPanel, promptPanel, statsPanel, sessionsPanel, configPanel].forEach(panel => {
            if (panel !== keepOpen) {
                panel.style.display = 'none';
            }
        });
    }

    // Initialize sessions if not exists
    if (!sessions.default) {
        sessions.default = {
            id: 'default',
            name: '預設對話',
            messages: [],
            created: new Date().toISOString(),
            pinned: false
        };
        localStorage.setItem('sessions', JSON.stringify(sessions));
    }

    // Load TTS preference
    ttsEnabled = localStorage.getItem('ttsEnabled') === 'true';
    ttsToggle.textContent = ttsEnabled ? '🔊' : '🔇';
});