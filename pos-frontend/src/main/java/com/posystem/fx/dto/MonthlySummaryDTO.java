package com.posystem.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {
    private double totalRevenue;
    private int totalTransactions;
    private double totalDiscount;
    private double netTotal;
    private double totalActualPrice;
    private double reportSubtotal;
    private Map<String, Integer> mostSoldProducts;
    private Map<String, Double> categoryWiseSales;
    private String month;
    private List<OrderDetailDTO> orders;
}
