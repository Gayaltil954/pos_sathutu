package com.posystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "products")
@CompoundIndexes({
    @CompoundIndex(name = "category_active_idx", def = "{'category': 1, 'active': 1}"),
    @CompoundIndex(name = "name_active_idx", def = "{'name': 1, 'active': 1}"),
    @CompoundIndex(name = "stock_active_idx", def = "{'stock': 1, 'active': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    private String name;

    @Indexed
    private String category;

    private double price;

    private double basePrice;

    @Indexed(unique = true, sparse = true)
    private String qrCode;

    private int stock;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Indexed
    private Boolean active = true;

    private String imageData; // Base64 encoded image

    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    public void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
