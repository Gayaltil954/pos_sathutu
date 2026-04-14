package com.posystem.controller;

import com.posystem.model.Product;
import com.posystem.dto.ApiResponse;
import com.posystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> addProduct(@RequestBody Product product) {
        try {
            Product savedProduct = productService.addProduct(product);
            return ResponseEntity.ok(ApiResponse.success(savedProduct, "Product added successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error adding product: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String id) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(product.get(), "Product fetched successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Product not found"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products, "Products fetched successfully"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Product>>> getByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(products, "Products fetched by category"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Product>>> searchProducts(@RequestParam String term) {
        List<Product> products = productService.searchProduct(term);
        return ResponseEntity.ok(ApiResponse.success(products, "Search results"));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts(
            @RequestParam(required = false) Integer threshold) {
        try {
            List<Product> products = productService.getLowStockProducts(threshold);
            return ResponseEntity.ok(ApiResponse.success(products, "Low stock products fetched"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching low stock products: " + e.getMessage()));
        }
    }

    @GetMapping("/low-stock/count")
    public ResponseEntity<ApiResponse<Integer>> getLowStockCount(
            @RequestParam(required = false) Integer threshold) {
        try {
            int count = productService.getLowStockProducts(threshold).size();
            return ResponseEntity.ok(ApiResponse.success(count, "Low stock count fetched"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching low stock count: " + e.getMessage()));
        }
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<List<Product>>> searchAdvanced(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        List<Product> products;
        if (name != null && category != null) {
            products = productService.searchByNameAndCategory(name, category);
        } else if (name != null) {
            products = productService.searchProduct(name);
        } else {
            products = productService.getProductsByCategory(category);
        }
        return ResponseEntity.ok(ApiResponse.success(products, "Search results"));
    }

    @GetMapping("/qrcode/{qrCode}")
    public ResponseEntity<ApiResponse<Product>> getByQRCode(@PathVariable String qrCode) {
        Optional<Product> product = productService.getProductByQrCode(qrCode);
        if (product.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(product.get(), "Product found"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Product not found with given QR code"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable String id,
            @RequestBody Product productDetails) {
        Product updatedProduct = productService.updateProduct(id, productDetails);
        if (updatedProduct != null) {
            return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Product updated successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Product not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Product not found"));
    }
}
