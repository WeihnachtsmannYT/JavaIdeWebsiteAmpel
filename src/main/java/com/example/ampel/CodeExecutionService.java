package com.example.ampel;

import org.springframework.web.socket.WebSocketSession;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

public class CodeExecutionService {

    public static String executeJavaCode(String userCode, String sessionId) throws Exception {
        System.out.println("🔍 [DEBUG] Received request to execute Java code for session: " + sessionId);

        // ✅ Ensure session is valid
        AmpelSession session = AmpelSession.getSession(sessionId);
        if (session == null) {
            return "❌ ERROR: AmpelSession is NULL for session ID: " + sessionId;
        }

        WebSocketSession wsSession = session.getSocketSession();
        if (wsSession == null || !wsSession.isOpen()) {
            return "❌ ERROR: No active WebSocket connection for session: " + sessionId;
        }

        AmpelController ampel = session.getAmpelController();
        if (ampel == null) {
            return "❌ ERROR: AmpelController is NULL for session ID: " + sessionId;
        }

        // ✅ Modify user code to include `myAmpel`
        String modifiedCode = "import com.example.ampel.AmpelController;\n" +
                userCode.replaceFirst("\\{", "{\n    static AmpelController myAmpel;\n    public static void setAmpel(AmpelController ampel) { myAmpel = ampel; }");

        System.out.println("📜 [DEBUG] Modified Code:\n" + modifiedCode);

        // ✅ Ensure JavaCompiler is available
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return "❌ ERROR: No Java compiler found! Ensure you have a JDK installed.";
        }

        SimpleJavaFileObject file = new InMemoryJavaFileObject("Main", modifiedCode);
        JavaFileObject[] files = {file};

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MemoryJavaFileManager memFileManager = new MemoryJavaFileManager(fileManager);

        // ✅ Compile code
        JavaCompiler.CompilationTask task = compiler.getTask(null, memFileManager, diagnostics, null, null, Arrays.asList(files));

        if (!task.call()) {
            StringBuilder errors = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                errors.append("Zeile ").append(diagnostic.getLineNumber()).append(": ").append(diagnostic.getMessage(null)).append("\n");
            }
            return "❌ Compilation Error:\n" + errors;
        }

        // ✅ Load and execute compiled class
        MemoryClassLoader classLoader = new MemoryClassLoader(memFileManager.getClassBytes());
        Class<?> compiledClass = classLoader.loadClass("Main");

        try {
            Method setAmpelMethod = compiledClass.getDeclaredMethod("setAmpel", AmpelController.class);
            setAmpelMethod.invoke(null, ampel);
        } catch (NoSuchMethodException e) {
            return "❌ ERROR: Method 'setAmpel(AmpelController ampel)' is missing!";
        }

        try {
            Method mainMethod = compiledClass.getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (InvocationTargetException e) {
            return "💥 Runtime Error: " + e.getCause().toString();
        } catch (Exception e) {
            return "⚠️ Unexpected Error: " + e.getMessage();
        }

        return "✅ Code executed successfully!";
    }
}
