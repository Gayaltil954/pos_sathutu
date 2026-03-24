package com.posystem.controller;

import com.posystem.model.Category;
import com.posystem.dto.ApiResponse;
import com.posystem.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Void>> initializeCategories() {
        try {
            categoryService.initializeDefaultCategories();
            return ResponseEntity.ok(ApiResponse.success(null, "Default categories initialized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error initializing categories: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> addCategory(@RequestBody Category category) {
        try {
            Category savedCategory = categoryService.addCategory(category);
            return ResponseEntity.ok(ApiResponse.success(savedCategory, "Category added successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error adding category: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategory(@PathVariable String id) {
        Optional<Category> category = categoryService.getCategoryById(id);
        if (category.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(category.get(), "Category fetched successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Category not found"));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<Category>> getCategoryByName(@PathVariable String name) {
        Optional<Category> category = categoryService.getCategoryByName(name);
        if (category.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(category.get(), "Category fetched successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Category not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable String id,
            @RequestBody Category categoryDetails) {
        Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
        if (updatedCategory != null) {
            return ResponseEntity.ok(ApiResponse.success(updatedCategory, "Category updated successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Category not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        if (categoryService.deleteCategory(id)) {
            return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Category not found"));
    }
}
