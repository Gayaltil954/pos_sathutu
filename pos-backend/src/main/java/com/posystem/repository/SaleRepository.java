package com.posystem.repository;

import com.posystem.model.Sale;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends MongoRepository<Sale, String> {

    List<Sale> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Sale> findByDateGreaterThanEqualAndDateLessThan(LocalDateTime startDate, LocalDateTime endDate);
}
