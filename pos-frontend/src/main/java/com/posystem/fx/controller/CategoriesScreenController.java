package com.posystem.fx.controller;

import com.posystem.fx.dto.CategoryDTO;
import com.posystem.fx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoriesScreenController {

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

    @FXML
    public void initialize() {
        setupTable();
        refreshCategories();
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
        try {
            List<CategoryDTO> categories = apiService.getAllCategories();
            categoryTable.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load categories");
        }
    }

    @FXML
    private void addCategory() {
        try {
            if (nameField.getText().isEmpty()) {
                showError("Category name is required");
                return;
            }

            CategoryDTO category = new CategoryDTO();
            category.setName(nameField.getText());
            category.setDescription(descriptionField.getText());
            category.setActive(true);

            CategoryDTO savedCategory;
            if (editingCategoryId != null) {
                savedCategory = apiService.updateCategory(editingCategoryId, category);
                if (savedCategory == null || savedCategory.getId() == null) {
                    showError("Failed to update category");
                    return;
                }
                showInfo("Category updated successfully");
            } else {
                savedCategory = apiService.addCategory(category);
                if (savedCategory == null || savedCategory.getId() == null) {
                    showError("Failed to save category to database");
                    return;
                }
                showInfo("Category added successfully");
            }

            clearFields();
            refreshCategories();
        } catch (Exception e) {
            showError("Error saving category: " + e.getMessage());
        }
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

        try {
            boolean deleted = apiService.deleteCategory(category.getId());
            if (!deleted) {
                showError("Failed to delete category");
                return;
            }

            showInfo("Category deleted successfully");
            if (category.getId().equals(editingCategoryId)) {
                clearFields();
            }
            refreshCategories();
        } catch (Exception e) {
            showError("Error deleting category: " + e.getMessage());
        }
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
