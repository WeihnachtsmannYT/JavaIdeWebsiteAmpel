package com.example.ampel;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.example.ampel.CodeExecutionService.executeJavaCode;


@RestController
@RequestMapping("/api")
public class MainController {

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runCode(@RequestBody CodeRequest request) {
        String sessionId = request.getSessionId();
        Map<String, String> response = new HashMap<>();

        if (sessionId != null) {
            System.out.println("🔄 Running code for session: " + sessionId);

            try {
                String executedCode = executeJavaCode(request.getCode(), sessionId);
                System.out.println(executedCode);

                response.put("output", executedCode);
                response.put("error", "");

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                e.printStackTrace();
                response.put("output", "");
                response.put("error", "Error executing code: " + e.getMessage());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }

        response.put("output", "");
        response.put("error", "Error: no session value sent!");
        return ResponseEntity.badRequest().body(response);
    }


    @PostMapping("/stop")
    public Map<String, String> stopSimulation(@RequestBody CodeRequest request) {
        String sessionId = request.getSessionId();
        System.out.println("⏹️ Stopping execution for session: " + sessionId);

        CodeExecutionService.stopExecution(sessionId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Execution stopped for session: " + sessionId);

        return response;
    }
}
