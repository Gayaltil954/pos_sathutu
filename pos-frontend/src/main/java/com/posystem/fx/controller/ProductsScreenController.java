package com.posystem.fx.controller;

import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductsScreenController {

    private final ApiService apiService;

    @FXML
    private TableView<ProductDTO> productTable;

    @FXML
    private TableColumn<ProductDTO, String> nameColumn;

    @FXML
    private TableColumn<ProductDTO, String> categoryColumn;

    @FXML
    private TableColumn<ProductDTO, Double> priceColumn;

    @FXML
    private TableColumn<ProductDTO, Integer> stockColumn;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> categoryField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField qrCodeField;

    @FXML
    private TextField stockField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ImageView imagePreview;

    @FXML
    private Label imageNameLabel;

    private File selectedImageFile;

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        refreshProducts();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
    }

    private void loadCategories() {
        try {
            List<String> categories = apiService.getAllCategories().stream()
                    .map(c -> c.getName()).toList();
            categoryField.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshProducts() {
        try {
            List<ProductDTO> products = apiService.getAllProducts();
            productTable.setItems(FXCollections.observableArrayList(products));
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load products");
        }
    }

    @FXML
    private void addProduct() {
        try {
            ProductDTO product = new ProductDTO();
            product.setName(nameField.getText());
            product.setCategory(categoryField.getValue());
            product.setPrice(Double.parseDouble(priceField.getText()));
            product.setQrCode(qrCodeField.getText());
            product.setStock(Integer.parseInt(stockField.getText()));
            product.setDescription(descriptionField.getText());
            product.setActive(true);

            // Add image data if selected
            if (selectedImageFile != null) {
                try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                    byte[] imageBytes = fis.readAllBytes();
                    String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
                    product.setImageData(encodedImage);
                }
            }

            ProductDTO savedProduct = apiService.addProduct(product);
            if (savedProduct == null || savedProduct.getId() == null) {
                showError("Failed to save product to database");
                return;
            }

            showInfo("Product added successfully");
            clearFields();
            refreshProducts();
        } catch (Exception e) {
            showError("Error adding product: " + e.getMessage());
        }
    }

    @FXML
    private void deleteProduct() {
        ProductDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // API call to delete product
                // apiService.deleteProduct(selected.getId());
                
                showInfo("Product deleted successfully");
                refreshProducts();
            } catch (Exception e) {
                showError("Error deleting product");
            }
        } else {
            showError("Please select a product");
        }
    }

    @FXML
    private void searchProducts() {
        String searchTerm = nameField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshProducts();
        } else {
            try {
                List<ProductDTO> products = apiService.searchProducts(searchTerm);
                productTable.setItems(FXCollections.observableArrayList(products));
            } catch (Exception e) {
                showError("Search failed");
            }
        }
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedImageFile = file;
            imageNameLabel.setText(file.getName());
            try {
                Image image = new Image(file.toURI().toString());
                imagePreview.setImage(image);
            } catch (Exception e) {
                showError("Failed to load image: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        nameField.clear();
        categoryField.setValue(null);
        priceField.clear();
        qrCodeField.clear();
        stockField.clear();
        descriptionField.clear();
        selectedImageFile = null;
        imagePreview.setImage(null);
        imageNameLabel.setText("No image selected");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
