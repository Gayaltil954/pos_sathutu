package com.posystem.fx.controller;

import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.SaleDTO;
import com.posystem.fx.dto.AppStatusDTO;
import com.posystem.fx.service.ApiService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PosScreenController implements RefreshableView {

    private static final long MIN_REFRESH_INTERVAL_MS = 8_000;

    private final ApiService apiService;

    @FXML
    private TilePane productsContainer;

    @FXML
    private TableView<CartItem> cartTable;

    @FXML
    private TableColumn<CartItem, String> productNameColumn;

    @FXML
    private TableColumn<CartItem, Integer> quantityColumn;

    @FXML
    private TableColumn<CartItem, Double> priceColumn;

    @FXML
    private TableColumn<CartItem, Double> subtotalColumn;

    @FXML
    private Label totalLabel;

    @FXML
    private Label subtotalLabel;

    @FXML
    private TextField discountInput;

    @FXML
    private Label discountLabel;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private TextField searchBar;

    @FXML
    private Label lowStockAlertLabel;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private List<ProductDTO> allProducts = new ArrayList<>();
    private final Map<String, VBox> productCardCache = new HashMap<>();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(180));
    private int lowStockThreshold = 5;
    private boolean dataLoadStarted;
    private boolean initialDataLoaded;
    private volatile boolean pendingExternalRefresh;
    private volatile boolean pendingCategoryRefresh;
    private volatile boolean loadInProgress;
    private volatile boolean categoryLoadInProgress;
    private volatile long lastLoadedAtMs;

    @FXML
    public void initialize() {
        setupTable();
        setupSearchDebounce();
        apiService.registerProductsChangedListener(this::onProductsChangedExternally);
        apiService.registerCategoriesChangedListener(this::onCategoriesChangedExternally);
        dataLoadStarted = true;
        loadInitialDataAsync(false, false);

        if (categoryCombo != null) {
            categoryCombo.setOnAction(e -> applyFilters());
        }

        if (searchBar != null) {
            searchBar.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());
        }

        if (discountInput != null) {
            discountInput.textProperty().addListener((obs, oldVal, newVal) -> updateTotal());
        }
    }

    @Override
    public void onViewActivated() {
        if (pendingCategoryRefresh) {
            pendingCategoryRefresh = false;
            loadCategoriesAsync(true, true);
        }

        if (pendingExternalRefresh) {
            pendingExternalRefresh = false;
            loadInitialDataAsync(true, false);
            return;
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

    private void setupSearchDebounce() {
        searchDebounce.setOnFinished(event -> applyFilters());
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        cartTable.setItems(cartItems);
    }

    private void loadInitialDataAsync(boolean preserveCategorySelection, boolean forceRefresh) {
        if (loadInProgress) {
            return;
        }
        loadInProgress = true;

        String previousCategory = preserveCategorySelection && categoryCombo != null
                ? categoryCombo.getValue()
                : "All";

        CompletableFuture<List<ProductDTO>> productsFuture = CompletableFuture.supplyAsync(() ->
            apiService.getAllProducts(forceRefresh));
        CompletableFuture<List<String>> categoriesFuture = CompletableFuture.supplyAsync(() ->
            apiService.getAllCategories(forceRefresh).stream().map(c -> c.getName()).toList()
        );

        CompletableFuture.allOf(productsFuture, categoriesFuture)
                .thenRun(() -> Platform.runLater(() -> {
                    allProducts = productsFuture.join();
                    List<String> categories = categoriesFuture.join();

                    if (categoryCombo != null) {
                        List<String> categoryOptions = new ArrayList<>();
                        categoryOptions.add("All");
                        categoryOptions.addAll(categories);
                        categoryCombo.setItems(FXCollections.observableArrayList(categoryOptions));
                        if (previousCategory != null && categoryOptions.contains(previousCategory)) {
                            categoryCombo.setValue(previousCategory);
                        } else {
                            categoryCombo.setValue("All");
                        }
                    }

                        productCardCache.clear();
                    applyFilters();
                    updateLowStockAlert();
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
                                productCardCache.clear();
                                applyFilters();
                                updateLowStockAlert();
                            }));
                }))
                .exceptionally(ex -> {
                    loadInProgress = false;
                    Platform.runLater(() -> showError("Failed to load products/categories: " + ex.getMessage()));
                    return null;
                });
    }

    private boolean shouldRefreshData() {
        return System.currentTimeMillis() - lastLoadedAtMs >= MIN_REFRESH_INTERVAL_MS;
    }

    private void buildProductCardCache(List<ProductDTO> products) {
        productCardCache.clear();
        for (ProductDTO product : products) {
            productCardCache.put(product.getId(), createProductCard(product));
        }
    }

    private VBox createProductCard(ProductDTO product) {
        VBox productCard = new VBox(8);
        boolean lowStock = product.getStock() <= lowStockThreshold;
        String cardColor = lowStock ? "#f9e2e0" : "#ffffff";
        String borderColor = lowStock ? "#c0392b" : "#bdc3c7";
        productCard.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-padding: 10; -fx-alignment: CENTER; -fx-background-color: " + cardColor + ";");
        productCard.setPrefWidth(150);
        productCard.setAlignment(Pos.CENTER);

        if (product.getImageData() != null && !product.getImageData().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(product.getImageData());
                Image image = new Image(new ByteArrayInputStream(imageBytes), 130, 100, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(100);
                imageView.setFitWidth(130);
                imageView.setPreserveRatio(true);
                productCard.getChildren().add(imageView);
            } catch (Exception ignored) {
            }
        }

        String lowStockText = lowStock ? "\nLow Stock: " + product.getStock() : "\nStock: " + product.getStock();
        Label productLabel = new Label(product.getName() + "\nRs " + product.getPrice() + lowStockText);
        productLabel.setWrapText(true);
        productLabel.setStyle("-fx-font-size: 11; -fx-text-alignment: CENTER;");
        productCard.getChildren().add(productLabel);

        Button addBtn = new Button("Add to Cart");
        addBtn.setPrefWidth(130);
        addBtn.setStyle("-fx-font-size: 10;");
        addBtn.setOnAction(e -> addToCart(product));
        productCard.getChildren().add(addBtn);

        return productCard;
    }

    private void displayProducts(List<ProductDTO> products) {
        if (productsContainer != null) {
            productsContainer.getChildren().clear();

            for (ProductDTO product : products) {
                VBox productCard = productCardCache.get(product.getId());
                if (productCard == null) {
                    productCard = createProductCard(product);
                    productCardCache.put(product.getId(), productCard);
                }
                productsContainer.getChildren().add(productCard);
            }
        }
    }

    @FXML
    private void filterByCategory() {
        applyFilters();
    }

    @FXML
    private void filterBySearch() {
        applyFilters();
    }

    @FXML
    private void refreshPosData() {
        loadInitialDataAsync(true, true);
    }

    private void applyFilters() {
        String selectedCategory = categoryCombo != null ? categoryCombo.getValue() : "All";
        String searchTerm = searchBar != null ? searchBar.getText().trim().toLowerCase() : "";
        String normalizedSelectedCategory = normalizeCategory(selectedCategory);

        List<ProductDTO> filtered = allProducts.stream()
            .filter(p -> normalizedSelectedCategory == null
                || "all".equals(normalizedSelectedCategory)
                || normalizedSelectedCategory.equals(normalizeCategory(p.getCategory())))
                .filter(p -> searchTerm.isEmpty() || p.getName().toLowerCase().contains(searchTerm))
                .toList();

        displayProducts(filtered);
    }

    private void onProductsChangedExternally() {
        Platform.runLater(() -> {
            if (isViewAttached()) {
                loadInitialDataAsync(true, false);
            } else {
                pendingExternalRefresh = true;
            }
        });
    }

    private void onCategoriesChangedExternally() {
        Platform.runLater(() -> {
            if (isViewAttached()) {
                loadCategoriesAsync(true, true);
            } else {
                pendingCategoryRefresh = true;
            }
        });
    }

    private void loadCategoriesAsync(boolean preserveCategorySelection, boolean forceRefresh) {
        if (categoryLoadInProgress || categoryCombo == null) {
            return;
        }

        categoryLoadInProgress = true;
        String previousCategory = preserveCategorySelection ? categoryCombo.getValue() : "All";

        CompletableFuture
                .supplyAsync(() -> apiService.getAllCategories(forceRefresh).stream().map(c -> c.getName()).toList())
                .thenAccept(categories -> Platform.runLater(() -> {
                    List<String> categoryOptions = new ArrayList<>();
                    categoryOptions.add("All");
                    categoryOptions.addAll(categories);
                    categoryCombo.setItems(FXCollections.observableArrayList(categoryOptions));
                    if (previousCategory != null && categoryOptions.contains(previousCategory)) {
                        categoryCombo.setValue(previousCategory);
                    } else {
                        categoryCombo.setValue("All");
                    }
                    categoryLoadInProgress = false;
                    applyFilters();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> categoryLoadInProgress = false);
                    return null;
                });
    }

    private boolean isViewAttached() {
        return productsContainer != null && productsContainer.getParent() != null;
    }

    private String normalizeCategory(String category) {
        return category == null ? null : category.trim().toLowerCase();
    }

    private void addToCart(ProductDTO product) {
        CartItem existing = cartItems.stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int requestedQty = existing == null ? 1 : existing.getQuantity() + 1;
        if (requestedQty > product.getStock()) {
            showError("Cannot add more units. Available stock: " + product.getStock());
            return;
        }

        if (product.getStock() <= lowStockThreshold) {
            if (lowStockAlertLabel != null) {
                lowStockAlertLabel.setText("Low Stock: " + product.getName() + " has only " + product.getStock() + " units left.");
                lowStockAlertLabel.setVisible(true);
                lowStockAlertLabel.setManaged(true);
            }
        }

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
            existing.setSubtotal(existing.getQuantity() * existing.getPrice());
        } else {
            cartItems.add(new CartItem(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    1,
                    product.getBasePrice(),
                    product.getPrice(),
                    product.getPrice()
            ));
        }
        cartTable.refresh();
        updateTotal();
    }

    @FXML
    private void removeFromCart() {
        CartItem selected = resolveCartSelection();
        if (selected == null) {
            showError("Cart is empty. Add an item first.");
            return;
        }

        cartItems.remove(selected);
        cartTable.refresh();
        updateTotal();
    }

    @FXML
    private void updateQuantity() {
        CartItem selected = resolveCartSelection();
        if (selected == null) {
            showError("Cart is empty. Add an item first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getQuantity()));
        dialog.setTitle("Update Quantity");
        dialog.setHeaderText("Enter new quantity for " + selected.getProductName());
        dialog.showAndWait().ifPresent(newQty -> {
            try {
                int qty = Integer.parseInt(newQty);
                if (qty <= 0) {
                    showError("Quantity must be greater than zero");
                    return;
                }

                ProductDTO product = allProducts.stream()
                        .filter(p -> p.getId().equals(selected.getProductId()))
                        .findFirst()
                        .orElse(null);
                if (product != null && qty > product.getStock()) {
                    showError("Requested quantity exceeds available stock: " + product.getStock());
                    return;
                }

                selected.setQuantity(qty);
                selected.setSubtotal(qty * selected.getPrice());
                cartTable.refresh();
                updateTotal();
            } catch (NumberFormatException e) {
                showError("Please enter a valid number");
            }
        });
    }

    @FXML
    private void updatePrice() {
        CartItem selected = resolveCartSelection();
        if (selected == null) {
            showError("Cart is empty. Add an item first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getPrice()));
        dialog.setTitle("Update Price");
        dialog.setHeaderText("Enter new selling price for " + selected.getProductName());
        dialog.showAndWait().ifPresent(newPrice -> {
            try {
                double price = Double.parseDouble(newPrice);
                if (price < 0) {
                    showError("Price cannot be negative");
                    return;
                }
                selected.setPrice(price);
                selected.setSubtotal(selected.getQuantity() * selected.getPrice());
                cartTable.refresh();
                updateTotal();
            } catch (NumberFormatException e) {
                showError("Please enter a valid price");
            }
        });
    }

    private void updateTotal() {
        double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        double discount = 0;

        try {
            String discountStr = discountInput.getText().trim();
            if (!discountStr.isEmpty()) {
                discount = Double.parseDouble(discountStr);
            }
        } catch (NumberFormatException e) {
            discount = 0;
        }

        double total = subtotal - discount;

        if (subtotalLabel != null) subtotalLabel.setText("Rs " + String.format("%.2f", subtotal));
        if (discountLabel != null) discountLabel.setText("Rs " + String.format("%.2f", discount));
        if (totalLabel != null) totalLabel.setText("Rs " + String.format("%.2f", total));
    }

    private CartItem resolveCartSelection() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected;
        }

        if (cartItems.isEmpty()) {
            return null;
        }

        int lastIndex = cartItems.size() - 1;
        cartTable.getSelectionModel().select(lastIndex);
        return cartItems.get(lastIndex);
    }

    @FXML
    private void checkout() {
        if (cartItems.isEmpty()) {
            showError("Cart is empty!");
            return;
        }

        try {
            double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
            String discountText = discountInput.getText() == null ? "" : discountInput.getText().trim();
            double discount = Double.parseDouble(discountText.isEmpty() ? "0" : discountText);
            double total = subtotal - discount;

            SaleDTO sale = new SaleDTO();
            sale.setItems(cartItems.stream().map(item -> 
                new SaleDTO.SaleItemDTO(item.getProductId(), item.getProductName(), 
                    item.getCategory(), item.getQuantity(), item.getPrice(), item.getBasePrice(), item.getSubtotal())
            ).toList());
            sale.setSubtotal(subtotal);
            sale.setDiscount(discount);
            sale.setFinalTotal(total);
            sale.setPaymentMethod("Cash");

            CompletableFuture
                    .supplyAsync(() -> apiService.recordSale(sale))
                    .thenAccept(savedSale -> Platform.runLater(() -> {
                        if (savedSale != null) {
                            showInfo("Sale completed successfully!\nSale ID: " + savedSale.getId());
                            cartItems.clear();
                            discountInput.clear();
                            updateTotal();
                            applySoldItemsToLocalInventory(savedSale.getItems());
                        } else {
                            showError("Failed to record sale");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError("Error during checkout: " + ex.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            showError("Error during checkout: " + e.getMessage());
        }
    }

    @FXML
    private void clearCart() {
        cartItems.clear();
        discountInput.clear();
        updateTotal();
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

    private void updateLowStockAlert() {
        long count = allProducts.stream().filter(product -> product.getStock() <= lowStockThreshold).count();
        if (count > 0) {
            lowStockAlertLabel.setText("Low Stock Alert: " + count + " product(s) at or below " + lowStockThreshold);
            lowStockAlertLabel.setVisible(true);
            lowStockAlertLabel.setManaged(true);
        } else {
            lowStockAlertLabel.setVisible(false);
            lowStockAlertLabel.setManaged(false);
        }
    }

    private void applySoldItemsToLocalInventory(List<SaleDTO.SaleItemDTO> soldItems) {
        if (soldItems == null || soldItems.isEmpty()) {
            return;
        }

        for (SaleDTO.SaleItemDTO soldItem : soldItems) {
            ProductDTO product = allProducts.stream()
                    .filter(item -> item.getId().equals(soldItem.getProductId()))
                    .findFirst()
                    .orElse(null);
            if (product != null) {
                product.setStock(Math.max(0, product.getStock() - soldItem.getQuantity()));
            }
        }

        productCardCache.clear();
        applyFilters();
        updateLowStockAlert();
    }

    // Inner class for cart items
    public static class CartItem {
        private String productId;
        private String productName;
        private String category;
        private int quantity;
        private double basePrice;
        private double price;
        private double subtotal;

        public CartItem(String productId, String productName, String category, int quantity, double basePrice, double price, double subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.quantity = quantity;
            this.basePrice = basePrice;
            this.price = price;
            this.subtotal = subtotal;
        }
        // Getters and Setters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getBasePrice() { return basePrice; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    }
}
