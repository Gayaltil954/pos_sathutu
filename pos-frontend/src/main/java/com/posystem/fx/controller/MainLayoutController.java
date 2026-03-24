package com.posystem.fx.controller;

import com.posystem.fx.PosFxApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainLayoutController {

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
    public void initialize() {
        posButton.setOnAction(e -> loadScreen("fxml/PosScreen.fxml"));
        productsButton.setOnAction(e -> loadScreen("fxml/ProductsScreen.fxml"));
        categoriesButton.setOnAction(e -> loadScreen("fxml/CategoriesScreen.fxml"));
        reportsButton.setOnAction(e -> loadScreen("fxml/ReportsScreen.fxml"));

        // Load POS screen by default
        loadScreen("fxml/PosScreen.fxml");
    }

    private void loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
            loader.setControllerFactory(PosFxApplication.getSpringContext()::getBean);
            Parent screen = loader.load();
            contentArea.getChildren().setAll(screen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
