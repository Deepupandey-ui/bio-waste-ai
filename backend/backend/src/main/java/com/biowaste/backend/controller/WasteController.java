package com.biowaste.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class WasteController {

    // application.properties mein likho: gemini.api.key=${GOOGLE_API_KEY}
    // Ye tumhara environment variable GOOGLE_API_KEY automatically pick karega
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=";

    // ─── Real Indian bio-waste buyers database ────────────────────────────────
    private static final List<Map<String, Object>> BUYERS_DB = Arrays.asList(
        createBuyer("Ghazipur Waste to Energy Plant",          "Biofuel", 28.6253, 77.3275, "+91-11-23363321"),
        createBuyer("Okhla Compost Plant",                     "Compost", 28.5355, 77.2800, "+91-11-26815334"),
        createBuyer("Narela Bawana Waste to Energy",           "Biofuel", 28.8436, 77.1082, "+91-11-23963455"),
        createBuyer("Delhi MSW Solutions Limited",             "Compost", 28.8400, 77.1000, "+91-11-40411234"),
        createBuyer("Timarpur Okhla Waste Management Company", "Biofuel", 28.5441, 77.2917, "+91-11-26811235"),
        createBuyer("IARI Composting Unit",                    "Compost", 28.6380, 77.1530, "+91-11-25843375"),
        createBuyer("Noida Biogas Plant Sector 62",            "Biofuel", 28.6270, 77.3650, "+91-120-4252000"),
        createBuyer("Gurgaon Organic Waste Converter",         "Compost", 28.4595, 77.0266, "+91-124-4010101")
    );

    private static Map<String, Object> createBuyer(
            String name, String type, double lat, double lng, String contact) {
        Map<String, Object> b = new HashMap<>();
        b.put("name", name);
        b.put("type", type);
        b.put("lat", lat);
        b.put("lng", lng);
        b.put("contact", contact);
        return b;
    }

    // ─── Health check ─────────────────────────────────────────────────────────
    @GetMapping("/analyze")
    public String analyze() {
        return "BioWaste AI Backend running with Gemini Vision! 🚀";
    }

    // ─── Main analysis endpoint ───────────────────────────────────────────────
    @PostMapping("/analyze-image")
    public ResponseEntity<Map<String, Object>> analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lat", required = false) Double userLat,
            @RequestParam(value = "lng", required = false) Double userLng) {

        Map<String, Object> result = new HashMap<>();

        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            String geminiResponse = callGeminiAPI(base64Image, mimeType);
            result = parseGeminiResponse(geminiResponse);

        } catch (Exception e) {
            System.err.println("[GEMINI ERROR] " + e.getMessage());
            result.put("wasteType",  "Mixed");
            result.put("condition",  "Semi-rotten");
            result.put("waste",      "65%");
            result.put("use",        "Compost");
            result.put("products",   "Organic Fertilizer, Biogas");
            result.put("buyerType",  "Waste Management Companies");
            result.put("pricePerKg", "₹1 - ₹3");
            result.put("profit",     "₹120");
            result.put("co2",        "80kg");
            result.put("geminiRaw",  "Gemini unavailable - demo data");
            result.put("error",      e.getMessage());
        }

        String detectedUse = (String) result.getOrDefault("use", "Compost");
        List<Map<String, Object>> nearestBuyers = findNearestBuyers(detectedUse, userLat, userLng);
        result.put("buyers", nearestBuyers);

        return ResponseEntity.ok(result);
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
    // ─── Call Gemini Vision API (no Jackson — plain String HTTP) ─────────────
    private String callGeminiAPI(String base64Image, String mimeType) {
        RestTemplate restTemplate = new RestTemplate();

        String prompt = "Analyze this image and return ONLY a JSON object with no markdown. "
            + "Use exactly these keys: "
            + "isBioWaste (true/false), "
            + "wasteType (vegetable/fruit/food/agricultural/mixed), "
            + "condition (fresh/semi-rotten/fully-rotten), "
            + "wastePercent (number 0-100), "
            + "bestUse (Compost/Biofuel/Reuse/Sell), "
            + "products (comma separated string), "
            + "buyerType (string), "
            + "pricePerKgMin (number), "
            + "pricePerKgMax (number), "
            + "moistureContent (Low/Medium/High), "
            + "contaminationLevel (0%/Low/Medium/High). "
            + "Return ONLY raw JSON. No explanation. No markdown. No code block.";

        // Build JSON request as plain String — no Jackson dependency needed
        String requestJson = "{"
            + "\"contents\": [{"
            + "  \"parts\": ["
            + "    {\"inline_data\": {\"mime_type\": \"" + mimeType + "\", \"data\": \"" + base64Image + "\"}},"
            + "    {\"text\": \"" + prompt.replace("\"", "\\\"") + "\"}"
            + "  ]"
            + "}]}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            GEMINI_URL + geminiApiKey, entity, String.class);

        String body = response.getBody();
        if (body == null) throw new RuntimeException("Empty response from Gemini");

        // Extract the text value from Gemini's response JSON string
        int textStart = body.indexOf("\"text\":");
        if (textStart == -1) throw new RuntimeException("No text field in Gemini response");

        int quoteStart = body.indexOf("\"", textStart + 7) + 1;
        int quoteEnd   = body.lastIndexOf("\"");
        String extracted = body.substring(quoteStart, quoteEnd);

        return extracted
            .replace("\\n", " ")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    // ─── Parse Gemini JSON — manual parsing, zero external dependencies ───────
    private Map<String, Object> parseGeminiResponse(String geminiText) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cleaned = geminiText
                .replace("```json", "")
                .replace("```", "")
                .trim();

            result.put("wasteType",          extractString(cleaned, "wasteType",         "Mixed"));
            result.put("condition",           extractString(cleaned, "condition",          "Unknown"));
            result.put("use",                 extractString(cleaned, "bestUse",            "Compost"));
            result.put("products",            extractString(cleaned, "products",           "Organic Fertilizer"));
            result.put("buyerType",           extractString(cleaned, "buyerType",          "Waste Management Companies"));
            result.put("moistureContent",     extractString(cleaned, "moistureContent",    "Medium"));
            result.put("contaminationLevel",  extractString(cleaned, "contaminationLevel", "0%"));

            double wastePercent = extractNumber(cleaned, "wastePercent", 50);
            result.put("waste", (int) wastePercent + "%");

            double priceMin = extractNumber(cleaned, "pricePerKgMin", 1);
            double priceMax = extractNumber(cleaned, "pricePerKgMax", 3);
            result.put("pricePerKg", "₹" + (int) priceMin + " - ₹" + (int) priceMax);
            result.put("profit", "₹" + Math.round(((priceMin + priceMax) / 2.0) * 50));

            result.put("co2", estimateCO2(extractString(cleaned, "bestUse", "Compost")));
            result.put("isBioWaste", cleaned.contains("\"isBioWaste\": true")
                                  || cleaned.contains("\"isBioWaste\":true"));
            result.put("geminiRaw", geminiText);

        } catch (Exception e) {
            System.err.println("[PARSE ERROR] " + e.getMessage());
            result.put("waste",     "50%");
            result.put("use",       "Compost");
            result.put("products",  "Organic Fertilizer");
            result.put("profit",    "₹100");
            result.put("co2",       "60kg");
            result.put("geminiRaw", geminiText);
        }
        return result;
    }

    // ─── Extract string value by key from raw JSON string ────────────────────
    private String extractString(String json, String key, String defaultVal) {
        try {
            int idx   = json.indexOf("\"" + key + "\"");
            if (idx == -1) return defaultVal;
            int colon = json.indexOf(":", idx);
            int start = json.indexOf("\"", colon) + 1;
            int end   = json.indexOf("\"", start);
            return (start > 0 && end > start) ? json.substring(start, end) : defaultVal;
        } catch (Exception e) { return defaultVal; }
    }

    // ─── Extract number value by key from raw JSON string ────────────────────
    private double extractNumber(String json, String key, double defaultVal) {
        try {
            int idx   = json.indexOf("\"" + key + "\"");
            if (idx == -1) return defaultVal;
            int colon = json.indexOf(":", idx) + 1;
            while (colon < json.length() && json.charAt(colon) == ' ') colon++;
            int end = colon;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
            return Double.parseDouble(json.substring(colon, end));
        } catch (Exception e) { return defaultVal; }
    }

    // ─── Nearest buyers with Haversine distance ───────────────────────────────
    private List<Map<String, Object>> findNearestBuyers(
            String detectedUse, Double userLat, Double userLng) {

        String buyerType = detectedUse.contains("Biofuel") ? "Biofuel" : "Compost";
        List<Map<String, Object>> filtered = BUYERS_DB.stream()
            .filter(b -> b.get("type").equals(buyerType))
            .collect(Collectors.toList());

        if (userLat == null || userLng == null)
            return filtered.subList(0, Math.min(3, filtered.size()));

        List<Map<String, Object>> withDistance = new ArrayList<>();
        for (Map<String, Object> b : filtered) {
            double dist = haversine(userLat, userLng,
                (double) b.get("lat"), (double) b.get("lng"));
            Map<String, Object> copy = new HashMap<>(b);
            copy.put("distance", Math.round(dist * 100.0) / 100.0);
            String encoded = java.net.URLEncoder.encode(
                (String) b.get("name"), java.nio.charset.StandardCharsets.UTF_8);
            copy.put("mapLink", "https://www.google.com/maps/search/?api=1&query=" + encoded);
            withDistance.add(copy);
        }
        withDistance.sort(Comparator.comparingDouble(b -> (double) b.get("distance")));
        return withDistance.subList(0, Math.min(3, withDistance.size()));
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String estimateCO2(String use) {
        return switch (use) {
            case "Biofuel" -> "120kg";
            case "Compost" -> "50kg";
            case "Reuse"   -> "30kg";
            default        -> "60kg";
        };
    }
}
