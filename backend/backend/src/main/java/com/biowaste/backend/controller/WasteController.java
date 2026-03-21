package com.biowaste.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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

    Random rand = new Random();

    int waste = rand.nextInt(80) + 20; 

    String use = (waste > 50) ? "Biofuel" : "Compost";

    Map<String, String> result = new HashMap<>();
    result.put("waste", waste + "%");
    result.put("use", use);
    result.put("products", use.equals("Biofuel") ? "Ethanol, Biogas" : "Organic Fertilizer");
    result.put("profit", "₹" + (waste * 2));
    result.put("co2", (waste * 2) + "kg");

    return result;
}
}