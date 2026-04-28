package com.biowaste.backend.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping({"/analyze", "/api/analyze"})
    public ResponseEntity<Map<String, String>> analyzeHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Backend is running. Use POST /api/analyze-image for image analysis."
        ));
    }
}
