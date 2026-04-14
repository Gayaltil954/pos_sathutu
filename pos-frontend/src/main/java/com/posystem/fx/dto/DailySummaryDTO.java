package com.posystem.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryDTO {
    private double totalSales;
    private double totalDiscount;
    private double netTotal;
    private double totalActualPrice;
    private double reportSubtotal;
    private int numberOfTransactions;
    private String date;
    private List<OrderDetailDTO> orders;
}
