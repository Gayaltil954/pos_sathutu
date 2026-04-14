package com.posystem.fx.controller;

import com.posystem.fx.dto.DailySummaryDTO;
import com.posystem.fx.dto.MonthlySummaryDTO;
import com.posystem.fx.dto.OrderDetailDTO;
import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.SaleDTO;
import com.posystem.fx.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ReportsScreenController {

    private final ApiService apiService;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label dailyTotalDiscountLabel;

    @FXML
    private Label dailyNetTotalLabel;

    @FXML
    private Label dailyTotalActualPriceLabel;

    @FXML
    private Label dailyReportSubtotalLabel;

    @FXML
    private Label dailyTransactionsLabel;

    @FXML
    private Label revenueLabel;

    @FXML
    private Label monthlyTotalDiscountLabel;

    @FXML
    private Label monthlyNetTotalLabel;

    @FXML
    private Label monthlyTotalActualPriceLabel;

    @FXML
    private Label monthlyReportSubtotalLabel;

    @FXML
    private Label monthlyTransactionsLabel;

    @FXML
    private DatePicker datePickerDaily;

    @FXML
    private ComboBox<String> monthCombo;

    @FXML
    private TextArea dailyOrdersDetailsArea;

    @FXML
    private TextArea monthlyOrdersDetailsArea;

    @FXML
    private TextField editSaleIdField;

    @FXML
    private ComboBox<String> editProductPicker;

    @FXML
    private TableView<EditableSaleItem> editableSaleTable;

    @FXML
    private TableColumn<EditableSaleItem, String> editProductNameColumn;

    @FXML
    private TableColumn<EditableSaleItem, String> editCategoryColumn;

    @FXML
    private TableColumn<EditableSaleItem, Integer> editQuantityColumn;

    @FXML
    private TableColumn<EditableSaleItem, Double> editPriceColumn;

    @FXML
    private TableColumn<EditableSaleItem, Double> editLineTotalColumn;

    @FXML
    private Label editableSubtotalLabel;

    @FXML
    private TextField editableDiscountField;

    @FXML
    private Label editableNetTotalLabel;

    @FXML
    private Label editSaleStatusLabel;

    private final ObservableList<EditableSaleItem> editableItems = FXCollections.observableArrayList();
    private List<ProductDTO> availableProducts = new ArrayList<>();
    private boolean productOptionsLoaded;
    private boolean productOptionsLoading;
    private final List<Runnable> productOptionCallbacks = new ArrayList<>();
    private SaleDTO loadedSale;
    private LocalDate cachedDailySummaryDate;
    private DailySummaryDTO cachedDailySummary;
    private String cachedMonthlySummaryMonth;
    private MonthlySummaryDTO cachedMonthlySummary;
    private boolean dailySummaryLoadInProgress;
    private boolean monthlySummaryLoadInProgress;

    @FXML
    public void initialize() {
        if (datePickerDaily != null) {
            datePickerDaily.setValue(LocalDate.now());
        }

        List<String> months = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            months.add(currentMonth.minusMonths(i).toString());
        }

        if (monthCombo != null) {
            monthCombo.getItems().setAll(months);
            monthCombo.setValue(currentMonth.toString());
        }

        if (dailyOrdersDetailsArea != null) {
            dailyOrdersDetailsArea.setText("Select a date and click Load Summary.");
        }
        if (monthlyOrdersDetailsArea != null) {
            monthlyOrdersDetailsArea.setText("Select a month and click Load Summary.");
        }

        setupEditableSaleTable();
        if (editSaleIdField != null) {
            editSaleIdField.setOnAction(event -> loadSaleForEdit());
        }
        if (editProductPicker != null) {
            editProductPicker.setOnShowing(event -> loadProductOptions());
        }
        if (editableDiscountField != null) {
            editableDiscountField.textProperty().addListener((obs, oldValue, newValue) -> recalculateEditableTotals());
        }
    }

    private void setupEditableSaleTable() {
        if (editableSaleTable == null) {
            return;
        }
        editProductNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        editCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        editQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        editPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        editLineTotalColumn.setCellValueFactory(new PropertyValueFactory<>("itemTotal"));
        editableSaleTable.setItems(editableItems);
    }

    private void loadProductOptions() {
        loadProductOptions(null);
    }

    private void loadProductOptions(Runnable onLoaded) {
        if (productOptionsLoaded) {
            if (onLoaded != null) {
                onLoaded.run();
            }
            return;
        }

        if (onLoaded != null) {
            productOptionCallbacks.add(onLoaded);
        }

        if (productOptionsLoading) {
            return;
        }

        productOptionsLoading = true;
        editSaleStatusLabel.setText("Loading products for editing...");

        CompletableFuture
                .supplyAsync(apiService::getAllProducts)
                .thenAccept(products -> Platform.runLater(() -> {
                    availableProducts = products;
                    productOptionsLoaded = true;
                    productOptionsLoading = false;
                    List<String> productOptions = products.stream()
                            .map(p -> p.getId() + " | " + p.getName())
                            .toList();
                    editProductPicker.setItems(FXCollections.observableArrayList(productOptions));
                    for (Runnable callback : productOptionCallbacks) {
                        callback.run();
                    }
                    productOptionCallbacks.clear();
                    editSaleStatusLabel.setText("Products loaded. You can add items now.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        productOptionsLoading = false;
                        productOptionCallbacks.clear();
                        showError("Failed to load products for sale editing: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void loadLatestSales() {
        CompletableFuture
                .supplyAsync(apiService::getRecentSales)
                .thenAccept(sales -> Platform.runLater(() -> {
                    if (sales == null || sales.isEmpty()) {
                        showError("No sales available to edit");
                        return;
                    }

                    SaleDTO latest = sales.stream()
                            .filter(sale -> sale.getId() != null)
                            .findFirst()
                            .orElse(null);

                    if (latest == null) {
                        showError("No valid sale ID found in sales list");
                        return;
                    }

                    editSaleIdField.setText(latest.getId());
                    loadSaleForEdit();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Failed to load recent sales: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void loadSaleForEdit() {
        String saleId = editSaleIdField.getText() == null ? "" : editSaleIdField.getText().trim();
        if (saleId.isEmpty()) {
            showError("Please enter a sale ID");
            return;
        }

        editSaleStatusLabel.setText("Loading sale " + saleId + "...");

        CompletableFuture
                .supplyAsync(() -> apiService.getSaleById(saleId))
                .thenAccept(sale -> Platform.runLater(() -> {
                    if (sale == null) {
                        showError("Sale not found: " + saleId);
                        return;
                    }

                    loadedSale = sale;
                    editableItems.clear();
                    if (sale.getItems() != null) {
                        for (SaleDTO.SaleItemDTO item : sale.getItems()) {
                            editableItems.add(new EditableSaleItem(
                                    item.getProductId(),
                                    item.getProductName(),
                                    item.getCategory(),
                                    item.getQuantity(),
                                    item.getPrice(),
                                    item.getBasePrice()
                            ));
                        }
                    }

                    editableDiscountField.setText(String.valueOf(sale.getDiscount()));
                    recalculateEditableTotals();
                    editSaleStatusLabel.setText("Loaded sale " + saleId + ". You can now edit and save.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Failed to load sale: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void addItemToEditableSale() {
        loadProductOptions(this::addItemAfterProductsLoaded);
    }

    private void addItemAfterProductsLoaded() {
        if (!productOptionsLoaded) {
            editSaleStatusLabel.setText("Loading products for editing...");
            return;
        }

        String selected = editProductPicker.getValue();
        if (selected == null || selected.isBlank()) {
            showError("Please select a product to add");
            return;
        }

        String productId = selected.split("\\|")[0].trim();
        ProductDTO product = availableProducts.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (product == null) {
            showError("Selected product no longer exists");
            return;
        }

        EditableSaleItem existing = editableItems.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
            existing.recalculate();
        } else {
            editableItems.add(new EditableSaleItem(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    1,
                    product.getPrice(),
                    product.getBasePrice()
            ));
        }

        editableSaleTable.refresh();
        recalculateEditableTotals();
    }

    @FXML
    private void editSelectedItemQuantity() {
        EditableSaleItem selected = editableSaleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an item to edit quantity");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getQuantity()));
        dialog.setTitle("Edit Quantity");
        dialog.setHeaderText("Update quantity for " + selected.getProductName());
        dialog.showAndWait().ifPresent(value -> {
            try {
                int qty = Integer.parseInt(value);
                if (qty <= 0) {
                    showError("Quantity must be greater than zero");
                    return;
                }
                selected.setQuantity(qty);
                selected.recalculate();
                editableSaleTable.refresh();
                recalculateEditableTotals();
            } catch (NumberFormatException e) {
                showError("Please enter a valid quantity");
            }
        });
    }

    @FXML
    private void editSelectedItemPrice() {
        EditableSaleItem selected = editableSaleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an item to edit price");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getPrice()));
        dialog.setTitle("Edit Price");
        dialog.setHeaderText("Update price for " + selected.getProductName());
        dialog.showAndWait().ifPresent(value -> {
            try {
                double price = Double.parseDouble(value);
                if (price < 0) {
                    showError("Price cannot be negative");
                    return;
                }
                selected.setPrice(price);
                selected.recalculate();
                editableSaleTable.refresh();
                recalculateEditableTotals();
            } catch (NumberFormatException e) {
                showError("Please enter a valid price");
            }
        });
    }

    @FXML
    private void removeSelectedItem() {
        EditableSaleItem selected = editableSaleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an item to remove");
            return;
        }

        editableItems.remove(selected);
        recalculateEditableTotals();
    }

    @FXML
    private void saveEditedSale() {
        if (loadedSale == null || loadedSale.getId() == null) {
            showError("Load a sale before saving changes");
            return;
        }
        if (editableItems.isEmpty()) {
            showError("Sale must contain at least one item");
            return;
        }

        try {
            double discount = parseEditableDiscount();
            SaleDTO payload = new SaleDTO();
            payload.setId(loadedSale.getId());
            payload.setPaymentMethod(loadedSale.getPaymentMethod());
            payload.setNotes(loadedSale.getNotes());
            payload.setDiscount(discount);
            payload.setItems(editableItems.stream().map(item -> new SaleDTO.SaleItemDTO(
                    item.getProductId(),
                    item.getProductName(),
                    item.getCategory(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getBasePrice(),
                    item.getItemTotal()
            )).toList());
            payload.setSubtotal(editableItems.stream().mapToDouble(EditableSaleItem::getItemTotal).sum());
            payload.setFinalTotal(Math.max(0, payload.getSubtotal() - discount));

            CompletableFuture
                    .supplyAsync(() -> apiService.updateSale(loadedSale.getId(), payload))
                    .thenAccept(updatedSale -> Platform.runLater(() -> {
                        if (updatedSale == null) {
                            showError("Failed to update sale. Check stock constraints and try again.");
                            return;
                        }
                        loadedSale = updatedSale;
                        editSaleStatusLabel.setText("Sale " + updatedSale.getId() + " updated successfully.");
                        showInfo("Sale updated successfully. Inventory and totals were recalculated.");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError("Failed to update sale: " + ex.getMessage()));
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private double parseEditableDiscount() {
        String text = editableDiscountField.getText() == null ? "0" : editableDiscountField.getText().trim();
        if (text.isEmpty()) {
            return 0;
        }

        try {
            double value = Double.parseDouble(text);
            if (value < 0) {
                throw new IllegalArgumentException("Discount cannot be negative");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid discount amount");
        }
    }

    private void recalculateEditableTotals() {
        double subtotal = editableItems.stream().mapToDouble(EditableSaleItem::getItemTotal).sum();
        double discount;
        try {
            discount = parseEditableDiscount();
        } catch (IllegalArgumentException ignored) {
            discount = 0;
        }

        editableSubtotalLabel.setText(String.format("Rs %.2f", subtotal));
        editableNetTotalLabel.setText(String.format("Rs %.2f", Math.max(0, subtotal - discount)));
    }

    @FXML
    private void loadDailySummary() {
        LocalDate date = datePickerDaily.getValue();
        if (date == null) {
            showError("Please select a date");
            return;
        }

        if (date.equals(cachedDailySummaryDate) && cachedDailySummary != null) {
            applyDailySummary(cachedDailySummary);
            return;
        }

        if (dailySummaryLoadInProgress) {
            return;
        }
        dailySummaryLoadInProgress = true;
        dailyOrdersDetailsArea.setText("Loading daily summary...");

        CompletableFuture
                .supplyAsync(() -> apiService.getDailySummary(date.toString()))
                .thenAccept(summary -> Platform.runLater(() -> {
                    dailySummaryLoadInProgress = false;
                    if (summary == null) {
                        showError("No daily summary data found");
                        return;
                    }
                    cachedDailySummaryDate = date;
                    cachedDailySummary = summary;
                    applyDailySummary(summary);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        dailySummaryLoadInProgress = false;
                        showError("Error loading daily summary: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void loadMonthlySummary() {
        String month = monthCombo.getValue();
        if (month == null) {
            showError("Please select a month");
            return;
        }

        if (month.equals(cachedMonthlySummaryMonth) && cachedMonthlySummary != null) {
            applyMonthlySummary(cachedMonthlySummary);
            return;
        }

        if (monthlySummaryLoadInProgress) {
            return;
        }
        monthlySummaryLoadInProgress = true;
        monthlyOrdersDetailsArea.setText("Loading monthly summary...");

        CompletableFuture
                .supplyAsync(() -> apiService.getMonthlySummary(month))
                .thenAccept(summary -> Platform.runLater(() -> {
                    monthlySummaryLoadInProgress = false;
                    if (summary == null) {
                        showError("No monthly summary data found");
                        return;
                    }
                    cachedMonthlySummaryMonth = month;
                    cachedMonthlySummary = summary;
                    applyMonthlySummary(summary);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        monthlySummaryLoadInProgress = false;
                        showError("Error loading monthly summary: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void applyDailySummary(DailySummaryDTO summary) {
        totalSalesLabel.setText(String.format("Rs %.2f", summary.getTotalSales()));
        dailyTotalDiscountLabel.setText(String.format("Rs %.2f", summary.getTotalDiscount()));
        dailyNetTotalLabel.setText(String.format("Rs %.2f", summary.getNetTotal()));
        dailyTotalActualPriceLabel.setText(String.format("Rs %.2f", summary.getTotalActualPrice()));
        dailyReportSubtotalLabel.setText(String.format("Rs %.2f", summary.getReportSubtotal()));
        dailyTransactionsLabel.setText(String.valueOf(summary.getNumberOfTransactions()));
        dailyOrdersDetailsArea.setText(formatOrders(summary.getOrders()));
    }

    private void applyMonthlySummary(MonthlySummaryDTO summary) {
        revenueLabel.setText(String.format("Rs %.2f", summary.getTotalRevenue()));
        monthlyTotalDiscountLabel.setText(String.format("Rs %.2f", summary.getTotalDiscount()));
        monthlyNetTotalLabel.setText(String.format("Rs %.2f", summary.getNetTotal()));
        monthlyTotalActualPriceLabel.setText(String.format("Rs %.2f", summary.getTotalActualPrice()));
        monthlyReportSubtotalLabel.setText(String.format("Rs %.2f", summary.getReportSubtotal()));
        monthlyTransactionsLabel.setText(String.valueOf(summary.getTotalTransactions()));
        monthlyOrdersDetailsArea.setText(formatOrders(summary.getOrders()));
    }

    @FXML
    private void downloadDailyPdf() {
        downloadDailyReport("pdf");
    }

    @FXML
    private void downloadDailyExcel() {
        downloadDailyReport("xlsx");
    }

    @FXML
    private void downloadMonthlyPdf() {
        downloadMonthlyReport("pdf");
    }

    @FXML
    private void downloadMonthlyExcel() {
        downloadMonthlyReport("xlsx");
    }

    private void downloadDailyReport(String format) {
        LocalDate date = datePickerDaily.getValue();
        if (date == null) {
            showError("Please select a date first");
            return;
        }

        CompletableFuture
                .supplyAsync(() -> apiService.downloadDailyReport(date.toString(), format))
                .thenAccept(bytes -> Platform.runLater(() -> saveDownloadedFile(bytes,
                        "daily_report_" + date + "." + normalizeExtension(format),
                        format)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Download failed: " + ex.getMessage()));
                    return null;
                });
    }

    private void downloadMonthlyReport(String format) {
        String month = monthCombo.getValue();
        if (month == null) {
            showError("Please select a month first");
            return;
        }

        CompletableFuture
                .supplyAsync(() -> apiService.downloadMonthlyReport(month, format))
                .thenAccept(bytes -> Platform.runLater(() -> saveDownloadedFile(bytes,
                        "monthly_report_" + month + "." + normalizeExtension(format),
                        format)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Download failed: " + ex.getMessage()));
                    return null;
                });
    }

    private void saveDownloadedFile(byte[] bytes, String defaultName, String format) {
        if (bytes == null || bytes.length == 0) {
            showError("No file content received from server");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(defaultName);
        if ("pdf".equalsIgnoreCase(format)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        }

        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            showInfo("Report downloaded successfully: " + file.getName());
        } catch (IOException e) {
            showError("Failed to save report: " + e.getMessage());
        }
    }

    private String normalizeExtension(String format) {
        return "pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx";
    }

    private String formatOrders(List<OrderDetailDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            return "No completed orders for the selected period.";
        }

        StringBuilder builder = new StringBuilder();
        for (OrderDetailDTO order : orders) {
            builder.append("Order ID: ").append(safe(order.getSaleId())).append("\n");
            builder.append("Date/Time: ").append(safe(order.getDateTime())).append("\n");
            builder.append("Items Sold: ").append(order.getTotalItems()).append("\n");
            builder.append("Order Total: ").append(String.format("Rs %.2f", order.getOrderTotal())).append("\n");
            builder.append("Product Details:\n");

            if (order.getItems() == null || order.getItems().isEmpty()) {
                builder.append("  - No item details\n");
            } else {
                for (OrderDetailDTO.OrderItemDetailDTO item : order.getItems()) {
                    builder.append("  - ")
                            .append(safe(item.getProductName()))
                            .append(" | Qty: ").append(item.getQuantity())
                            .append(" | Sell: ").append(String.format("Rs %.2f", item.getSellingPrice()))
                            .append(" | Base: ").append(String.format("Rs %.2f", item.getBasePrice()))
                            .append(" | Line: ").append(String.format("Rs %.2f", item.getLineTotal()))
                            .append("\n");
                }
            }
            builder.append("----------------------------------------\n");
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
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

    public static class EditableSaleItem {
        private String productId;
        private String productName;
        private String category;
        private int quantity;
        private double price;
        private double basePrice;
        private double itemTotal;

        public EditableSaleItem(String productId, String productName, String category, int quantity,
                                double price, double basePrice) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.quantity = quantity;
            this.price = price;
            this.basePrice = basePrice;
            this.itemTotal = quantity * price;
        }

        public void recalculate() {
            this.itemTotal = this.quantity * this.price;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getBasePrice() { return basePrice; }
        public double getItemTotal() { return itemTotal; }
    }
}
