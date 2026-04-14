package com.posystem.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.posystem.dto.DailySummaryDTO;
import com.posystem.dto.MonthlySummaryDTO;
import com.posystem.dto.OrderDetailDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class ReportExportService {

    private static final String CURRENCY_FORMAT = "Rs %.2f";

    public byte[] generateDailyReportPdf(DailySummaryDTO summary, LocalDate date) throws DocumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter.getInstance(document, output);

        document.open();
        addTitle(document, "Daily Report - " + date);
        addSummaryParagraph(document, summary.getTotalSales(), summary.getTotalDiscount(), summary.getNetTotal(),
                summary.getTotalActualPrice(), summary.getReportSubtotal(), summary.getNumberOfTransactions());
        addOrdersTable(document, summary.getOrders());
        document.close();

        return output.toByteArray();
    }

    public byte[] generateMonthlyReportPdf(MonthlySummaryDTO summary, YearMonth month) throws DocumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        PdfWriter.getInstance(document, output);

        document.open();
        addTitle(document, "Monthly Report - " + month);
        addSummaryParagraph(document, summary.getTotalRevenue(), summary.getTotalDiscount(), summary.getNetTotal(),
                summary.getTotalActualPrice(), summary.getReportSubtotal(), summary.getTotalTransactions());
        addOrdersTable(document, summary.getOrders());
        document.close();

        return output.toByteArray();
    }

    public byte[] generateDailyReportExcel(DailySummaryDTO summary, LocalDate date) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Daily " + date);
            configureSheetLayout(sheet);
            writeSheetHeader(workbook, sheet, "Daily Report - " + date);
            int rowIndex = writeSummaryRows(workbook, sheet, summary.getTotalSales(), summary.getTotalDiscount(),
                    summary.getNetTotal(), summary.getTotalActualPrice(), summary.getReportSubtotal(),
                    summary.getNumberOfTransactions());
            writeOrderRows(workbook, sheet, rowIndex, summary.getOrders());
            workbook.write(output);
            return output.toByteArray();
        }
    }

    public byte[] generateMonthlyReportExcel(MonthlySummaryDTO summary, YearMonth month) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Monthly " + month);
            configureSheetLayout(sheet);
            writeSheetHeader(workbook, sheet, "Monthly Report - " + month);
            int rowIndex = writeSummaryRows(workbook, sheet, summary.getTotalRevenue(), summary.getTotalDiscount(),
                    summary.getNetTotal(), summary.getTotalActualPrice(), summary.getReportSubtotal(),
                    summary.getTotalTransactions());
            writeOrderRows(workbook, sheet, rowIndex, summary.getOrders());
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void addTitle(Document document, String titleText) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Paragraph title = new Paragraph(titleText, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));
    }

    private void addSummaryParagraph(Document document,
                                     double totalSales,
                                     double totalDiscount,
                                     double netTotal,
                                     double totalActualPrice,
                                     double reportSubtotal,
                                     int orderCount) throws DocumentException {
        Font normal = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        document.add(new Paragraph("Total Sales/Revenue: " + String.format(CURRENCY_FORMAT, totalSales), normal));
        document.add(new Paragraph("Total Discount: " + String.format(CURRENCY_FORMAT, totalDiscount), normal));
        document.add(new Paragraph("Net Total: " + String.format(CURRENCY_FORMAT, netTotal), normal));
        document.add(new Paragraph("Total Actual Price: " + String.format(CURRENCY_FORMAT, totalActualPrice), normal));
        document.add(new Paragraph("Subtotal (Net Total - Total Actual Price): " + String.format(CURRENCY_FORMAT, reportSubtotal), normal));
        document.add(new Paragraph("Orders Completed: " + orderCount, normal));
        document.add(new Paragraph("\n"));
    }

    private void addOrdersTable(Document document, java.util.List<OrderDetailDTO> orders) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1.2f, 1.2f, 3.8f});
        table.setWidthPercentage(100);

        addHeaderCell(table, "Order");
        addHeaderCell(table, "Qty");
        addHeaderCell(table, "Total");
        addHeaderCell(table, "Items Sold (Qty)");

        if (orders != null) {
            for (OrderDetailDTO order : orders) {
                String orderText = safe(order.getSaleId()) + "\n" + safe(order.getDateTime());
                String itemsText = (order.getItems() == null ? java.util.List.<OrderDetailDTO.OrderItemDetailDTO>of() : order.getItems())
                        .stream()
                        .map(item -> safe(item.getProductName()) + " (" + item.getQuantity() + ")")
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("-");

                table.addCell(orderText);
                table.addCell(String.valueOf(order.getTotalItems()));
                table.addCell(String.format(CURRENCY_FORMAT, order.getOrderTotal()));
                table.addCell(itemsText);
            }
        }

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font header = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, header));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void writeSheetHeader(XSSFWorkbook workbook, XSSFSheet sheet, String title) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(FontUnderline.SINGLE.getByteValue());
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private int writeSummaryRows(XSSFWorkbook workbook,
                                 XSSFSheet sheet,
                                 double totalSales,
                                 double totalDiscount,
                                 double netTotal,
                                 double totalActualPrice,
                                 double reportSubtotal,
                                 int orderCount) {
        int rowIndex = 2;
        rowIndex = writeSummaryLine(sheet, rowIndex, "Total Sales/Revenue", totalSales);
        rowIndex = writeSummaryLine(sheet, rowIndex, "Total Discount", totalDiscount);
        rowIndex = writeSummaryLine(sheet, rowIndex, "Net Total", netTotal);
        rowIndex = writeSummaryLine(sheet, rowIndex, "Total Actual Price", totalActualPrice);
        rowIndex = writeSummaryLine(sheet, rowIndex, "Subtotal (Net Total - Total Actual Price)", reportSubtotal);
        Row ordersRow = sheet.createRow(rowIndex++);
        ordersRow.createCell(0).setCellValue("Orders Completed");
        ordersRow.createCell(1).setCellValue(orderCount);

        rowIndex++;
        Row header = sheet.createRow(rowIndex++);
        String[] columns = {"Order ID", "Date Time", "Product", "Qty", "Selling Price", "Base Price", "Line Total", "Actual Line Total"};

        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        return rowIndex;
    }

    private int writeSummaryLine(XSSFSheet sheet, int rowIndex, String key, double value) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
        return rowIndex;
    }

    private void writeOrderRows(XSSFWorkbook workbook, XSSFSheet sheet, int rowIndex, java.util.List<OrderDetailDTO> orders) {
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

        if (orders == null) {
            return;
        }

        for (OrderDetailDTO order : orders) {
            if (order.getItems() == null || order.getItems().isEmpty()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safe(order.getSaleId()));
                row.createCell(1).setCellValue(safe(order.getDateTime()));
                row.createCell(2).setCellValue("-");
                row.createCell(3).setCellValue(0);
                continue;
            }

            for (OrderDetailDTO.OrderItemDetailDTO item : order.getItems()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safe(order.getSaleId()));
                row.createCell(1).setCellValue(safe(order.getDateTime()));
                row.createCell(2).setCellValue(safe(item.getProductName()));
                row.createCell(3).setCellValue(item.getQuantity());

                Cell sellPrice = row.createCell(4);
                sellPrice.setCellValue(item.getSellingPrice());
                sellPrice.setCellStyle(numberStyle);

                Cell basePrice = row.createCell(5);
                basePrice.setCellValue(item.getBasePrice());
                basePrice.setCellStyle(numberStyle);

                Cell lineTotal = row.createCell(6);
                lineTotal.setCellValue(item.getLineTotal());
                lineTotal.setCellStyle(numberStyle);

                Cell actualLineTotal = row.createCell(7);
                actualLineTotal.setCellValue(item.getActualLineTotal());
                actualLineTotal.setCellStyle(numberStyle);
            }
        }
    }

    private void configureSheetLayout(XSSFSheet sheet) {
        int[] widths = {5000, 5200, 9000, 2600, 3600, 3600, 3600, 4200};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
