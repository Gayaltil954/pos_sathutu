package com.posystem.service;

import com.posystem.model.Product;
import com.posystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${app.low-stock-threshold:5}")
    private int defaultLowStockThreshold;

    public Product addProduct(Product product) {
        if (product.getActive() == null) {
            product.setActive(true);
        }
        if (product.getName() != null) {
            product.setName(product.getName().trim());
        }
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().trim());
        }
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
            if (productDetails.getName() != null) existingProduct.setName(productDetails.getName().trim());
            if (productDetails.getCategory() != null) existingProduct.setCategory(productDetails.getCategory().trim());
            if (productDetails.getPrice() > 0) existingProduct.setPrice(productDetails.getPrice());
            if (productDetails.getBasePrice() >= 0) existingProduct.setBasePrice(productDetails.getBasePrice());
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
        return productRepository.findByNameContainsIgnoreCaseAndActive(searchTerm, true);
    }

    public Optional<Product> getProductByQrCode(String qrCode) {
        return productRepository.findByQrCode(qrCode);
    }

    public List<Product> searchByNameAndCategory(String name, String category) {
        return productRepository.findByCategoryAndNameContainsIgnoreCaseAndActive(category, name, true);
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        int resolvedThreshold = threshold != null ? threshold : defaultLowStockThreshold;
        if (resolvedThreshold < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }
        return productRepository.findByStockLessThanEqualAndActive(resolvedThreshold, true);
    }

    public int getLowStockCount(Integer threshold) {
        int resolvedThreshold = threshold != null ? threshold : defaultLowStockThreshold;
        if (resolvedThreshold < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }
        return Math.toIntExact(productRepository.countByStockLessThanEqualAndActive(resolvedThreshold, true));
    }

    public int getDefaultLowStockThreshold() {
        return defaultLowStockThreshold;
    }
}
