package com.posystem.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryDTO {
    private double totalSales;
    private double totalDiscount;
    private int numberOfTransactions;
    private String date;
}
