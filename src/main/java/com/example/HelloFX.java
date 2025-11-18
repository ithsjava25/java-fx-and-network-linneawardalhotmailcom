package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class HelloFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.load();  // Kan kasta undantag om filen saknas eller är oläslig
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Kunde inte läsa .env-filen. Kontrollera att den finns i projektets rotmapp: " + e.getMessage(),
                    e  // Inkludera original-undantaget för bättre felsökning
            );
        }

        String hostName = dotenv.get("HOST_NAME");
        if (hostName == null) {
            throw new IllegalStateException(
                    "Kunde inte läsa HOST_NAME från .env-filen. " +
                            "Kontrollera att variabeln är definierad i .env-filen!"
            );
        }

        System.out.println("Ansluter till ntfy på: " + hostName);  // Debug-logg

        HelloModel model = new HelloModel();
        ChatNetworkClient httpClient = new NtfyHttpClient();
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloFX.class.getResource("/com/example/hello-view.fxml")
        );
        fxmlLoader.setControllerFactory(c -> new HelloController(model, httpClient, hostName));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 640, 480);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/example/styles.css")).toExternalForm()
        );
        stage.setTitle("Chat App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}