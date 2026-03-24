package com.posystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleDTO {

    private String id;

    private List<SaleItemDTO> items;

    private double subtotal;

    private double discount;

    private double finalTotal;

    private String date;

    private String paymentMethod;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDTO {
        private String productId;
        private String productName;
        private String category;
        private int quantity;
        private double price;
        private double itemTotal;
    }
}
