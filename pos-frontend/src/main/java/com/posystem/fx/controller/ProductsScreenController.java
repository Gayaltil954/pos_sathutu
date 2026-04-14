package com.posystem.fx.controller;

import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.AppStatusDTO;
import com.posystem.fx.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ProductsScreenController implements RefreshableView {

    private static final long MIN_REFRESH_INTERVAL_MS = 8_000;

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
    private TableColumn<ProductDTO, Double> basePriceColumn;

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
    private TextField basePriceField;

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

    @FXML
    private Label lowStockSummaryLabel;

    private File selectedImageFile;
    private String editingProductId;
    private String existingImageData;
    private int lowStockThreshold = 5;
    private boolean dataLoadStarted;
    private boolean initialDataLoaded;
    private volatile boolean pendingCategoryRefresh;
    private volatile boolean loadInProgress;
    private volatile boolean categoryLoadInProgress;
    private volatile long lastLoadedAtMs;

    @FXML
    public void initialize() {
        setupTable();
        apiService.registerCategoriesChangedListener(this::onCategoriesChangedExternally);
        dataLoadStarted = true;
        loadInitialDataAsync(false, false);
    }

    @Override
    public void onViewActivated() {
        if (pendingCategoryRefresh) {
            pendingCategoryRefresh = false;
            refreshCategoryOptionsAsync(true, true);
        }

        if (!initialDataLoaded && !dataLoadStarted) {
            dataLoadStarted = true;
            loadInitialDataAsync(false, true);
            return;
        }

        if (initialDataLoaded && shouldRefreshData()) {
            loadInitialDataAsync(true, true);
        }
    }

    private void loadInitialDataAsync(boolean preserveCategorySelection, boolean forceRefresh) {
        if (loadInProgress) {
            return;
        }
        loadInProgress = true;

        String previousCategory = preserveCategorySelection && categoryField != null
                ? categoryField.getValue()
                : null;

        CompletableFuture<List<String>> categoriesFuture = CompletableFuture.supplyAsync(() ->
                apiService.getAllCategories(forceRefresh).stream().map(c -> c.getName()).toList()
        );

        CompletableFuture<List<ProductDTO>> productsFuture = CompletableFuture.supplyAsync(() ->
                apiService.getAllProducts(forceRefresh));

        CompletableFuture.allOf(categoriesFuture, productsFuture)
                .thenRun(() -> Platform.runLater(() -> {
                    List<String> categories = categoriesFuture.join();
                    categoryField.setItems(FXCollections.observableArrayList(categories));
                    if (previousCategory != null && categories.contains(previousCategory)) {
                        categoryField.setValue(previousCategory);
                    }
                    List<ProductDTO> products = productsFuture.join();
                    productTable.setItems(FXCollections.observableArrayList(products));
                    updateLowStockSummary(products);
                    productTable.refresh();
                    initialDataLoaded = true;
                    lastLoadedAtMs = System.currentTimeMillis();
                    loadInProgress = false;

                    CompletableFuture
                            .supplyAsync(apiService::getSystemStatus)
                            .thenAccept(status -> Platform.runLater(() -> {
                                if (status == null) {
                                    return;
                                }
                                lowStockThreshold = status.getLowStockThreshold();
                                updateLowStockSummary(products);
                                productTable.refresh();
                            }));
                }))
                .exceptionally(ex -> {
                    loadInProgress = false;
                    Platform.runLater(() -> showError("Failed to load initial data: " + ex.getMessage()));
                    return null;
                });
    }

    private boolean shouldRefreshData() {
        return System.currentTimeMillis() - lastLoadedAtMs >= MIN_REFRESH_INTERVAL_MS;
    }

    private void onCategoriesChangedExternally() {
        Platform.runLater(() -> {
            if (isViewAttached()) {
                refreshCategoryOptionsAsync(true, true);
            } else {
                pendingCategoryRefresh = true;
            }
        });
    }

    private boolean isViewAttached() {
        return productTable != null && productTable.getParent() != null;
    }

    private void refreshCategoryOptionsAsync(boolean preserveCategorySelection, boolean forceRefresh) {
        if (categoryLoadInProgress || categoryField == null) {
            return;
        }

        categoryLoadInProgress = true;
        String previousCategory = preserveCategorySelection ? categoryField.getValue() : null;

        CompletableFuture
                .supplyAsync(() -> apiService.getAllCategories(forceRefresh).stream().map(c -> c.getName()).toList())
                .thenAccept(categories -> Platform.runLater(() -> {
                    categoryField.setItems(FXCollections.observableArrayList(categories));
                    if (previousCategory != null && categories.contains(previousCategory)) {
                        categoryField.setValue(previousCategory);
                    }
                    categoryLoadInProgress = false;
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> categoryLoadInProgress = false);
                    return null;
                });
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        basePriceColumn.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(item));
                if (item <= lowStockThreshold) {
                    setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        productTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(ProductDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (item.getStock() <= lowStockThreshold) {
                    setStyle("-fx-background-color: #fdecea;");
                } else {
                    setStyle("");
                }
            }
        });

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
        loadInitialDataAsync(true, true);
    }

    @FXML
    private void addProduct() {
        String name = nameField.getText();
        String category = categoryField.getValue();
        String priceText = priceField.getText();
        String basePriceText = basePriceField.getText();
        String qrCode = qrCodeField.getText();
        String stockText = stockField.getText();
        String description = descriptionField.getText();
        String productId = editingProductId;
        String imageData = existingImageData;
        File imageFile = selectedImageFile;

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ProductDTO product = new ProductDTO();
                        product.setName(name);
                        product.setCategory(category);
                        product.setPrice(Double.parseDouble(priceText));
                        product.setBasePrice(Double.parseDouble(basePriceText));
                        product.setQrCode(qrCode);
                        product.setStock(Integer.parseInt(stockText));
                        product.setDescription(description);
                        product.setActive(true);

                        if (imageFile != null) {
                            try (FileInputStream fis = new FileInputStream(imageFile)) {
                                byte[] imageBytes = fis.readAllBytes();
                                product.setImageData(Base64.getEncoder().encodeToString(imageBytes));
                            }
                        } else if (productId != null && imageData != null) {
                            product.setImageData(imageData);
                        }

                        return productId != null
                                ? apiService.updateProduct(productId, product)
                                : apiService.addProduct(product);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(savedProduct -> Platform.runLater(() -> {
                    if (savedProduct == null || savedProduct.getId() == null) {
                        showError(productId != null ? "Failed to update product" : "Failed to save product to database");
                        return;
                    }

                    upsertProductInTable(savedProduct);
                    clearFields();
                    showInfo(productId != null ? "Product updated successfully" : "Product added successfully");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Error saving product: " + ex.getMessage()));
                    return null;
                });
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

        CompletableFuture
                .supplyAsync(() -> apiService.deleteProduct(product.getId()))
                .thenAccept(deleted -> Platform.runLater(() -> {
                    if (!deleted) {
                        showError("Failed to delete product");
                        return;
                    }

                    removeProductFromTable(product.getId());
                    if (product.getId().equals(editingProductId)) {
                        clearFields();
                    }
                    showInfo("Product deleted successfully");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Error deleting product: " + ex.getMessage()));
                    return null;
                });
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
        basePriceField.setText(String.valueOf(product.getBasePrice()));
        qrCodeField.setText(product.getQrCode());
        stockField.setText(String.valueOf(product.getStock()));
        descriptionField.setText(product.getDescription());

        selectedImageFile = null;
        if (existingImageData != null && !existingImageData.isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(existingImageData);
                imagePreview.setImage(new Image(new ByteArrayInputStream(imageBytes), 120, 120, true, true));
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
            CompletableFuture
                    .supplyAsync(() -> apiService.searchProducts(searchTerm))
                    .thenAccept(products -> Platform.runLater(() ->
                            productTable.setItems(FXCollections.observableArrayList(products))))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError("Search failed: " + ex.getMessage()));
                        return null;
                    });
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
        basePriceField.clear();
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

    private void updateLowStockSummary(List<ProductDTO> products) {
        long lowStockCount = products.stream().filter(p -> p.getStock() <= lowStockThreshold).count();
        if (lowStockCount > 0) {
            lowStockSummaryLabel.setText("Low Stock Alert: " + lowStockCount
                    + " product(s) are at or below threshold (" + lowStockThreshold + ")");
            lowStockSummaryLabel.setVisible(true);
            lowStockSummaryLabel.setManaged(true);
        } else {
            lowStockSummaryLabel.setVisible(false);
            lowStockSummaryLabel.setManaged(false);
        }
    }

    private void upsertProductInTable(ProductDTO savedProduct) {
        if (savedProduct == null || savedProduct.getId() == null || productTable == null) {
            return;
        }

        ObservableList<ProductDTO> items = productTable.getItems();
        if (items == null) {
            items = FXCollections.observableArrayList();
            productTable.setItems(items);
        }

        int existingIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            ProductDTO current = items.get(i);
            if (current != null && savedProduct.getId().equals(current.getId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            items.set(existingIndex, savedProduct);
        } else {
            items.add(savedProduct);
        }

        productTable.refresh();
        updateLowStockSummary(items);
    }

    private void removeProductFromTable(String productId) {
        if (productId == null || productId.isBlank() || productTable == null || productTable.getItems() == null) {
            return;
        }

        productTable.getItems().removeIf(item -> item != null && productId.equals(item.getId()));
        productTable.refresh();
        updateLowStockSummary(productTable.getItems());
    }
}
