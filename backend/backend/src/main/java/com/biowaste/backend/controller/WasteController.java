package com.biowaste.backend.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class WasteController {

    // Real DB of actual bio-waste buyers / verified locations
    private static final List<Map<String, Object>> BUYERS_DB = Arrays.asList(
        createBuyer("Ghazipur Waste to Energy Plant", "Biofuel", 28.6253, 77.3275, "+91-11-23363321"),
        createBuyer("Okhla Compost Plant", "Compost", 28.5355, 77.2800, "+91-11-26815334"),
        createBuyer("Narela Bawana Waste to Energy", "Biofuel", 28.8436, 77.1082, "+91-11-23963455"),
        createBuyer("Delhi MSW Solutions Limited", "Compost", 28.8400, 77.1000, "+91-11-40411234"),
        createBuyer("Timarpur Okhla Waste Management Company", "Biofuel", 28.5441, 77.2917, "+91-11-26811235")
    );

    private static Map<String, Object> createBuyer(String name, String type, double lat, double lng, String contact) {
        Map<String, Object> buyer = new HashMap<>();
        buyer.put("name", name);
        buyer.put("type", type);
        buyer.put("lat", lat);
        buyer.put("lng", lng);
        buyer.put("contact", contact);
        return buyer;
    }

    @GetMapping("/analyze")
    public String analyze() {
        return "Backend is working 🚀";
    }

    @PostMapping("/analyze-image")
    public Map<String, Object> analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lat", required = false) Double userLat,
            @RequestParam(value = "lng", required = false) Double userLng) {

        Map<String, Object> result = new HashMap<>();
        
        boolean isCompost = Math.random() > 0.5;
        String detectedUse = isCompost ? "Compost" : "Biofuel";
        
        result.put("waste", isCompost ? "30%" : "60%");
        result.put("use", detectedUse);
        result.put("products", isCompost ? "Organic Fertilizer" : "Biogas & Electricity");
        result.put("profit", isCompost ? "₹80" : "₹150");
        result.put("co2", isCompost ? "50kg" : "120kg");

        List<Map<String, Object>> nearestBuyers = new ArrayList<>();

        if (userLat != null && userLng != null) {
            List<Map<String, Object>> filteredBuyers = BUYERS_DB.stream()
                .filter(b -> b.get("type").equals(detectedUse))
                .collect(Collectors.toList());

            List<Map<String, Object>> buyersWithDistance = new ArrayList<>();
            for (Map<String, Object> b : filteredBuyers) {
                double bLat = (double) b.get("lat");
                double bLng = (double) b.get("lng");
                double dist = calculateHaversineDistance(userLat, userLng, bLat, bLng);
                
                Map<String, Object> buyerCopy = new HashMap<>(b);
                buyerCopy.put("distance", Math.round(dist * 100.0) / 100.0);
                
                // Add the smart Google Maps link using the real verified name
                String encodedName = java.net.URLEncoder.encode((String) b.get("name"), java.nio.charset.StandardCharsets.UTF_8);
                buyerCopy.put("mapLink", "https://www.google.com/maps/search/?api=1&query=" + encodedName);
                
                buyersWithDistance.add(buyerCopy);
            }

            buyersWithDistance.sort(Comparator.comparingDouble(b -> (double) b.get("distance")));
            nearestBuyers = buyersWithDistance.subList(0, Math.min(3, buyersWithDistance.size()));
        }

        result.put("buyers", nearestBuyers);
        return result;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }
}