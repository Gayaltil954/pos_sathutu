package com.posystem.dto;

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

    private String qrCode;

    private int stock;

    private String description;

    private Boolean active;
}
