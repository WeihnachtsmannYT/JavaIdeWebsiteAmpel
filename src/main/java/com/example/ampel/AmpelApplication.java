package com.example.ampel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@Controller
public class AmpelApplication extends Application {

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load("http://localhost:8080/"); // Lade eine Website

        // JavaScript aktivieren
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setOnAlert(event -> {
            System.out.println("Alert: " + event.getData());
        });

        Scene scene = new Scene(webView, 1280, 800);
        stage.setScene(scene);
        stage.setTitle("Java Ampel by Linus");

        stage.setOnCloseRequest(e -> {
            Platform.exit(); // Beendet JavaFX
            System.exit(0);  // Beendet alle Threads
        });

        stage.show();
    }


    public static void main(String[] args) {
        SpringApplication.run(AmpelApplication.class, args);

        launch(args);

    }
    @GetMapping("/")
    public String home() {
        return "index"; // Lädt index.html aus src/main/resources/templates/
    }

}
