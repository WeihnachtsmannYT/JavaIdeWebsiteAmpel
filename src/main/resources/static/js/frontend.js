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
                    isRunning = false;
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
        if (data.output.startsWith("✅ Code executed successfully")||
        data.output.startsWith("⏳ Execution timed out after")) {
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
    isRunning = false;
    fetch("/api/stop", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId: sessionId })
    })
    .then(res => res.json())
    .then(data => {
        console.log(data.output);
        updateTerminal(data.output, data.error);
    })
    .catch(err => updateTerminal("", "❌ Fehler: " + err));
}

function resetSimulation() {
    isRunning = false;
    stopSimulation();

    // Setze alle Lichter auf Grau (RGB 125,125,125)
    for (let i = 0; i <= 2; i++) {
        updateLight(i,`rgb(125, 125, 125)`);
    }

    clearTerminal();
    updateTerminal("✅ Simulation resetted!", "");
}

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


    let color;
    if (error) {
        color = '#ff4444'; // Rot für Fehler
    } else if (output && output.includes("⏹️ Simulation stopped for you!")) {
        color = '#ffff44'; // Gelb für Stopp-Nachricht
    } else {
        color = '#44ff44'; // Grün für normale Nachrichten
    }

    message.style.color = color;

    if (error) {
        message.innerHTML = `<span class="timestamp">${new Date().toLocaleTimeString()}</span> ${error}`;
    } else {
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

function clearTerminal() {
    const terminal = document.getElementById("terminal");
    while(terminal.firstChild) {
        terminal.removeChild(terminal.firstChild);
    }
}