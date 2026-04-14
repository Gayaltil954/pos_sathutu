package com.posystem.controller;

import com.posystem.dto.ApiResponse;
import com.posystem.dto.AppStatusDTO;
import com.posystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SystemController {

    private final ProductService productService;

    @Value("${app.version:1.3.7}")
    private String appVersion;

    @Value("${app.latest-version:1.3.7}")
    private String latestVersion;

    @Value("${app.latest-changelog:No changelog available.}")
    private String latestChangelog;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AppStatusDTO>> getAppStatus() {
        int threshold = productService.getDefaultLowStockThreshold();
        int lowStockCount = productService.getLowStockCount(threshold);
        boolean updateAvailable = isUpdateAvailable(appVersion, latestVersion);

        AppStatusDTO status = new AppStatusDTO(
                appVersion,
                latestVersion,
                updateAvailable,
                latestChangelog,
                threshold,
                lowStockCount
        );

        return ResponseEntity.ok(ApiResponse.success(status, "System status fetched"));
    }

    private boolean isUpdateAvailable(String currentVersion, String targetVersion) {
        String[] current = currentVersion.split("\\.");
        String[] target = targetVersion.split("\\.");
        int max = Math.max(current.length, target.length);

        for (int i = 0; i < max; i++) {
            int c = i < current.length ? parsePart(current[i]) : 0;
            int t = i < target.length ? parsePart(target[i]) : 0;
            if (t > c) {
                return true;
            }
            if (t < c) {
                return false;
            }
        }

        return false;
    }

    private int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
