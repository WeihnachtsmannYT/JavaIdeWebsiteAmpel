package com.example.ampel;

import org.springframework.web.socket.WebSocketSession;

import javax.tools.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodeExecutionService {
    private static final Map<String, Thread> runningSimulations = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 5000; // 5 Sekunden

    public static String executeJavaCode(String userCode, String sessionId) {
        System.out.println("🔍 [DEBUG] Received request to execute Java code for session: " + sessionId);

        // ✅ Sicherstellen, dass eine gültige Sitzung existiert
        AmpelSession session = AmpelSession.getSession(sessionId);
        if (session == null) return "❌ ERROR: AmpelSession is NULL for session ID: " + sessionId;

        WebSocketSession wsSession = session.getSocketSession();
        if (wsSession == null || !wsSession.isOpen())
            return "❌ ERROR: No active WebSocket connection for session: " + sessionId;

        AmpelController ampel = session.getAmpelController();
        if (ampel == null) return "❌ ERROR: AmpelController is NULL for session ID: " + sessionId;

        // ✅ Benutzer-Code anpassen
        String modifiedCode = "import com.example.ampel.AmpelController;\n" +
                userCode.replaceFirst("\\{", "{\n    static AmpelController myAmpel;\n    public static void setAmpel(AmpelController ampel) { myAmpel = ampel; }");

        System.out.println("📜 [DEBUG] Modified Code:\n" + modifiedCode);

        // ✅ Compiler prüfen
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) return "❌ ERROR: No Java compiler found! Ensure you have a JDK installed.";

        SimpleJavaFileObject file = new InMemoryJavaFileObject("Main", modifiedCode);
        JavaFileObject[] files = {file};

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MemoryJavaFileManager memFileManager = new MemoryJavaFileManager(fileManager);

        // ✅ Code kompilieren
        JavaCompiler.CompilationTask task = compiler.getTask(null, memFileManager, diagnostics, null, null, Arrays.asList(files));

        if (!task.call()) {
            StringBuilder errors = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                errors.append("Zeile ").append(diagnostic.getLineNumber()).append(": ").append(diagnostic.getMessage(null)).append("\n");
            }
            return "❌ Compilation Error:\n" + errors;
        }

        // ✅ Klasse laden
        MemoryClassLoader classLoader = new MemoryClassLoader(memFileManager.getClassBytes());
        Class<?> compiledClass;
        try {
            compiledClass = classLoader.loadClass("Main");
        } catch (Exception e) {
            return "❌ ERROR: Failed to load compiled class!";
        }

        try {
            Method setAmpelMethod = compiledClass.getDeclaredMethod("setAmpel", AmpelController.class);
            setAmpelMethod.invoke(null, ampel);
        } catch (NoSuchMethodException e) {
            return "❌ ERROR: Method 'setAmpel(AmpelController ampel)' is missing!";
        } catch (InvocationTargetException e) {
            System.out.println("💥 Runtime Error: " + e.getCause().toString());
        } catch (IllegalAccessException e) {
            System.out.println("⚠️ Unexpected Error: " + e.getMessage());
        }

        // ✅ Code-Ausführung mit Zeitmessung und Abbruch
        Thread executionThread = new Thread(() -> {
            try {
                Method mainMethod = compiledClass.getDeclaredMethod("main", String[].class);
                mainMethod.invoke(null, (Object) new String[]{});
            } catch (InvocationTargetException e) {
                System.out.println("💥 Runtime Error: " + e.getCause().toString());
            } catch (Exception e) {
                System.out.println("⚠️ Unexpected Error: " + e.getMessage());
            }
        });

        runningSimulations.put(sessionId, executionThread);
        long startTime = System.currentTimeMillis();
        executionThread.start();

        try {
            executionThread.join(TIMEOUT_MS);
        } catch (InterruptedException e) {
            return "❌ ERROR: Execution interrupted!";
        }

        long executionTime = System.currentTimeMillis() - startTime;
        runningSimulations.remove(sessionId);

        if (executionThread.isAlive()) {
            executionThread.interrupt();
            return "⏳ Execution timed out after " + executionTime + " ms!";
        }

        return "✅ Code executed successfully in " + executionTime + " ms!";
    }

    public static void stopExecution(String sessionId) {
        Thread runningThread = runningSimulations.get(sessionId);
        if (runningThread != null && runningThread.isAlive()) {
            runningThread.interrupt();
            runningSimulations.remove(sessionId);
            System.out.println("⏹️ Simulation stopped for session: " + sessionId);
        }
    }
}
