package com.posystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    private String id;

    private String name;

    private String description;

    private LocalDateTime createdAt;

    private Boolean active = true;

    public static final String[] DEFAULT_CATEGORIES = {
            "Chargers", "Backcovers", "Handsfree", "Tempered Glass", "Battery",
            "OTG", "Chip Reader", "Phones", "Speakers", "Mouse", "Keyboard",
            "Powerbank", "Router", "Dongle", "Phone Cable", "Earbuds",
            "Earbuds Covers", "Charging Dock", "Smartwatch", "Pen", "Chip"
    };
}
