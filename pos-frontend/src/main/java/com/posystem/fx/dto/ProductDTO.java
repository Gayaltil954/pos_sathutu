package com.posystem.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String id;
    private String name;
    private String category;
    private double price;
    private double basePrice;
    private String qrCode;
    private int stock;
    private String description;
    private Boolean active;
    private String imageData; // Base64 encoded image
}
