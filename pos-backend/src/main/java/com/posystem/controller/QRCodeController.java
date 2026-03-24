package com.posystem.controller;

import com.posystem.dto.ApiResponse;
import com.posystem.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qrcode")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<byte[]>> generateQRCode(@RequestParam String data) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQRCodeBytes(data);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new ApiResponse<>(true, "QR Code generated", qrCodeImage, java.time.LocalDateTime.now().toString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error generating QR code"));
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generateQRCodeGet(@RequestParam String data) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQRCodeBytes(data);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Error generating QR code"));
        }
    }
}
