package com.posystem.service;

import com.posystem.model.Sale;
import com.posystem.model.Product;
import com.posystem.repository.ProductRepository;
import com.posystem.repository.SaleRepository;
import com.posystem.dto.DailySummaryDTO;
import com.posystem.dto.MonthlySummaryDTO;
import com.posystem.dto.OrderDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SaleService {

    private static final long SUMMARY_CACHE_TTL_MS = 15_000;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Map<LocalDate, CacheEntry<DailySummaryDTO>> dailySummaryCache = new HashMap<>();
    private final Map<YearMonth, CacheEntry<MonthlySummaryDTO>> monthlySummaryCache = new HashMap<>();

    @Transactional
    public Sale saveSale(Sale sale) {
        validateSaleItems(sale);

        Map<String, Integer> newQuantityByProductId = toQuantityMap(sale.getItems());
        Map<String, Product> productsById = loadProductsByIds(newQuantityByProductId.keySet());

        applyStockDeltaAndValidate(productsById, Collections.emptyMap(), newQuantityByProductId);
        recalculateSale(sale, productsById);
        sale.setDate();

        invalidateSummaryCaches();

        return saleRepository.save(sale);
    }

    @Transactional
    public Sale updateSale(String saleId, Sale updatedSale) {
        Sale existingSale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

        validateSaleItems(updatedSale);

        Map<String, Integer> oldQuantityByProductId = toQuantityMap(existingSale.getItems());
        Map<String, Integer> newQuantityByProductId = toQuantityMap(updatedSale.getItems());
        Set<String> involvedProductIds = new HashSet<>();
        involvedProductIds.addAll(oldQuantityByProductId.keySet());
        involvedProductIds.addAll(newQuantityByProductId.keySet());
        Map<String, Product> productsById = loadProductsByIds(involvedProductIds);

        applyStockDeltaAndValidate(productsById, oldQuantityByProductId, newQuantityByProductId);

        existingSale.setItems(updatedSale.getItems());
        existingSale.setDiscount(updatedSale.getDiscount());
        existingSale.setPaymentMethod(updatedSale.getPaymentMethod());
        existingSale.setNotes(updatedSale.getNotes());
        recalculateSale(existingSale, productsById);

        invalidateSummaryCaches();

        return saleRepository.save(existingSale);
    }

    private void validateSaleItems(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("Sale items cannot be empty");
        }

        for (Sale.SaleItem item : sale.getItems()) {
            if (item.getProductId() == null || item.getProductId().isBlank()) {
                throw new IllegalArgumentException("Product id is required for each sale item");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Sale item quantity must be greater than zero");
            }
            if (item.getPrice() < 0) {
                throw new IllegalArgumentException("Sale item price cannot be negative");
            }
        }

        if (sale.getDiscount() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
    }

    private Map<String, Integer> toQuantityMap(List<Sale.SaleItem> items) {
        Map<String, Integer> quantityByProductId = new HashMap<>();
        if (items == null) {
            return quantityByProductId;
        }

        for (Sale.SaleItem item : items) {
            quantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantityByProductId;
    }

    private Map<String, Product> loadProductsByIds(Set<String> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        Map<String, Product> productsById = new HashMap<>(products.size());
        for (Product product : products) {
            productsById.put(product.getId(), product);
        }

        for (String productId : productIds) {
            if (!productsById.containsKey(productId)) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
        }
        return productsById;
    }

    private void applyStockDeltaAndValidate(Map<String, Product> productsById,
                                            Map<String, Integer> oldQuantityByProductId,
                                            Map<String, Integer> newQuantityByProductId) {
        for (Map.Entry<String, Product> entry : productsById.entrySet()) {
            String productId = entry.getKey();
            Product product = entry.getValue();
            int oldQty = oldQuantityByProductId.getOrDefault(productId, 0);
            int newQty = newQuantityByProductId.getOrDefault(productId, 0);
            int adjustedStock = product.getStock() + oldQty - newQty;

            if (adjustedStock < 0) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            product.setStock(adjustedStock);
            product.setUpdatedAt();
        }

        productRepository.saveAll(productsById.values());
    }

    private void recalculateSale(Sale sale, Map<String, Product> productsById) {
        double subtotal = 0;
        for (Sale.SaleItem item : sale.getItems()) {
            Product matchedProduct = productsById.get(item.getProductId());
            if (matchedProduct != null) {
                item.setProductName(matchedProduct.getName());
                item.setCategory(matchedProduct.getCategory());
                item.setBasePrice(matchedProduct.getBasePrice());
            }

            double lineTotal = item.getPrice() * item.getQuantity();
            item.setItemTotal(lineTotal);
            subtotal += lineTotal;
        }

        sale.setSubtotal(subtotal);
        double discount = Math.max(0, sale.getDiscount());
        sale.setDiscount(discount);
        sale.setFinalTotal(Math.max(0, subtotal - discount));
    }

    public Optional<Sale> getSaleById(String id) {
        return saleRepository.findById(id);
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public List<Sale> getRecentSales() {
        return saleRepository.findTop20ByOrderByDateDesc();
    }

    public DailySummaryDTO getDailySummary(LocalDate date) {
        DailySummaryDTO cached = getDailySummaryFromCache(date);
        if (cached != null) {
            return cached;
        }

        List<Sale> sales = getSalesForDay(date);

        double totalSales = sales.stream().mapToDouble(Sale::getSubtotal).sum();
        double totalDiscount = sales.stream().mapToDouble(Sale::getDiscount).sum();
        double netTotal = totalSales - totalDiscount;
        double totalActualPrice = calculateTotalActualPrice(sales);
        double reportSubtotal = netTotal - totalActualPrice;
        int transactionCount = sales.size();
        List<OrderDetailDTO> orders = mapOrderDetails(sales);

        DailySummaryDTO summary = new DailySummaryDTO();
        summary.setTotalSales(totalSales);
        summary.setTotalDiscount(totalDiscount);
        summary.setNetTotal(netTotal);
        summary.setTotalActualPrice(totalActualPrice);
        summary.setReportSubtotal(reportSubtotal);
        summary.setNumberOfTransactions(transactionCount);
        summary.setDate(date.toString());
        summary.setOrders(orders);

        putDailySummaryCache(date, summary);

        return summary;
    }

    public MonthlySummaryDTO getMonthlySummary(YearMonth month) {
        MonthlySummaryDTO cached = getMonthlySummaryFromCache(month);
        if (cached != null) {
            return cached;
        }

        List<Sale> sales = getSalesForMonth(month);

        double totalRevenue = sales.stream().mapToDouble(Sale::getSubtotal).sum();
        double totalDiscount = sales.stream().mapToDouble(Sale::getDiscount).sum();
        double netTotal = totalRevenue - totalDiscount;
        double totalActualPrice = calculateTotalActualPrice(sales);
        double reportSubtotal = netTotal - totalActualPrice;
        int transactionCount = sales.size();
        List<OrderDetailDTO> orders = mapOrderDetails(sales);

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
        summary.setNetTotal(netTotal);
        summary.setTotalActualPrice(totalActualPrice);
        summary.setReportSubtotal(reportSubtotal);
        summary.setMostSoldProducts(mostSoldProducts);
        summary.setCategoryWiseSales(categoryWiseSales);
        summary.setMonth(month.toString());
        summary.setOrders(orders);

        putMonthlySummaryCache(month, summary);

        return summary;
    }

    private synchronized DailySummaryDTO getDailySummaryFromCache(LocalDate date) {
        CacheEntry<DailySummaryDTO> entry = dailySummaryCache.get(date);
        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() - entry.createdAt > SUMMARY_CACHE_TTL_MS) {
            dailySummaryCache.remove(date);
            return null;
        }

        return entry.value;
    }

    private synchronized MonthlySummaryDTO getMonthlySummaryFromCache(YearMonth month) {
        CacheEntry<MonthlySummaryDTO> entry = monthlySummaryCache.get(month);
        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() - entry.createdAt > SUMMARY_CACHE_TTL_MS) {
            monthlySummaryCache.remove(month);
            return null;
        }

        return entry.value;
    }

    private synchronized void putDailySummaryCache(LocalDate date, DailySummaryDTO summary) {
        dailySummaryCache.put(date, new CacheEntry<>(summary));
    }

    private synchronized void putMonthlySummaryCache(YearMonth month, MonthlySummaryDTO summary) {
        monthlySummaryCache.put(month, new CacheEntry<>(summary));
    }

    private synchronized void invalidateSummaryCaches() {
        dailySummaryCache.clear();
        monthlySummaryCache.clear();
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final long createdAt;

        private CacheEntry(T value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }
    }

        private double calculateTotalActualPrice(List<Sale> sales) {
        return sales.stream()
            .filter(Objects::nonNull)
            .flatMap(sale -> sale.getItems() == null ? java.util.stream.Stream.empty() : sale.getItems().stream())
            .mapToDouble(item -> item.getBasePrice() * item.getQuantity())
            .sum();
        }

        private List<OrderDetailDTO> mapOrderDetails(List<Sale> sales) {
        return sales.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Sale::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toOrderDetail)
            .toList();
        }

        private OrderDetailDTO toOrderDetail(Sale sale) {
        List<OrderDetailDTO.OrderItemDetailDTO> itemDetails = sale.getItems() == null
            ? List.of()
            : sale.getItems().stream().map(item -> new OrderDetailDTO.OrderItemDetailDTO(
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getBasePrice(),
                item.getItemTotal(),
                item.getBasePrice() * item.getQuantity()
            )).toList();

        int totalItems = itemDetails.stream().mapToInt(OrderDetailDTO.OrderItemDetailDTO::getQuantity).sum();
        String dateTime = sale.getDate() == null ? "-" : sale.getDate().format(ORDER_DATE_FORMAT);

        return new OrderDetailDTO(
            sale.getId(),
            dateTime,
            totalItems,
            sale.getFinalTotal(),
            itemDetails
        );
        }

    public List<Sale> getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return saleRepository.findByDateGreaterThanEqualAndDateLessThan(start, end);
    }

    private List<Sale> getSalesForDay(LocalDate date) {
        try {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return saleRepository.findByDateGreaterThanEqualAndDateLessThan(start, end);
        } catch (Exception ignored) {
            return saleRepository.findAll().stream()
                    .filter(sale -> sale.getDate() != null)
                    .filter(sale -> sale.getDate().toLocalDate().equals(date))
                    .toList();
        }
    }

    private List<Sale> getSalesForMonth(YearMonth month) {
        try {
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
            return saleRepository.findByDateGreaterThanEqualAndDateLessThan(start, end);
        } catch (Exception ignored) {
            return saleRepository.findAll().stream()
                    .filter(sale -> sale.getDate() != null)
                    .filter(sale -> YearMonth.from(sale.getDate()).equals(month))
                    .toList();
        }
    }
}
