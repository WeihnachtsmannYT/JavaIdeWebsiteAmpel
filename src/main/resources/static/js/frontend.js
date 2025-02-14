let socket;
let isRunning = false;
let isSessionReady = false;
let sessionId = localStorage.getItem("sessionId") || generateSessionId();
localStorage.setItem("sessionId", sessionId);
const routerHost = window.location.hostname;
const routerPort = window.location.port;

function generateSessionId() {
    return 'sess-' + Math.random().toString(36).substr(2, 9);
}

function connectWebSocket() {
    socket = new WebSocket(`ws://${routerHost}:${routerPort}/ws?sessionId=${encodeURIComponent(sessionId)}`);

    socket.onopen = () => {
        console.log("✅ WebSocket connected!");
        updateTerminal("✅ WebSocket connected!", "");
    };

    socket.onmessage = function(event) {
        try {
            try {
                const data = JSON.parse(event.data);
                console.log("🟡 Raw WebSocket message:", event.data);

                if (data.type === "session_relinked") {
                    isSessionReady = true;
                    console.log("🔗 Session confirmed:", data.sessionId);
                    updateTerminal("🔗 WebSocket reconnected!", "");
                    return;
                }

                if (data.index !== undefined) {
                    console.log("📥 Received WebSocket JSON:", data);
                    updateLight(data.index, `rgb(${data.r}, ${data.g}, ${data.b})`);
                }
            } catch (error) {
                console.error("❌ JSON parse error:", error);
                console.log("❌ Received raw message:", event.data);
            }
        }catch (error) {
            console.error("❌ Message handling error:", error);
        }
    };

    socket.onclose = () => {
        console.warn("❌ WebSocket closed! Reconnecting in 3s...");
        setTimeout(connectWebSocket, 3000);
    };

    socket.onerror = (error) => {
        console.error("❌ WebSocket error:", error);
    };
}

connectWebSocket();

function runCode() {
    if (isRunning){
        console.error("Wait for previous simulation to end!");
        updateTerminal("", "❌ Error: Wait for previous simulation to end");
        return;
    }

    isRunning = true;

    if (!isSessionReady) {
        console.error("Wait for session confirmation!");
        updateTerminal("", "❌ Error: Wait for WebSocket connection to initialize");
        isRunning = false;
        return;
    }
    fetch("/api/run", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId: sessionId, code: editor.getValue() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.output === ("✅ Code executed successfully!")) {
            console.log("✅ Simulation finished!");
            isRunning = false;
        }
        updateTerminal(data.output, data.error);
    })
    .catch(err => {
    updateTerminal("", "❌ Fehler: " + err)
    isRunning = false;
    });
}

function stopSimulation() {
    fetch("/api/stop", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId: sessionId })
    })
    .then(res => res.json())
    .then(data => {
        console.log(data.message);
        updateTerminal(data.message, "");
    })
    .catch(err => updateTerminal("", "❌ Fehler: " + err));
}

// WebSocket-Verbindung herstellen und Debugging aktivieren
socket.onopen = function() {
    console.log("✅ WebSocket verbunden!");
};

socket.onerror = function(error) {
    console.log("✅ WebSocket-Fehler: ", error);
};

// Initialisiere Monaco-Editor
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs' }});
require(["vs/editor/editor.main"], function() {
    // Fetch the content FIRST
    fetch("/value.java")
        .then(response => response.text())
        .then(code => {
            // Initialize Monaco AFTER fetch completes
            editor = monaco.editor.create(document.getElementById("editor"), {
                value: code,  // Use fetched code
                language: "java",
                theme: "vs-dark"
            });
        })
        .catch(error => {
            console.error("Failed to fetch initial code:", error);
            editor = monaco.editor.create(document.getElementById("editor"), {
                value: "// Default code if fetch fails\npublic class Main {\n    public static void main(String[] args) {}\n}",
                language: "java",
                theme: "vs-dark"
            });
        });
});

// Ampellicht mit Farbe aktualisieren
function updateLight(index, color) {
    let light = document.getElementById(`light${index}`);
    if (light) {
        console.log(`Ändere Licht ${index} zu ${color}`);
        updateTerminal(`Ändere Licht ${index} zu ${color}`,"");// Debug-Ausgabe
        light.style.backgroundColor = color;

        // splitt color to rgba to 4 values
        let [r, g, b] = color.match(/\d+/g).map(Number);
        // Convert to rgba
        color = `rgba(${r}, ${g}, ${b}, 0.7)`;

        // splitt color to rgba for compatibility with older browsers
        light.style.webkitBoxShadow = `0px 0px 15px 10px ${color}`;
        light.style.MozBoxShadow = `0px 0px 15px 10px ${color}`;
        light.style.boxShadow = `0px 0px 15px 10px ${color}`;
    } else {
        console.log(`Fehler: Kein Licht mit ID light${index} gefunden.`);
        updateTerminal("",`Fehler: Kein Licht mit ID light${index} gefunden.`);
    }
}

// Terminal für Fehler oder Erfolg aktualisieren
function updateTerminal(output, error) {
    const terminal = document.getElementById("terminal");
    const maxMessages = 50;

    // Create new message
    const message = document.createElement('div');
    message.className = 'terminal-message';

    if (error) {
        message.style.color = '#ff4444';
        message.innerHTML = `<span class="timestamp">${new Date().toLocaleTimeString()}</span> ${error}`;
    } else {
        message.style.color = '#44ff44';
        message.innerHTML = `<span class="timestamp">${new Date().toLocaleTimeString()}</span> ${output}`;
    }

    // Append and limit history
    terminal.appendChild(message);
    while(terminal.children.length > maxMessages) {
        terminal.removeChild(terminal.firstChild);
    }

    // Auto-scroll
    terminal.scrollTop = terminal.scrollHeight;
}