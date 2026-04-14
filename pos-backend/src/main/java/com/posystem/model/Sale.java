package com.posystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    private String id;

    private List<SaleItem> items;

    private double subtotal;

    private double discount;

    private double finalTotal;

    @Indexed
    private LocalDateTime date;

    private String paymentMethod;

    private String notes;

    public void setDate() {
        this.date = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItem {
        private String productId;
        private String productName;
        private String category;
        private int quantity;
        private double price;
        private double basePrice;
        private double itemTotal;
    }
}
