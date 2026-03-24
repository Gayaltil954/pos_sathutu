package com.posystem.service;

import com.posystem.model.Product;
import com.posystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product addProduct(Product product) {
        product.setCreatedAt();
        product.setUpdatedAt();
        return productRepository.save(product);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product updateProduct(String id, Product productDetails) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product existingProduct = product.get();
            if (productDetails.getName() != null) existingProduct.setName(productDetails.getName());
            if (productDetails.getCategory() != null) existingProduct.setCategory(productDetails.getCategory());
            if (productDetails.getPrice() > 0) existingProduct.setPrice(productDetails.getPrice());
            if (productDetails.getQrCode() != null) existingProduct.setQrCode(productDetails.getQrCode());
            if (productDetails.getStock() >= 0) existingProduct.setStock(productDetails.getStock());
            if (productDetails.getDescription() != null) existingProduct.setDescription(productDetails.getDescription());
            if (productDetails.getActive() != null) existingProduct.setActive(productDetails.getActive());
            existingProduct.setUpdatedAt();
            return productRepository.save(existingProduct);
        }
        return null;
    }

    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Product> getAllProducts() {
        return productRepository.findByActive(true);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActive(category, true);
    }

    public List<Product> searchProduct(String searchTerm) {
        return productRepository.findByNameContainsIgnoreCase(searchTerm);
    }

    public Optional<Product> getProductByQrCode(String qrCode) {
        return productRepository.findByQrCode(qrCode);
    }

    public List<Product> searchByNameAndCategory(String name, String category) {
        return productRepository.findByCategoryAndNameContainsIgnoreCase(category, name);
    }
}
