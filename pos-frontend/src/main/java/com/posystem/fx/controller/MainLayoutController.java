package com.posystem.fx.controller;

import com.posystem.fx.PosFxApplication;
import com.posystem.fx.dto.AppStatusDTO;
import com.posystem.fx.service.ApiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MainLayoutController {

    private final ApiService apiService;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button posButton;

    @FXML
    private Button productsButton;

    @FXML
    private Button categoriesButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button exitButton;

    @FXML
    private Label versionLabel;

    @FXML
    private Label dashboardLowStockAlertLabel;

    @FXML
    private Button updateButton;

    private final Map<String, ScreenEntry> screenCache = new HashMap<>();
    private Timeline statusPoller;
    private AppStatusDTO latestStatus;
    private String notifiedVersion;

    @FXML
    public void initialize() {
        posButton.setOnAction(e -> loadScreen("fxml/PosScreen.fxml"));
        productsButton.setOnAction(e -> loadScreen("fxml/ProductsScreen.fxml"));
        categoriesButton.setOnAction(e -> loadScreen("fxml/CategoriesScreen.fxml"));
        reportsButton.setOnAction(e -> loadScreen("fxml/ReportsScreen.fxml"));
        exitButton.setOnAction(e -> exitSystem());

        // Load POS screen by default
        loadScreen("fxml/PosScreen.fxml");
        refreshUpdateStatus();
        startStatusPolling();
    }

    @FXML
    private void refreshUpdateStatus() {
        CompletableFuture
                .supplyAsync(apiService::getSystemStatus)
                .thenAccept(status -> javafx.application.Platform.runLater(() -> {
                    if (status == null) {
                        return;
                    }

                    latestStatus = status;
                    versionLabel.setText("Version: " + status.getCurrentVersion());

                    int lowStockCount = status.getLowStockCount();
                    if (lowStockCount > 0) {
                        dashboardLowStockAlertLabel.setText("Low Stock Alert: " + lowStockCount
                                + " item(s) are at or below threshold (" + status.getLowStockThreshold() + ")");
                        dashboardLowStockAlertLabel.setVisible(true);
                        dashboardLowStockAlertLabel.setManaged(true);
                    } else {
                        dashboardLowStockAlertLabel.setVisible(false);
                        dashboardLowStockAlertLabel.setManaged(false);
                    }

                    boolean updateAvailable = status.isUpdateAvailable();
                    updateButton.setVisible(updateAvailable);
                    updateButton.setManaged(updateAvailable);
                    if (updateAvailable) {
                        updateButton.setText("Update " + status.getLatestVersion() + " Available");
                        if (notifiedVersion == null || !notifiedVersion.equals(status.getLatestVersion())) {
                            showUpdateDialog(status);
                            notifiedVersion = status.getLatestVersion();
                        }
                    }
                }))
                .exceptionally(ex -> null);
    }

    private void showUpdateDialog(AppStatusDTO status) {
        if (latestStatus == null || !status.isUpdateAvailable()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Application Update");
        alert.setHeaderText("New version available: " + status.getLatestVersion());
        alert.setContentText("Current version: " + status.getCurrentVersion()
                + "\n\nChangelog:\n" + status.getChangelog()
                + "\n\nUse your deployment/update flow, then restart the app.");
        alert.showAndWait();
    }

    private void startStatusPolling() {
        statusPoller = new Timeline(new KeyFrame(Duration.seconds(20), e -> refreshUpdateStatus()));
        statusPoller.setCycleCount(Timeline.INDEFINITE);
        statusPoller.play();
    }

    private void loadScreen(String fxmlPath) {
        try {
            ScreenEntry entry = screenCache.get(fxmlPath);
            if (entry == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
                loader.setControllerFactory(PosFxApplication.getSpringContext()::getBean);
                Parent screen = loader.load();
                Object controller = loader.getController();
                entry = new ScreenEntry(screen, controller);
                screenCache.put(fxmlPath, entry);
            }

            contentArea.getChildren().setAll(entry.screen());
            if (entry.controller() instanceof RefreshableView refreshableView) {
                refreshableView.onViewActivated();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showLoadError(fxmlPath, e);
        } catch (Exception e) {
            e.printStackTrace();
            showLoadError(fxmlPath, e);
        }
    }

    private void showLoadError(String fxmlPath, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Screen Load Error");
        alert.setHeaderText("Could not open screen: " + fxmlPath);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private void exitSystem() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Exit System");
        confirm.setHeaderText("Close POS System");
        confirm.setContentText("Are you sure you want to safely exit the system for the day?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
        if (statusPoller != null) {
            statusPoller.stop();
        }
        PosFxApplication.shutdownApplication();
        stage.close();
    }

    private record ScreenEntry(Parent screen, Object controller) {
    }
}
