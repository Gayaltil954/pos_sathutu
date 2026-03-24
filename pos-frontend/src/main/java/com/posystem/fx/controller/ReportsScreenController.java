package com.posystem.fx.controller;

import com.posystem.fx.dto.DailySummaryDTO;
import com.posystem.fx.dto.MonthlySummaryDTO;
import com.posystem.fx.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportsScreenController {

    private final ApiService apiService;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label dailyTotalDiscountLabel;

    @FXML
    private Label dailyTransactionsLabel;

    @FXML
    private Label revenueLabel;

    @FXML
    private Label monthlyTotalDiscountLabel;

    @FXML
    private Label monthlyTransactionsLabel;

    @FXML
    private DatePicker datePickerDaily;

    @FXML
    private ComboBox<String> monthCombo;

    @FXML
    public void initialize() {
        datePickerDaily.setValue(LocalDate.now());

        List<String> months = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            months.add(currentMonth.minusMonths(i).toString());
        }
        monthCombo.getItems().setAll(months);
        monthCombo.setValue(currentMonth.toString());
    }

    @FXML
    private void loadDailySummary() {
        try {
            LocalDate date = datePickerDaily.getValue();
            if (date != null) {
                DailySummaryDTO summary = apiService.getDailySummary(date.toString());
                if (summary == null) {
                    showError("No daily summary data found");
                    return;
                }

                totalSalesLabel.setText(String.format("Rs %.2f", summary.getTotalSales()));
                dailyTotalDiscountLabel.setText(String.format("Rs %.2f", summary.getTotalDiscount()));
                dailyTransactionsLabel.setText(String.valueOf(summary.getNumberOfTransactions()));
            } else {
                showError("Please select a date");
            }
        } catch (Exception e) {
            showError("Error loading daily summary: " + e.getMessage());
        }
    }

    @FXML
    private void loadMonthlySummary() {
        try {
            String month = monthCombo.getValue();
            if (month != null) {
                MonthlySummaryDTO summary = apiService.getMonthlySummary(month);
                if (summary == null) {
                    showError("No monthly summary data found");
                    return;
                }

                revenueLabel.setText(String.format("Rs %.2f", summary.getTotalRevenue()));
                monthlyTotalDiscountLabel.setText(String.format("Rs %.2f", summary.getTotalDiscount()));
                monthlyTransactionsLabel.setText(String.valueOf(summary.getTotalTransactions()));
            } else {
                showError("Please select a month");
            }
        } catch (Exception e) {
            showError("Error loading monthly summary: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
