package com.posystem.fx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.posystem.fx.dto.ProductDTO;
import com.posystem.fx.dto.CategoryDTO;
import com.posystem.fx.dto.SaleDTO;
import com.posystem.fx.dto.ApiResponse;
import com.posystem.fx.dto.DailySummaryDTO;
import com.posystem.fx.dto.MonthlySummaryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ApiService {

    @Value("${api.base-url:http://localhost:8080/api}")
    private String baseUrl;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ApiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    // Product Operations
    public List<ProductDTO> getAllProducts() {
        try {
            String response = restTemplate.getForObject(baseUrl + "/products", String.class);
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
        try {
            String response = restTemplate.getForObject(baseUrl + "/products/search?term=" + term, String.class);
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

    public List<ProductDTO> getProductsByCategory(String category) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/products/category/" + category, String.class);
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

    public ProductDTO addProduct(ProductDTO productDTO) {
        try {
            String response = restTemplate.postForObject(baseUrl + "/products", productDTO, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), ProductDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Category Operations
    public List<CategoryDTO> getAllCategories() {
        try {
            String response = restTemplate.getForObject(baseUrl + "/categories", String.class);
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

    public void initializeCategories() {
        try {
            restTemplate.postForObject(baseUrl + "/categories/initialize", null, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sales Operations
    public SaleDTO recordSale(SaleDTO saleDTO) {
        try {
            String response = restTemplate.postForObject(baseUrl + "/sales", saleDTO, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), SaleDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DailySummaryDTO getDailySummary(String date) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/summary/daily?date=" + date, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), DailySummaryDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MonthlySummaryDTO getMonthlySummary(String month) {
        try {
            String response = restTemplate.getForObject(baseUrl + "/sales/summary/monthly?month=" + month, String.class);
            ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
            if (apiResponse != null && apiResponse.isSuccess()) {
                return objectMapper.convertValue(apiResponse.getData(), MonthlySummaryDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
