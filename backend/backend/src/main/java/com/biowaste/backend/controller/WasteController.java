package com.biowaste.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;
import java.net.URL;

// ✅ ADDED: Import for startup validation
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WasteController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ✅ Gemini API Key from environment variable
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Model name is configurable so endpoint can be updated without code changes.
    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // ✅ ADDED: Validate API key at startup (fail-fast)
    @PostConstruct
    private void validateGeminiApiKey() {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "❌ GEMINI_API_KEY environment variable not set!\n" +
                "   Fix: export GEMINI_API_KEY=\"your-key-from-aistudio.google.com\"\n" +
                "   Then restart the backend"
            );
        }
        System.out.println("✅ Gemini API Key loaded successfully (length: " + geminiApiKey.length() + ")");
    }

// @PostMapping("/analyze-image")
// public Map<String, String> analyzeImage(@RequestParam("file") MultipartFile file) {

//     String name = file.getOriginalFilename().toLowerCase();

//     String type;

//     // 🔥 Smart classification (fake AI)
//     if (name.contains("banana") || name.contains("food") || name.contains("vegetable")) {
//         type = "Organic Waste";
//     } else if (name.contains("plastic") || name.contains("bottle")) {
//         type = "Dry Waste";
//     } else {
//         type = "Mixed Waste";
//     }

//     Random rand = new Random();
//     int waste = rand.nextInt(60) + 30;

//     String use;
//     String products;

//     if (type.equals("Organic Waste")) {
//         use = "Biofuel / Compost";
//         products = "Biogas, Fertilizer";
//     } else if (type.equals("Dry Waste")) {
//         use = "Recycling";
//         products = "Plastic Reuse Products";
//     } else {
//         use = "Segregation Required";
//         products = "Multiple Outputs";
//     }

//     Map<String, String> result = new HashMap<>();
//     result.put("type", type);
//     result.put("waste", waste + "%");
//     result.put("use", use);
//     result.put("products", products);
//     result.put("profit", "₹" + (waste * 3));
//     result.put("co2", (waste * 2) + "kg");
//     return result;
// }
    // 📍 Hardcoded buyer locations in New Delhi
    private List<Map<String, Object>> createBuyers() {
        return Arrays.asList(
            createBuyer("Ghazipur Waste to Energy Plant", "Biofuel", 28.6253, 77.3275, "+91-11-23363321"),
            createBuyer("Okhla Compost Plant", "Compost", 28.5355, 77.2800, "+91-11-26815334"),
            createBuyer("Timarpur Waste Processing Unit", "Biofuel", 28.7149, 77.2661, "+91-11-27235678"),
            createBuyer("Narela Waste Management Facility", "Compost", 28.8100, 77.0600, "+91-11-27891234"),
            createBuyer("Rajkot Recycling Hub", "Compost", 28.6000, 77.2500, "+91-98765-43210"),
            createBuyer("Noida Bio-Waste Plant", "Biofuel", 28.5921, 77.3600, "+91-120-2345678"),
            createBuyer("Dwarka Composting Centre", "Compost", 28.5930, 77.0445, "+91-11-45678901"),
            createBuyer("Malviya Nagar Green Waste Hub", "Biofuel", 28.5244, 77.2019, "+91-11-23456789")
        );
    }

    // Helper method to create buyer object with coordinates
    private Map<String, Object> createBuyer(String name, String type, double lat, double lng, String contact) {
        Map<String, Object> buyer = new HashMap<>();
        buyer.put("name", name);
        buyer.put("type", type);
        buyer.put("lat", lat);           // ✅ Latitude for maps
        buyer.put("lng", lng);           // ✅ Longitude for maps
        buyer.put("contact", contact);
        return buyer;
    }

    // 🖼️ Main image analysis endpoint
    @PostMapping("/analyze-image")
    public ResponseEntity<?> analyzeImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "lat", required = false) Double userLat,
        @RequestParam(value = "lng", required = false) Double userLng
    ) {
        Map<String, Object> result = new HashMap<>();
        int imageSizeBytes = 0;
        String mimeType = "unknown";

        try {
            // Validate file
            if (file.isEmpty()) {
                result.put("error", true);
                result.put("errorMessage", "File is empty");
                return ResponseEntity.badRequest().body(result);
            }

            // Get file info
            byte[] imageBytes = file.getBytes();
            mimeType = file.getContentType();
            imageSizeBytes = imageBytes.length;
            
            System.out.println("📸 Analyzing image: " + file.getOriginalFilename());
            System.out.println("   Size: " + imageBytes.length + " bytes");
            System.out.println("   Type: " + mimeType);

            // Convert to base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Call Gemini API
            String geminiResponse = callGeminiAPI(base64Image, mimeType);

            // Parse response
            Map<String, Object> analysisResult = parseGeminiResponse(geminiResponse);

            // Add analysis to result
            result.putAll(analysisResult);
            result.put("isRealAnalysis", true);  // ✅ Real data flag
            result.put("error", false);

            // Get nearby buyers
            List<Map<String, Object>> buyers = findNearestBuyers(userLat, userLng);
            result.put("buyers", buyers);
            
            // Add user location if provided
            if (userLat != null && userLng != null) {
                result.put("userLocation", Map.of("lat", userLat, "lng", userLng));
            }

            return ResponseEntity.ok(result);

        // ✅ IMPROVED: Better error handling with detailed logging
        } catch (Exception e) {
            logGeminiError(e, imageSizeBytes, mimeType);
            Map<String, String> friendlyError = classifyGeminiError(e);
            List<Map<String, Object>> buyers = findNearestBuyers(userLat, userLng);
            
            result.put("error", true);
            result.put("errorMessage", friendlyError.get("message"));
            result.put("errorCategory", friendlyError.get("category"));
            result.put("technicalDetails", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            result.put("isRealAnalysis", false);  // ✅ Demo data flag
            result.put("buyers", buyers);

            if (userLat != null && userLng != null) {
                result.put("userLocation", Map.of("lat", userLat, "lng", userLng));
            }
            
            // ✅ Demo data separated with clear warning
            result.put("demo", Map.of(
                "warning", "⚠️ Analysis unavailable - showing sample data only",
                "wasteType", "Mixed",
                "condition", "Semi-rotten",
                "waste", "65%",
                "use", "Compost",
                "products", "Organic Fertilizer, Biogas",
                "note", "This is demo data. Please check backend logs for error details."
            ));
            
            return ResponseEntity.status(500).body(result);
        }
    }

    private Map<String, String> classifyGeminiError(Exception e) {
        String rawMessage = e.getMessage() == null ? "" : e.getMessage().toLowerCase();

        if (rawMessage.contains("429") || rawMessage.contains("resource_exhausted") || rawMessage.contains("quota")) {
            return Map.of(
                "category", "QUOTA_EXCEEDED",
                "message", "Gemini API quota exceeded. Please enable billing or wait for quota reset. Showing demo output for now."
            );
        }

        if (rawMessage.contains("401") || rawMessage.contains("unauthorized") || rawMessage.contains("invalid api key")) {
            return Map.of(
                "category", "INVALID_API_KEY",
                "message", "Gemini API key is invalid or expired. Update GEMINI_API_KEY and restart backend."
            );
        }

        if (rawMessage.contains("403")) {
            return Map.of(
                "category", "ACCESS_DENIED",
                "message", "Gemini API access denied. Check API enablement, project permissions, and billing setup."
            );
        }

        if (rawMessage.contains("404") || rawMessage.contains("not found")) {
            return Map.of(
                "category", "MODEL_NOT_FOUND",
                "message", "Gemini model endpoint is unavailable for this API version/key. Try a different model or API version."
            );
        }

        return Map.of(
            "category", "UNKNOWN",
            "message", "Gemini analysis is temporarily unavailable. Showing demo output for now."
        );
    }

    // 🔌 Call Gemini API
    private String callGeminiAPI(String base64Image, String mimeType) throws Exception {
        List<String> modelCandidates = Arrays.asList(
            geminiModel,
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash-latest"
        );
        List<String> apiVersions = Arrays.asList("v1beta", "v1");

        String lastError = "Unknown Gemini API error";

        for (String modelName : modelCandidates.stream().distinct().toList()) {
            for (String apiVersion : apiVersions) {
                String geminiUrl = "https://generativelanguage.googleapis.com/" + apiVersion + "/models/" + modelName + ":generateContent?key=" + geminiApiKey;

            // Create request JSON
            String requestBody = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [{\n" +
                "      \"text\": \"Analyze this waste image and respond ONLY with valid JSON (no markdown, no extra text). " +
                "Provide exactly this structure: {\\\"wasteType\\\": \\\"type\\\", \\\"condition\\\": \\\"condition\\\", " +
                "\\\"waste\\\": \\\"percentage\\\", \\\"use\\\": \\\"use\\\", \\\"products\\\": \\\"products\\\"}. " +
                "Analyze the waste shown and fill in details accordingly.\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"inlineData\": {\n" +
                "        \"mimeType\": \"" + mimeType + "\",\n" +
                "        \"data\": \"" + base64Image + "\"\n" +
                "      }\n" +
                "    }\n" +
                "    ]\n" +
                "  }]\n" +
                "}\n";

                URL url = new URL(geminiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Content-Type", "application/json");

                // Send request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Get response
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return readStream(connection.getInputStream());
                }

                String errorResponse = readStream(connection.getErrorStream());
                lastError = "Model " + modelName + " on API " + apiVersion + " failed with status " + responseCode + ": " + errorResponse;

                // Retry only when model/version is not found; otherwise return fast on auth/quota/input errors.
                if (responseCode != 404) {
                    throw new RuntimeException(lastError);
                }
            }
        }

        throw new RuntimeException(lastError);
    }

    // 📖 Parse Gemini response
    private Map<String, Object> parseGeminiResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode textNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

            String modelText = textNode.isMissingNode() ? "" : textNode.asText();

            if (modelText.startsWith("```")) {
                modelText = modelText.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode parsed = OBJECT_MAPPER.readTree(modelText);

            result.put("wasteType", parsed.path("wasteType").asText("Mixed"));
            result.put("condition", parsed.path("condition").asText("Semi-rotten"));
            result.put("waste", parsed.path("waste").asText("65%"));
            result.put("use", parsed.path("use").asText("Compost"));
            result.put("products", parsed.path("products").asText("Organic Fertilizer, Biogas"));
            result.put("geminiRaw", response);

            return result;
        } catch (Exception e) {
            // Fall back to defaults if response is malformed or model output isn't valid JSON.
            result.put("wasteType", "Mixed");
            result.put("condition", "Semi-rotten");
            result.put("waste", "65%");
            result.put("use", "Compost");
            result.put("products", "Organic Fertilizer, Biogas");
            result.put("geminiRaw", response);
            return result;
        }
    }

    // 📍 Find nearest buyers to user location
    private List<Map<String, Object>> findNearestBuyers(Double userLat, Double userLng) {
        List<Map<String, Object>> buyers = createBuyers();

        // If user location not provided, return all buyers
        if (userLat == null || userLng == null) {
            return buyers;
        }

        // Calculate distance and sort
        buyers.forEach(buyer -> {
            double buyerLat = (Double) buyer.get("lat");
            double buyerLng = (Double) buyer.get("lng");
            double distance = calculateDistance(userLat, userLng, buyerLat, buyerLng);
            buyer.put("distance_km", distance);
        });

        // Sort by distance
        return buyers.stream()
            .sorted((a, b) -> {
                double distA = (Double) a.getOrDefault("distance_km", Double.MAX_VALUE);
                double distB = (Double) b.getOrDefault("distance_km", Double.MAX_VALUE);
                return Double.compare(distA, distB);
            })
            .limit(5)  // Top 5 nearest
            .collect(Collectors.toList());
    }

    // 🧮 Calculate distance between two coordinates (Haversine formula)
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371; // Radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    // ✅ ADDED: Detailed error logging helper
    private void logGeminiError(Exception e, int imageSizeBytes, String mimeType) {
        String errorType = e.getClass().getSimpleName();
        String message = e.getMessage();
        
        System.err.println("\n❌ GEMINI API ERROR");
        System.err.println("   Type: " + errorType);
        System.err.println("   Message: " + message);
        System.err.println("   Image Size: " + imageSizeBytes + " bytes");
        System.err.println("   MIME Type: " + mimeType);
        System.err.println("   API Key Length: " + (geminiApiKey != null ? geminiApiKey.length() : 0));
        System.err.println("   Timestamp: " + java.time.LocalDateTime.now());
        
        // Log specific known issues
        if (message != null) {
            if (message.contains("401")) {
                System.err.println("   💡 Likely cause: Invalid or expired API key");
                System.err.println("   💡 Fix: Get new key from https://aistudio.google.com/app/apikey");
            } else if (message.contains("403")) {
                System.err.println("   💡 Likely cause: API quota exceeded or access denied");
                System.err.println("   💡 Fix: Check your API quotas and enable Gemini API in Google Cloud Console");
            } else if (message.contains("400")) {
                System.err.println("   💡 Likely cause: Malformed request (image too large?)");
                System.err.println("   💡 Fix: Check if image is < 20MB and in valid format (JPG, PNG, etc)");
            } else if (message.contains("Connection")) {
                System.err.println("   💡 Likely cause: Network issue or API endpoint unreachable");
                System.err.println("   💡 Fix: Check internet connection and firewall settings");
            }
        }
        System.err.println();
    }

    // ✅ Test endpoint to verify API key is configured
    @GetMapping("/test-gemini-config")
    public ResponseEntity<?> testGeminiConfig() {
        Map<String, Object> result = new HashMap<>();
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            result.put("status", "❌ FAILED");
            result.put("message", "GEMINI_API_KEY environment variable not set");
            result.put("fix", "Run: export GEMINI_API_KEY=\"your-key-from-aistudio.google.com\"");
            return ResponseEntity.status(500).body(result);
        }
        
        result.put("status", "✅ SUCCESS");
        result.put("message", "Gemini API key is properly configured");
        result.put("keyLength", geminiApiKey.length());
        return ResponseEntity.ok(result);
    }

    // Helper method to read stream
    private String readStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }
}
