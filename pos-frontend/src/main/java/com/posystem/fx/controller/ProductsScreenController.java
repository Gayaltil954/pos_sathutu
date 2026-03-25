package com.posystem.fx.controller;

import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
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
    private TableColumn<ProductDTO, Void> actionColumn;

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

    @FXML
    private Button addProductButton;

    private File selectedImageFile;
    private String editingProductId;
    private String existingImageData;

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
        setupActionColumn();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actions = new HBox(8, editButton, deleteButton);

            {
                actions.setAlignment(Pos.CENTER);
                editButton.setOnAction(event -> {
                    ProductDTO product = getTableView().getItems().get(getIndex());
                    startEditProduct(product);
                });
                deleteButton.setOnAction(event -> {
                    ProductDTO product = getTableView().getItems().get(getIndex());
                    deleteProductById(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
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
            } else if (editingProductId != null && existingImageData != null) {
                product.setImageData(existingImageData);
            }

            ProductDTO savedProduct;
            if (editingProductId != null) {
                savedProduct = apiService.updateProduct(editingProductId, product);
                if (savedProduct == null || savedProduct.getId() == null) {
                    showError("Failed to update product");
                    return;
                }
                showInfo("Product updated successfully");
            } else {
                savedProduct = apiService.addProduct(product);
                if (savedProduct == null || savedProduct.getId() == null) {
                    showError("Failed to save product to database");
                    return;
                }
                showInfo("Product added successfully");
            }

            clearFields();
            refreshProducts();
        } catch (Exception e) {
            showError("Error saving product: " + e.getMessage());
        }
    }

    @FXML
    private void deleteProduct() {
        ProductDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product");
            return;
        }
        deleteProductById(selected);
    }

    private void deleteProductById(ProductDTO product) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Product");
        confirmAlert.setContentText("Are you sure you want to delete '" + product.getName() + "'?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            boolean deleted = apiService.deleteProduct(product.getId());
            if (!deleted) {
                showError("Failed to delete product");
                return;
            }

            showInfo("Product deleted successfully");
            if (product.getId().equals(editingProductId)) {
                clearFields();
            }
            refreshProducts();
        } catch (Exception e) {
            showError("Error deleting product: " + e.getMessage());
        }
    }

    private void startEditProduct(ProductDTO product) {
        if (product == null) {
            return;
        }

        editingProductId = product.getId();
        existingImageData = product.getImageData();

        nameField.setText(product.getName());
        categoryField.setValue(product.getCategory());
        priceField.setText(String.valueOf(product.getPrice()));
        qrCodeField.setText(product.getQrCode());
        stockField.setText(String.valueOf(product.getStock()));
        descriptionField.setText(product.getDescription());

        selectedImageFile = null;
        if (existingImageData != null && !existingImageData.isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(existingImageData);
                imagePreview.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                imageNameLabel.setText("Existing image");
            } catch (Exception ignored) {
                imagePreview.setImage(null);
                imageNameLabel.setText("No image selected");
            }
        } else {
            imagePreview.setImage(null);
            imageNameLabel.setText("No image selected");
        }

        addProductButton.setText("Update Product");
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
        editingProductId = null;
        existingImageData = null;
        imagePreview.setImage(null);
        imageNameLabel.setText("No image selected");
        addProductButton.setText("Add Product");
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
