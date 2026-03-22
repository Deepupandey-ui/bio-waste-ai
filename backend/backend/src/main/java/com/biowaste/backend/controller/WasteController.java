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

    String name = file.getOriginalFilename().toLowerCase();

    String type;

    // 🔥 Smart classification (fake AI)
    if (name.contains("banana") || name.contains("food") || name.contains("vegetable")) {
        type = "Organic Waste";
    } else if (name.contains("plastic") || name.contains("bottle")) {
        type = "Dry Waste";
    } else {
        type = "Mixed Waste";
    }

    Random rand = new Random();
    int waste = rand.nextInt(60) + 30;

    String use;
    String products;

    if (type.equals("Organic Waste")) {
        use = "Biofuel / Compost";
        products = "Biogas, Fertilizer";
    } else if (type.equals("Dry Waste")) {
        use = "Recycling";
        products = "Plastic Reuse Products";
    } else {
        use = "Segregation Required";
        products = "Multiple Outputs";
    }

    Map<String, String> result = new HashMap<>();
    result.put("type", type);
    result.put("waste", waste + "%");
    result.put("use", use);
    result.put("products", products);
    result.put("profit", "₹" + (waste * 3));
    result.put("co2", (waste * 2) + "kg");

    return result;
}
}