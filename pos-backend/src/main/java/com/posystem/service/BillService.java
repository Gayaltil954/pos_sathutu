package com.posystem.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.posystem.model.Sale;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BillService {

    private static final String SHOP_NAME = "SATHUTU MOBILE SHOP";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");

    public byte[] generateBillPDF(Sale sale) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        PdfWriter.getInstance(document, byteArrayOutputStream);

        document.open();

        // Shop Name
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Paragraph title = new Paragraph(SHOP_NAME, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Separator
        Paragraph separator = new Paragraph("------------------------------------");
        separator.setAlignment(Element.ALIGN_CENTER);
        document.add(separator);

        // Date and Time
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        LocalDateTime saleDate = sale.getDate() != null ? sale.getDate() : LocalDateTime.now();
        Paragraph dateTime = new Paragraph("Date: " + saleDate.format(dateFormatter) + "\nTime: " + saleDate.format(timeFormatter), normalFont);
        document.add(dateTime);

        document.add(new Paragraph("\n"));

        // Items heading
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Paragraph itemsHeading = new Paragraph("Items:", headerFont);
        document.add(itemsHeading);

        // Items
        if (sale.getItems() != null) {
            for (Sale.SaleItem item : sale.getItems()) {
                String itemLine = item.getProductName() + " x" + item.getQuantity() + "      Rs " + String.format("%.2f", item.getItemTotal());
                Paragraph itemPara = new Paragraph(itemLine, normalFont);
                document.add(itemPara);
            }
        }

        document.add(new Paragraph("\n"));

        // Separator
        document.add(separator);

        // Subtotal, Discount, Total
        String subtotalLine = "Subtotal:     Rs " + String.format("%.2f", sale.getSubtotal());
        String discountLine = "Discount:     Rs " + String.format("%.2f", sale.getDiscount());
        String totalLine = "Final Total:  Rs " + String.format("%.2f", sale.getFinalTotal());

        document.add(new Paragraph(subtotalLine, normalFont));
        document.add(new Paragraph(discountLine, normalFont));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(totalLine, headerFont));

        // Separator
        document.add(new Paragraph("\n"));
        document.add(separator);

        // Thank you
        Paragraph thankYou = new Paragraph("Thank You!", normalFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        document.add(thankYou);

        document.close();

        return byteArrayOutputStream.toByteArray();
    }

    public String generateBillText(Sale sale) {
        StringBuilder bill = new StringBuilder();
        bill.append("----------------------------\n");
        bill.append("   " + SHOP_NAME + "\n");
        bill.append("----------------------------\n");

        LocalDateTime saleDate = sale.getDate() != null ? sale.getDate() : LocalDateTime.now();
        bill.append("Date: ").append(saleDate.format(dateFormatter)).append("\n");
        bill.append("Time: ").append(saleDate.format(timeFormatter)).append("\n\n");

        bill.append("Items:\n");
        if (sale.getItems() != null) {
            for (Sale.SaleItem item : sale.getItems()) {
                bill.append(item.getProductName()).append(" x").append(item.getQuantity())
                    .append("      Rs ").append(String.format("%.2f", item.getItemTotal())).append("\n");
            }
        }

        bill.append("\nSubtotal:     Rs ").append(String.format("%.2f", sale.getSubtotal())).append("\n");
        bill.append("Discount:     Rs ").append(String.format("%.2f", sale.getDiscount())).append("\n");
        bill.append("----------------------------\n");
        bill.append("Total:        Rs ").append(String.format("%.2f", sale.getFinalTotal())).append("\n");
        bill.append("----------------------------\n");
        bill.append("Thank You!\n");

        return bill.toString();
    }
}
