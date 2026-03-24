package com.posystem.fx.controller;

import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.SaleDTO;
import com.posystem.fx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PosScreenController {

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

    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private List<ProductDTO> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        loadProducts();

        if (categoryCombo != null) {
            categoryCombo.setOnAction(e -> filterByCategory());
        }

        if (searchBar != null) {
            searchBar.textProperty().addListener((obs, oldVal, newVal) -> filterBySearch());
        }

        if (discountInput != null) {
            discountInput.textProperty().addListener((obs, oldVal, newVal) -> updateTotal());
        }
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        cartTable.setItems(cartItems);
    }

    private void loadCategories() {
        try {
            List<String> categories = new ArrayList<>();
            categories.add("All");
            categories.addAll(apiService.getAllCategories().stream()
                    .map(c -> c.getName()).toList());

            if (categoryCombo != null) {
                categoryCombo.setItems(FXCollections.observableArrayList(categories));
                categoryCombo.setValue("All");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        try {
            allProducts = apiService.getAllProducts();
            displayProducts(allProducts);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load products");
        }
    }

    private void displayProducts(List<ProductDTO> products) {
        if (productsContainer != null) {
            productsContainer.getChildren().clear();
            
            for (ProductDTO product : products) {
                // Create product card with image
                VBox productCard = new VBox(8);
                productCard.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-alignment: CENTER;");
                productCard.setPrefWidth(150);
                productCard.setAlignment(Pos.CENTER);

                // Display product image if available
                if (product.getImageData() != null && !product.getImageData().isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(product.getImageData());
                        Image image = new Image(new ByteArrayInputStream(imageBytes));
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(100);
                        imageView.setFitWidth(130);
                        imageView.setPreserveRatio(true);
                        productCard.getChildren().add(imageView);
                    } catch (Exception e) {
                        // If image fails to load, continue without it
                        e.printStackTrace();
                    }
                }

                // Product name and price label
                Label productLabel = new Label(product.getName() + "\nRs " + product.getPrice());
                productLabel.setWrapText(true);
                productLabel.setStyle("-fx-font-size: 11; -fx-text-alignment: CENTER;");
                productCard.getChildren().add(productLabel);

                // Add to cart button
                Button addBtn = new Button("Add to Cart");
                addBtn.setPrefWidth(130);
                addBtn.setStyle("-fx-font-size: 10;");
                addBtn.setOnAction(e -> addToCart(product));
                productCard.getChildren().add(addBtn);

                productsContainer.getChildren().add(productCard);
            }
        }
    }

    @FXML
    private void filterByCategory() {
        String selected = categoryCombo.getValue();
        List<ProductDTO> filtered;
        if ("All".equals(selected)) {
            filtered = allProducts;
        } else {
            filtered = allProducts.stream()
                    .filter(p -> p.getCategory().equals(selected))
                    .toList();
        }
        displayProducts(filtered);
    }

    @FXML
    private void filterBySearch() {
        String searchTerm = searchBar.getText().trim();
        List<ProductDTO> filtered = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .toList();
        displayProducts(filtered);
    }

    private void addToCart(ProductDTO product) {
        CartItem existing = cartItems.stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
            existing.setSubtotal(existing.getQuantity() * existing.getPrice());
        } else {
            cartItems.add(new CartItem(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    1,
                    product.getPrice(),
                    product.getPrice()
            ));
        }
        cartTable.refresh();
        updateTotal();
    }

    @FXML
    private void removeFromCart() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartItems.remove(selected);
            cartTable.refresh();
            updateTotal();
        }
    }

    @FXML
    private void updateQuantity() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getQuantity()));
            dialog.setTitle("Update Quantity");
            dialog.setHeaderText("Enter new quantity for " + selected.getProductName());
            dialog.showAndWait().ifPresent(newQty -> {
                try {
                    int qty = Integer.parseInt(newQty);
                    if (qty > 0) {
                        selected.setQuantity(qty);
                        selected.setSubtotal(qty * selected.getPrice());
                        cartTable.refresh();
                        updateTotal();
                    }
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number");
                }
            });
        }
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

    @FXML
    private void checkout() {
        if (cartItems.isEmpty()) {
            showError("Cart is empty!");
            return;
        }

        try {
            double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
            double discount = Double.parseDouble(discountInput.getText().isEmpty() ? "0" : discountInput.getText());
            double total = subtotal - discount;

            SaleDTO sale = new SaleDTO();
            sale.setItems(cartItems.stream().map(item -> 
                new SaleDTO.SaleItemDTO(item.getProductId(), item.getProductName(), 
                    item.getCategory(), item.getQuantity(), item.getPrice(), item.getSubtotal())
            ).toList());
            sale.setSubtotal(subtotal);
            sale.setDiscount(discount);
            sale.setFinalTotal(total);
            sale.setPaymentMethod("Cash");

            SaleDTO savedSale = apiService.recordSale(sale);

            if (savedSale != null) {
                showInfo("Sale completed successfully!\nSale ID: " + savedSale.getId());
                cartItems.clear();
                discountInput.clear();
                updateTotal();
            } else {
                showError("Failed to record sale");
            }
        } catch (Exception e) {
            showError("Error during checkout: " + e.getMessage());
            e.printStackTrace();
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

    // Inner class for cart items
    public static class CartItem {
        private String productId;
        private String productName;
        private String category;
        private int quantity;
        private double price;
        private double subtotal;

        public CartItem(String productId, String productName, String category, int quantity, double price, double subtotal) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.quantity = quantity;
            this.price = price;
            this.subtotal = subtotal;
        }

        // Getters and Setters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    }
}
