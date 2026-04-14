package com.posystem.controller;

import com.posystem.model.Sale;
import com.posystem.dto.ApiResponse;
import com.posystem.dto.DailySummaryDTO;
import com.posystem.dto.MonthlySummaryDTO;
import com.posystem.service.SaleService;
import com.posystem.service.BillService;
import com.posystem.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
    private final ReportExportService reportExportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Sale>> recordSale(@RequestBody Sale sale) {
        try {
            Sale savedSale = saleService.saveSale(sale);
            return ResponseEntity.ok(ApiResponse.success(savedSale, "Sale recorded successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error recording sale: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Sale>> updateSale(@PathVariable String id, @RequestBody Sale sale) {
        try {
            Sale updatedSale = saleService.updateSale(id, sale);
            return ResponseEntity.ok(ApiResponse.success(updatedSale, "Sale updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error updating sale: " + e.getMessage()));
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

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Sale>>> getRecentSales() {
        List<Sale> sales = saleService.getRecentSales();
        return ResponseEntity.ok(ApiResponse.success(sales, "Recent sales fetched successfully"));
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

    @GetMapping("/reports/daily/download")
    public ResponseEntity<byte[]> downloadDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "pdf") String format) {
        try {
            DailySummaryDTO summary = saleService.getDailySummary(date);
            String normalizedFormat = format.toLowerCase();

            if ("excel".equals(normalizedFormat) || "xlsx".equals(normalizedFormat)) {
                byte[] bytes = reportExportService.generateDailyReportExcel(summary, date);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily_report_" + date + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes);
            }

            byte[] bytes = reportExportService.generateDailyReportPdf(summary, date);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily_report_" + date + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/reports/monthly/download")
    public ResponseEntity<byte[]> downloadMonthlyReport(
            @RequestParam String month,
            @RequestParam(defaultValue = "pdf") String format) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            MonthlySummaryDTO summary = saleService.getMonthlySummary(yearMonth);
            String normalizedFormat = format.toLowerCase();

            if ("excel".equals(normalizedFormat) || "xlsx".equals(normalizedFormat)) {
                byte[] bytes = reportExportService.generateMonthlyReportExcel(summary, yearMonth);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monthly_report_" + month + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes);
            }

            byte[] bytes = reportExportService.generateMonthlyReportPdf(summary, yearMonth);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monthly_report_" + month + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
