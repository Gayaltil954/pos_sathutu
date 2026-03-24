package com.posystem.service;

import com.posystem.model.Category;
import com.posystem.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public void initializeDefaultCategories() {
        for (String categoryName : Category.DEFAULT_CATEGORIES) {
            Optional<Category> existing = categoryRepository.findByNameIgnoreCase(categoryName);
            if (existing.isEmpty()) {
                Category category = new Category();
                category.setName(categoryName);
                category.setActive(true);
                category.setCreatedAt(LocalDateTime.now());
                categoryRepository.save(category);
            }
        }
    }

    public Category addCategory(Category category) {
        category.setCreatedAt(LocalDateTime.now());
        category.setActive(true);
        return categoryRepository.save(category);
    }

    public Optional<Category> getCategoryById(String id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findByActive(true);
    }

    public Category updateCategory(String id, Category categoryDetails) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            Category existingCategory = category.get();
            if (categoryDetails.getName() != null) existingCategory.setName(categoryDetails.getName());
            if (categoryDetails.getDescription() != null) existingCategory.setDescription(categoryDetails.getDescription());
            if (categoryDetails.getActive() != null) existingCategory.setActive(categoryDetails.getActive());
            return categoryRepository.save(existingCategory);
        }
        return null;
    }

    public boolean deleteCategory(String id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
