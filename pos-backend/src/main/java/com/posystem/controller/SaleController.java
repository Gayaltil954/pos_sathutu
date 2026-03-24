package com.posystem.controller;

import com.posystem.model.Sale;
import com.posystem.dto.ApiResponse;
import com.posystem.dto.DailySummaryDTO;
import com.posystem.dto.MonthlySummaryDTO;
import com.posystem.service.SaleService;
import com.posystem.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SaleController {

    private final SaleService saleService;
    private final BillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<Sale>> recordSale(@RequestBody Sale sale) {
        try {
            Sale savedSale = saleService.saveSale(sale);
            return ResponseEntity.ok(ApiResponse.success(savedSale, "Sale recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error recording sale: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Sale>> getSale(@PathVariable String id) {
        Optional<Sale> sale = saleService.getSaleById(id);
        if (sale.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(sale.get(), "Sale fetched successfully"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Sale not found"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Sale>>> getAllSales() {
        List<Sale> sales = saleService.getAllSales();
        return ResponseEntity.ok(ApiResponse.success(sales, "All sales fetched successfully"));
    }

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<DailySummaryDTO>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailySummaryDTO summary = saleService.getDailySummary(date);
            return ResponseEntity.ok(ApiResponse.success(summary, "Daily summary fetched"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching daily summary: " + e.getMessage()));
        }
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<MonthlySummaryDTO>> getMonthlySummary(
            @RequestParam String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            MonthlySummaryDTO summary = saleService.getMonthlySummary(yearMonth);
            return ResponseEntity.ok(ApiResponse.success(summary, "Monthly summary fetched"));
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(400).body(ApiResponse.error("Invalid month format. Use YYYY-MM"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching monthly summary: " + e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<Sale>>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Sale> sales = saleService.getSalesByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(sales, "Sales fetched for date range"));
    }

    @GetMapping("/bill/pdf/{id}")
    public ResponseEntity<byte[]> getBillPDF(@PathVariable String id) {
        try {
            Optional<Sale> sale = saleService.getSaleById(id);
            if (sale.isPresent()) {
                byte[] pdfBytes = billService.generateBillPDF(sale.get());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header("Content-Disposition", "attachment; filename=\"bill_" + id + ".pdf\"")
                        .body(pdfBytes);
            }
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/bill/text/{id}")
    public ResponseEntity<ApiResponse<String>> getBillText(@PathVariable String id) {
        Optional<Sale> sale = saleService.getSaleById(id);
        if (sale.isPresent()) {
            String billText = billService.generateBillText(sale.get());
            return ResponseEntity.ok(ApiResponse.success(billText, "Bill generated"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Sale not found"));
    }
}
