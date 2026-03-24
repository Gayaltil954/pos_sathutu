package com.posystem.service;

import com.posystem.model.Sale;
import com.posystem.model.Product;
import com.posystem.repository.ProductRepository;
import com.posystem.repository.SaleRepository;
import com.posystem.dto.DailySummaryDTO;
import com.posystem.dto.MonthlySummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public Sale saveSale(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("Sale items cannot be empty");
        }

        Map<String, Integer> soldQuantityByProductId = new HashMap<>();
        Map<String, Product> productsById = new HashMap<>();

        for (Sale.SaleItem item : sale.getItems()) {
            if (item.getProductId() == null || item.getProductId().isBlank()) {
                throw new IllegalArgumentException("Product id is required for each sale item");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Sale item quantity must be greater than zero");
            }

            soldQuantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);

            if (!productsById.containsKey(item.getProductId())) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
                productsById.put(item.getProductId(), product);
            }
        }

        for (Map.Entry<String, Integer> entry : soldQuantityByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int soldQty = entry.getValue();
            if (product.getStock() < soldQty) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
        }

        for (Map.Entry<String, Integer> entry : soldQuantityByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.setStock(product.getStock() - entry.getValue());
            product.setUpdatedAt();
        }

        productRepository.saveAll(productsById.values());
        sale.setDate();
        return saleRepository.save(sale);
    }

    public Optional<Sale> getSaleById(String id) {
        return saleRepository.findById(id);
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public DailySummaryDTO getDailySummary(LocalDate date) {
        List<Sale> sales = saleRepository.findAll().stream()
            .filter(sale -> sale.getDate() != null)
            .filter(sale -> sale.getDate().toLocalDate().equals(date))
            .toList();

        double totalSales = sales.stream().mapToDouble(Sale::getFinalTotal).sum();
        double totalDiscount = sales.stream().mapToDouble(Sale::getDiscount).sum();
        int transactionCount = sales.size();

        DailySummaryDTO summary = new DailySummaryDTO();
        summary.setTotalSales(totalSales);
        summary.setTotalDiscount(totalDiscount);
        summary.setNumberOfTransactions(transactionCount);
        summary.setDate(date.toString());

        return summary;
    }

    public MonthlySummaryDTO getMonthlySummary(YearMonth month) {
        List<Sale> sales = saleRepository.findAll().stream()
            .filter(sale -> sale.getDate() != null)
            .filter(sale -> YearMonth.from(sale.getDate()).equals(month))
            .toList();

        double totalRevenue = sales.stream().mapToDouble(Sale::getFinalTotal).sum();
        double totalDiscount = sales.stream().mapToDouble(Sale::getDiscount).sum();
        int transactionCount = sales.size();

        // Most sold products
        Map<String, Integer> mostSoldProducts = new HashMap<>();
        sales.forEach(sale -> {
            if (sale.getItems() != null) {
                sale.getItems().forEach(item -> {
                    mostSoldProducts.put(item.getProductName(),
                            mostSoldProducts.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                });
            }
        });

        // Category wise sales
        Map<String, Double> categoryWiseSales = new HashMap<>();
        sales.forEach(sale -> {
            if (sale.getItems() != null) {
                sale.getItems().forEach(item -> {
                    categoryWiseSales.put(item.getCategory(),
                            categoryWiseSales.getOrDefault(item.getCategory(), 0.0) + item.getItemTotal());
                });
            }
        });

        MonthlySummaryDTO summary = new MonthlySummaryDTO();
        summary.setTotalRevenue(totalRevenue);
        summary.setTotalTransactions(transactionCount);
        summary.setTotalDiscount(totalDiscount);
        summary.setMostSoldProducts(mostSoldProducts);
        summary.setCategoryWiseSales(categoryWiseSales);
        summary.setMonth(month.toString());

        return summary;
    }

    public List<Sale> getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return saleRepository.findByDateGreaterThanEqualAndDateLessThan(start, end);
    }
}
