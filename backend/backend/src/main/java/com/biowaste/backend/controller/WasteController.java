package com.biowaste.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class WasteController {

    @GetMapping("/analyze")
    public String analyze() {
        return "Backend is working 🚀";
    }

   @PostMapping("/analyze-image")
public Map<String, String> analyzeImage(@RequestParam("file") MultipartFile file) {

    Map<String, String> result = new HashMap<>();

    result.put("waste", "30%");
    result.put("use", "Compost");
    result.put("products", "Organic Fertilizer");
    result.put("profit", "₹80");
    result.put("co2", "50kg");

    return result;
}
}