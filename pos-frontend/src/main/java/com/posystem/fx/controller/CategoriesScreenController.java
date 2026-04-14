package com.posystem.fx.controller;

import com.posystem.fx.dto.CategoryDTO;
import com.posystem.fx.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CategoriesScreenController implements RefreshableView {

    private final ApiService apiService;

    @FXML
    private TableView<CategoryDTO> categoryTable;

    @FXML
    private TableColumn<CategoryDTO, String> nameColumn;

    @FXML
    private TableColumn<CategoryDTO, String> descriptionColumn;

    @FXML
    private TableColumn<CategoryDTO, Void> actionColumn;

    @FXML
    private TextField nameField;

    @FXML
    private TextField descriptionField;

    @FXML
    private Button addCategoryButton;

    private String editingCategoryId;
    private boolean dataLoadStarted;
    private boolean initialDataLoaded;

    @FXML
    public void initialize() {
        setupTable();
        dataLoadStarted = true;
        refreshCategories(true);
    }

    @Override
    public void onViewActivated() {
        if (!initialDataLoaded && !dataLoadStarted) {
            dataLoadStarted = true;
            refreshCategories(true);
            return;
        }

        if (initialDataLoaded) {
            refreshCategories(true);
        }
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
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
                    CategoryDTO category = getTableView().getItems().get(getIndex());
                    startEditCategory(category);
                });
                deleteButton.setOnAction(event -> {
                    CategoryDTO category = getTableView().getItems().get(getIndex());
                    deleteCategoryById(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    @FXML
    private void refreshCategories() {
        refreshCategories(true);
    }

    private void refreshCategories(boolean forceRefresh) {
        CompletableFuture
                .supplyAsync(() -> apiService.getAllCategories(forceRefresh))
                .thenAccept(categories -> Platform.runLater(() ->
                        {
                            categoryTable.setItems(FXCollections.observableArrayList(categories));
                            initialDataLoaded = true;
                        }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Failed to load categories: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void addCategory() {
        String name = nameField.getText();
        String description = descriptionField.getText();
        String categoryId = editingCategoryId;

        if (name == null || name.isBlank()) {
            showError("Category name is required");
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        CategoryDTO category = new CategoryDTO();
                        category.setName(name);
                        category.setDescription(description);
                        category.setActive(true);
                        return categoryId != null
                                ? apiService.updateCategory(categoryId, category)
                                : apiService.addCategory(category);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(savedCategory -> Platform.runLater(() -> {
                    if (savedCategory == null || savedCategory.getId() == null) {
                        showError(categoryId != null ? "Failed to update category" : "Failed to save category to database");
                        return;
                    }

                    showInfo(categoryId != null ? "Category updated successfully" : "Category added successfully");
                    clearFields();
                    refreshCategories();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Error saving category: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void deleteCategory() {
        CategoryDTO selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a category");
            return;
        }
        deleteCategoryById(selected);
    }

    private void deleteCategoryById(CategoryDTO category) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Category");
        confirmAlert.setContentText("Are you sure you want to delete '" + category.getName() + "'?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> apiService.deleteCategory(category.getId()))
                .thenAccept(deleted -> Platform.runLater(() -> {
                    if (!deleted) {
                        showError("Failed to delete category");
                        return;
                    }

                    showInfo("Category deleted successfully");
                    if (category.getId().equals(editingCategoryId)) {
                        clearFields();
                    }
                    refreshCategories();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Error deleting category: " + ex.getMessage()));
                    return null;
                });
    }

    private void startEditCategory(CategoryDTO category) {
        if (category == null) {
            return;
        }

        editingCategoryId = category.getId();
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription());
        addCategoryButton.setText("Update Category");
    }

    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
        editingCategoryId = null;
        addCategoryButton.setText("Add Category");
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
