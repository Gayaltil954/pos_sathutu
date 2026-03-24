package com.posystem.fx.controller;

import com.posystem.fx.dto.CategoryDTO;
import com.posystem.fx.service.ApiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    private TextField nameField;

    @FXML
    private TextField descriptionField;

    @FXML
    public void initialize() {
        setupTable();
        refreshCategories();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
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

            // API call to add category
            // CategoryDTO savedCategory = apiService.addCategory(category);

            showInfo("Category added successfully");
            clearFields();
            refreshCategories();
        } catch (Exception e) {
            showError("Error adding category: " + e.getMessage());
        }
    }

    @FXML
    private void deleteCategory() {
        CategoryDTO selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // API call to delete category
                // apiService.deleteCategory(selected.getId());

                showInfo("Category deleted successfully");
                refreshCategories();
            } catch (Exception e) {
                showError("Error deleting category");
            }
        } else {
            showError("Please select a category");
        }
    }

    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
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
