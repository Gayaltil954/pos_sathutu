package com.posystem.fx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.CategoryDTO;
import com.posystem.fx.dto.SaleDTO;
import com.posystem.fx.dto.ApiResponse;
import com.posystem.fx.dto.DailySummaryDTO;
import com.posystem.fx.dto.MonthlySummaryDTO;
import com.posystem.fx.dto.AppStatusDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ApiService {

    private static final long PRODUCTS_CACHE_TTL_MS = 15_000;
    private static final long CATEGORIES_CACHE_TTL_MS = 60_000;
    private static final long SYSTEM_STATUS_CACHE_TTL_MS = 8_000;
    private static final long DAILY_SUMMARY_CACHE_TTL_MS = 20_000;
    private static final long MONTHLY_SUMMARY_CACHE_TTL_MS = 20_000;
    private static final String PRODUCTS_CACHE_KEY = "products";
    private static final String CATEGORIES_CACHE_KEY = "categories";
    private static final String SYSTEM_STATUS_CACHE_KEY = "system-status";
    private static final String DAILY_SUMMARY_CACHE_PREFIX = "daily-summary:";
    private static final String MONTHLY_SUMMARY_CACHE_PREFIX = "monthly-summary:";

    @Value("${api.base-url:http://localhost:8080/api}")
    private String baseUrl;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final List<Runnable> productChangeListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> categoryChangeListeners = new CopyOnWriteArrayList<>();

    public ApiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    // Product Operations
    public List<ProductDTO> getAllProducts() {
        return getAllProducts(false);
    }

    public List<ProductDTO> getAllProducts(boolean forceRefresh) {
        if (forceRefresh) {
            invalidateProductCache();
        }

        List<ProductDTO> cached = getCached(PRODUCTS_CACHE_KEY, PRODUCTS_CACHE_TTL_MS);
        if (cached != null) {
            return cached;
        }

        List<ProductDTO> fetched = fetchProductList(baseUrl + "/products");
        cache.put(PRODUCTS_CACHE_KEY, new CacheEntry<>(fetched));
        return fetched;
    }

    public void registerProductsChangedListener(Runnable listener) {
        if (listener != null) {
            productChangeListeners.add(listener);
        }
    }

    public void registerCategoriesChangedListener(Runnable listener) {
        if (listener != null) {
            categoryChangeListeners.add(listener);
        }
    }

    public ProductDTO getProductById(String id) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/products/" + id, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), ProductDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ProductDTO getProductByQRCode(String qrCode) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/products/qrcode/" + qrCode, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), ProductDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ProductDTO> searchProducts(String term) {
        String safeTerm = UriUtils.encodeQueryParam(term, StandardCharsets.UTF_8);
        return fetchProductList(baseUrl + "/products/search?term=" + safeTerm);
    }

    public List<ProductDTO> getProductsByCategory(String category) {
        String safeCategory = UriUtils.encodePathSegment(category, StandardCharsets.UTF_8);
        return fetchProductList(baseUrl + "/products/category/" + safeCategory);
    }

    public ProductDTO addProduct(ProductDTO productDTO) {
        try {
            String response = restTemplate.postForObject(baseUrl + "/products", productDTO, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                ProductDTO savedProduct = objectMapper.convertValue(apiResponse.getData(), ProductDTO.class);
                upsertProductInCache(savedProduct);
                invalidateSystemStatusCache();
                notifyProductsChanged();
                return savedProduct;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ProductDTO updateProduct(String id, ProductDTO productDTO) {
        try {
            HttpEntity<ProductDTO> request = new HttpEntity<>(productDTO);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + "/products/" + id,
                    HttpMethod.PUT,
                    request,
                    String.class
            );
            ApiResponse<?> apiResponse = objectMapper.readValue(responseEntity.getBody(), ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                ProductDTO savedProduct = objectMapper.convertValue(apiResponse.getData(), ProductDTO.class);
                upsertProductInCache(savedProduct);
                invalidateSystemStatusCache();
                notifyProductsChanged();
                return savedProduct;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteProduct(String id) {
        try {
            restTemplate.delete(baseUrl + "/products/" + id);
            removeProductFromCache(id);
            invalidateSystemStatusCache();
            notifyProductsChanged();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Category Operations
    public List<CategoryDTO> getAllCategories() {
        return getAllCategories(false);
    }

    public List<CategoryDTO> getAllCategories(boolean forceRefresh) {
        if (forceRefresh) {
            invalidateCategoryCache();
        }

        List<CategoryDTO> cached = getCached(CATEGORIES_CACHE_KEY, CATEGORIES_CACHE_TTL_MS);
        if (cached != null) {
            return cached;
        }

        List<CategoryDTO> fetched = fetchCategoryList(baseUrl + "/categories");
        cache.put(CATEGORIES_CACHE_KEY, new CacheEntry<>(fetched));
        return fetched;
    }

    public void initializeCategories() {
        try {
            restTemplate.postForObject(baseUrl + "/categories/initialize", null, String.class);
            invalidateCategoryCache();
            notifyCategoriesChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CategoryDTO addCategory(CategoryDTO categoryDTO) {
        try {
            String response = restTemplate.postForObject(baseUrl + "/categories", categoryDTO, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                invalidateCategoryCache();
                notifyCategoriesChanged();
                return objectMapper.convertValue(apiResponse.getData(), CategoryDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public CategoryDTO updateCategory(String id, CategoryDTO categoryDTO) {
        try {
            HttpEntity<CategoryDTO> request = new HttpEntity<>(categoryDTO);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + "/categories/" + id,
                    HttpMethod.PUT,
                    request,
                    String.class
            );
            ApiResponse<?> apiResponse = objectMapper.readValue(responseEntity.getBody(), ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                invalidateCategoryCache();
                notifyCategoriesChanged();
                return objectMapper.convertValue(apiResponse.getData(), CategoryDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteCategory(String id) {
        try {
            restTemplate.delete(baseUrl + "/categories/" + id);
            invalidateCategoryCache();
            notifyCategoriesChanged();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Sales Operations
    public SaleDTO recordSale(SaleDTO saleDTO) {
        try {
            String response = restTemplate.postForObject(baseUrl + "/sales", saleDTO, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                invalidateProductCache();
                invalidateSystemStatusCache();
                invalidateReportSummaryCaches();
                notifyProductsChanged();
                return objectMapper.convertValue(apiResponse.getData(), SaleDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public SaleDTO updateSale(String id, SaleDTO saleDTO) {
        try {
            HttpEntity<SaleDTO> request = new HttpEntity<>(saleDTO);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + "/sales/" + id,
                    HttpMethod.PUT,
                    request,
                    String.class
            );
            ApiResponse<?> apiResponse = objectMapper.readValue(responseEntity.getBody(), ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                invalidateProductCache();
                invalidateSystemStatusCache();
                invalidateReportSummaryCaches();
                notifyProductsChanged();
                return objectMapper.convertValue(apiResponse.getData(), SaleDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public SaleDTO getSaleById(String id) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/" + id, String.class);
            ApiResponse<SaleDTO> apiResponse = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<SaleDTO>>() {});
            if (apiResponse != null && apiResponse.isSuccess()) {
                return apiResponse.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SaleDTO> getAllSales() {
        try {
            String response = restTemplate.getForObject(baseUrl + "/sales", String.class);
            ApiResponse<List<SaleDTO>> apiResponse = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<List<SaleDTO>>>() {});
            if (apiResponse != null && apiResponse.isSuccess()) {
                return apiResponse.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    public List<SaleDTO> getRecentSales() {
        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/recent", String.class);
            ApiResponse<List<SaleDTO>> apiResponse = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<List<SaleDTO>>>() {});
            if (apiResponse != null && apiResponse.isSuccess()) {
                return apiResponse.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        String suffix = threshold == null ? "" : "?threshold=" + threshold;
        return fetchProductList(baseUrl + "/products/low-stock" + suffix);
    }

    public AppStatusDTO getSystemStatus() {
        AppStatusDTO cached = getCached(SYSTEM_STATUS_CACHE_KEY, SYSTEM_STATUS_CACHE_TTL_MS);
        if (cached != null) {
            return cached;
        }

        try {
            String response = restTemplate.getForObject(baseUrl + "/system/status", String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                AppStatusDTO status = objectMapper.convertValue(apiResponse.getData(), AppStatusDTO.class);
                cache.put(SYSTEM_STATUS_CACHE_KEY, new CacheEntry<>(status));
                return status;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DailySummaryDTO getDailySummary(String date) {
        String cacheKey = DAILY_SUMMARY_CACHE_PREFIX + date;
        DailySummaryDTO cached = getCached(cacheKey, DAILY_SUMMARY_CACHE_TTL_MS);
        if (cached != null) {
            return cached;
        }

        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/summary/daily?date=" + date, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                DailySummaryDTO summary = objectMapper.convertValue(apiResponse.getData(), DailySummaryDTO.class);
                cache.put(cacheKey, new CacheEntry<>(summary));
                return summary;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MonthlySummaryDTO getMonthlySummary(String month) {
        String cacheKey = MONTHLY_SUMMARY_CACHE_PREFIX + month;
        MonthlySummaryDTO cached = getCached(cacheKey, MONTHLY_SUMMARY_CACHE_TTL_MS);
        if (cached != null) {
            return cached;
        }

        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/summary/monthly?month=" + month, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                MonthlySummaryDTO summary = objectMapper.convertValue(apiResponse.getData(), MonthlySummaryDTO.class);
                cache.put(cacheKey, new CacheEntry<>(summary));
                return summary;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] downloadDailyReport(String date, String format) {
        try {
            String safeDate = UriUtils.encodeQueryParam(date, StandardCharsets.UTF_8);
            String safeFormat = UriUtils.encodeQueryParam(format, StandardCharsets.UTF_8);
            String url = baseUrl + "/sales/reports/daily/download?date=" + safeDate + "&format=" + safeFormat;
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] downloadMonthlyReport(String month, String format) {
        try {
            String safeMonth = UriUtils.encodeQueryParam(month, StandardCharsets.UTF_8);
            String safeFormat = UriUtils.encodeQueryParam(format, StandardCharsets.UTF_8);
            String url = baseUrl + "/sales/reports/monthly/download?month=" + safeMonth + "&format=" + safeFormat;
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ProductDTO> fetchProductList(String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                Object data = apiResponse.getData();
                if (data instanceof List) {
                    return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<List<ProductDTO>>() {});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    private List<CategoryDTO> fetchCategoryList(String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                Object data = apiResponse.getData();
                if (data instanceof List) {
                    return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<List<CategoryDTO>>() {});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, long ttlMs) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() - entry.createdAt > ttlMs) {
            cache.remove(key);
            return null;
        }

        return (T) entry.value;
    }

    private void invalidateProductCache() {
        cache.remove(PRODUCTS_CACHE_KEY);
    }

    @SuppressWarnings("unchecked")
    private void upsertProductInCache(ProductDTO product) {
        if (product == null || product.getId() == null) {
            invalidateProductCache();
            return;
        }

        CacheEntry<?> entry = cache.get(PRODUCTS_CACHE_KEY);
        if (entry == null || !(entry.value instanceof List<?> existingList)) {
            return;
        }

        List<ProductDTO> updated = new ArrayList<>((List<ProductDTO>) existingList);
        boolean replaced = false;
        for (int i = 0; i < updated.size(); i++) {
            ProductDTO existing = updated.get(i);
            if (existing != null && product.getId().equals(existing.getId())) {
                updated.set(i, product);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            updated.add(product);
        }

        cache.put(PRODUCTS_CACHE_KEY, new CacheEntry<>(updated));
    }

    @SuppressWarnings("unchecked")
    private void removeProductFromCache(String id) {
        if (id == null || id.isBlank()) {
            invalidateProductCache();
            return;
        }

        CacheEntry<?> entry = cache.get(PRODUCTS_CACHE_KEY);
        if (entry == null || !(entry.value instanceof List<?> existingList)) {
            return;
        }

        List<ProductDTO> updated = new ArrayList<>((List<ProductDTO>) existingList);
        updated.removeIf(item -> item != null && id.equals(item.getId()));
        cache.put(PRODUCTS_CACHE_KEY, new CacheEntry<>(updated));
    }

    private void invalidateCategoryCache() {
        cache.remove(CATEGORIES_CACHE_KEY);
    }

    private void invalidateSystemStatusCache() {
        cache.remove(SYSTEM_STATUS_CACHE_KEY);
    }

    private void invalidateReportSummaryCaches() {
        cache.keySet().removeIf(key -> key.startsWith(DAILY_SUMMARY_CACHE_PREFIX)
                || key.startsWith(MONTHLY_SUMMARY_CACHE_PREFIX));
    }

    private void notifyProductsChanged() {
        for (Runnable listener : productChangeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyCategoriesChanged() {
        for (Runnable listener : categoryChangeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final long createdAt;

        private CacheEntry(T value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
