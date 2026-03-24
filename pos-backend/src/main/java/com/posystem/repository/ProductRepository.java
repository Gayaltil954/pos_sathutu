package com.posystem.repository;

import com.posystem.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    List<Product> findByNameIgnoreCase(String name);

    List<Product> findByNameContainsIgnoreCase(String name);

    List<Product> findByCategoryAndNameContainsIgnoreCase(String category, String name);

    Optional<Product> findByQrCode(String qrCode);

    List<Product> findByActive(Boolean active);

    List<Product> findByCategoryAndActive(String category, Boolean active);
}
