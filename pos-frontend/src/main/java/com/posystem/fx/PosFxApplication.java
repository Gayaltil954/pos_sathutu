package com.posystem.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

@SpringBootApplication
public class PosFxApplication extends Application {

    private static ConfigurableApplicationContext springContext;
    private static ConfigurableApplicationContext backendContext;
    private static boolean backendStartedByApp;
    private Parent root;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            ensureBackendAvailable();
        } catch (RuntimeException exception) {
            showStartupErrorAndExit(exception);
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        loader.setControllerFactory(springContext::getBean);
        root = loader.load();

        Scene scene = new Scene(root, 1400, 800);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("POS System - GAYAL MOBILE SHOP");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (springContext != null) {
                springContext.close();
            }
            stopBackendIfStartedByApp();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        try {
            springContext = SpringApplication.run(PosFxApplication.class, args);
            launch();
        } catch (Throwable throwable) {
            writeFatalError(throwable);
            JOptionPane.showMessageDialog(
                    null,
                    "POS failed to start. Check log:\n" + getLauncherFatalLogPath(),
                    "POS Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (springContext != null) {
            springContext.close();
        }
        stopBackendIfStartedByApp();
    }

    private static void ensureBackendAvailable() {
        if (isBackendReachable()) {
            return;
        }

        startBackendInProcess();
        waitForBackendStartup();
    }

    private static boolean isBackendReachable() {
        String baseUrl = System.getProperty("api.base-url", "http://localhost:8080/api");
        String healthUrl = baseUrl + "/categories";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 500;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void startBackendInProcess() {
        if (backendContext != null && backendContext.isActive()) {
            return;
        }

        try {
            Class<?> backendMainClass = Class.forName("com.posystem.PosApplication");
            backendContext = SpringApplication.run(backendMainClass);
            backendStartedByApp = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start embedded backend context.", exception);
        }
    }

    private static void waitForBackendStartup() {
        int maxAttempts = 40;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (isBackendReachable()) {
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for backend startup.", interruptedException);
            }
        }

        throw new IllegalStateException("Backend did not become available on http://localhost:8080 in time.");
    }

    private static Path getLauncherLogPath() {
        try {
            Path logDirectory = Paths.get(System.getProperty("user.home"), ".pos-system");
            Files.createDirectories(logDirectory);
            Path logPath = logDirectory.resolve("backend.log");
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
            return logPath;
        } catch (IOException exception) {
            return null;
        }
    }

    private static Path getLauncherFatalLogPath() {
        return Paths.get(System.getProperty("user.home"), ".pos-system", "launcher-error.log");
    }

    private static void writeFatalError(Throwable throwable) {
        try {
            Path logPath = getLauncherFatalLogPath();
            Files.createDirectories(logPath.getParent());
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            String message = "\n[FATAL STARTUP ERROR]\n" + stringWriter + System.lineSeparator();
            Files.writeString(logPath, message, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static void showStartupErrorAndExit(RuntimeException exception) {
        String details = exception.getMessage();
        Path logPath = getLauncherLogPath();

        if (logPath != null) {
            try {
                Files.writeString(
                        logPath,
                        "\n[STARTUP ERROR] " + details + System.lineSeparator(),
                        StandardOpenOption.APPEND
                );
            } catch (IOException ignored) {
            }
            details = details + "\n\nLog file: " + logPath;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("POS Startup Error");
        alert.setHeaderText("Failed to start POS application");
        alert.setContentText(details + "\n\nCheck MongoDB/Internet connection and try again.");
        alert.showAndWait();

        stopBackendIfStartedByApp();
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }

    private static void stopBackendIfStartedByApp() {
        if (!backendStartedByApp || backendContext == null) {
            return;
        }

        if (backendContext.isActive()) {
            backendContext.close();
        }
    }
}
