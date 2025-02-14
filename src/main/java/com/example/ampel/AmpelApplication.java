package com.example.ampel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;

@SpringBootApplication
@Controller
public class AmpelApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmpelApplication.class, args);
    }
    @GetMapping("/")
    public String home() {
        return "index"; // Lädt index.html aus src/main/resources/templates/
    }
}
