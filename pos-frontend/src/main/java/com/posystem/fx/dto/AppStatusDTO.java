package com.posystem.fx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppStatusDTO {
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable;
    private String changelog;
    private int lowStockThreshold;
    private int lowStockCount;
}
