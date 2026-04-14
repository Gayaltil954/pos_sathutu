package com.posystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    private String saleId;

    private String dateTime;

    private int totalItems;

    private double orderTotal;

    private List<OrderItemDetailDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetailDTO {
        private String productName;
        private int quantity;
        private double sellingPrice;
        private double basePrice;
        private double lineTotal;
        private double actualLineTotal;
    }
}
