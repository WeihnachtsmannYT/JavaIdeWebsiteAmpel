package com.example.ampel;

import org.springframework.web.bind.annotation.*;

import static com.example.ampel.CodeExecutionService.executeJavaCode;

@RestController
@RequestMapping("/api")
public class MainController {

    @PostMapping("/run")
    public String runCode(@RequestBody CodeRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId != null) {
            System.out.println("🔄 Running code for session: " + sessionId);

            try {
                return executeJavaCode(request.getCode(), sessionId);
            } catch (Exception e) {
                e.printStackTrace();
                return "Error executing code: " + e.getMessage();
            }
        }
        return "Error no session value send!";
    }

    @PostMapping("/stop")
    public String stopSimulation(@RequestBody CodeRequest request) {
        String sessionId = request.getSessionId();
        System.out.println("⏹️ Stopping execution for session: " + sessionId);

        //CodeExecutionService.stopExecution(sessionId);
        return "Execution stopped!";
    }
}
