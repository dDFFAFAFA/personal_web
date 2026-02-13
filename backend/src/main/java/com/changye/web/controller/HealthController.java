package com.changye.web.controller;

import com.changye.web.common.ApiResponse;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        HealthStatus data = new HealthStatus("UP", "0.1.0", now);
        ApiResponse<HealthStatus> response = new ApiResponse<>(200, "success", data, now);
        log.info("Health check OK");
        return ResponseEntity.ok(response);
    }

    public record HealthStatus(String status, String version, String timestamp) {
    }
}
