// 🗺️ MAP.JS - Leaflet + OpenStreetMap Integration
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// Global variables
let map = null;
let userMarker = null;
let buyerMarkers = [];

// ─────────────────────────────────────────────────────────────────────────────
// 1️⃣  INITIALIZE MAP (Called once to set up Leaflet)
// ─────────────────────────────────────────────────────────────────────────────
function initializeMap() {
    const mapContainer = document.getElementById('map-container');
    
    if (!mapContainer) {
        console.log("ℹ️ Map container not found (ok if displaying list only)");
        return;
    }

    // If map already exists, don't reinitialize
    if (map !== null) {
        console.log("✅ Map already initialized");
        setTimeout(() => map.invalidateSize(), 0);
        return;
    }

    // Default location (New Delhi, India)
    const defaultLat = 28.6139;
    const defaultLng = 77.209;

    try {
        // ✅ Initialize Leaflet map with OpenStreetMap tiles (COMPLETELY FREE!)
        // OpenStreetMap provides free map tiles - no API key required!
        map = L.map('map-container', {
            center: [defaultLat, defaultLng],
            zoom: 12,
            scrollWheelZoom: true
        });

        // Add OpenStreetMap base layer (free tiles)
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19,
            minZoom: 2
        }).addTo(map);

        console.log("✅ Map initialized successfully with OpenStreetMap");
        setTimeout(() => map.invalidateSize(), 0);
        
    } catch (error) {
        console.error("❌ Error initializing map:", error);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2️⃣  DISPLAY USER LOCATION (Blue pin showing where image was taken)
// ─────────────────────────────────────────────────────────────────────────────
function displayUserLocation(lat, lng) {
    if (!map) {
        initializeMap();
    }
    
    if (!map) {
        console.error("❌ Map initialization failed");
        return;
    }

    // Remove old marker if it exists
    if (userMarker) {
        map.removeLayer(userMarker);
    }

    try {
        // ✅ Create blue circular marker for user location
        userMarker = L.circleMarker([lat, lng], {
            radius: 8,              // Marker size
            fillColor: "#0066cc",   // Blue color
            color: "#0066cc",       // Border color
            weight: 3,              // Border thickness
            opacity: 1,
            fillOpacity: 0.8        // Semi-transparent fill
        }).addTo(map);

        // Add popup when clicked
        userMarker.bindPopup(
            "<b>📍 Your Location</b><br>" +
            "Latitude: " + lat.toFixed(4) + "<br>" +
            "Longitude: " + lng.toFixed(4)
        );

        // Center map on user location
        map.setView([lat, lng], 13);
        
        console.log("✅ User location displayed at: " + lat + ", " + lng);
        
    } catch (error) {
        console.error("❌ Error displaying user location:", error);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3️⃣  DISPLAY BUYER LOCATIONS (Color-coded pins on map)
// ─────────────────────────────────────────────────────────────────────────────
function displayBuyers(buyers) {
    if (!map) {
        initializeMap();
    }
    
    if (!map || !buyers || buyers.length === 0) {
        console.log("ℹ️ No buyers to display or map not ready");
        return;
    }

    // Remove old buyer markers
    buyerMarkers.forEach(marker => {
        if (map.hasLayer(marker)) {
            map.removeLayer(marker);
        }
    });
    buyerMarkers = [];

    // ✅ Color scheme: Different colors for different waste types
    const colors = {
        "Biofuel": "#ff6b35",      // Orange for biofuel buyers
        "Compost": "#00a676"       // Green for compost buyers
    };

    buyers.forEach((buyer, index) => {
        // Validate coordinates exist
        if (buyer.lat == null || buyer.lng == null) {
            console.warn("⚠️ Buyer " + buyer.name + " has no coordinates");
            return;
        }

        try {
            // Get color based on waste type
            const color = colors[buyer.type] || "#999999";
            const icon = buyer.type === "Biofuel" ? "🔥" : "♻️";

            // ✅ Create colored circular marker
            const marker = L.circleMarker([buyer.lat, buyer.lng], {
                radius: 7,              // Marker size
                fillColor: color,       // Color based on type
                color: color,
                weight: 2,
                opacity: 1,
                fillOpacity: 0.7
            }).addTo(map);

            // ✅ Create detailed popup for buyer
            const distance = buyer.distance_km 
                ? "📏 Distance: " + buyer.distance_km.toFixed(1) + " km<br>" 
                : "";

            const popupContent = 
                "<div style='font-size: 0.9rem;'>" +
                "<b>" + icon + " " + buyer.name + "</b><br>" +
                "Type: " + buyer.type + "<br>" +
                distance +
                "📞 " + buyer.contact + "<br>" +
                "<small>Lat: " + buyer.lat.toFixed(4) + ", Lng: " + buyer.lng.toFixed(4) + "</small>" +
                "</div>";

            marker.bindPopup(popupContent);

            // Open popup on click
            marker.on('click', function() {
                this.openPopup();
            });

            buyerMarkers.push(marker);
            
        } catch (error) {
            console.error("❌ Error adding buyer marker:", buyer.name, error);
        }
    });

    console.log("✅ Displayed " + buyers.length + " buyers on map");

    // ✅ Auto-fit map to show all markers
    try {
        if (buyerMarkers.length > 0 && userMarker) {
            const group = new L.featureGroup([userMarker, ...buyerMarkers]);
            const bounds = group.getBounds();
            
            if (bounds.isValid()) {
                map.fitBounds(bounds, { 
                    padding: [50, 50],  // Add padding around markers
                    maxZoom: 14
                });
            }
        }
    } catch (error) {
        console.warn("⚠️ Could not auto-fit map bounds:", error);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4️⃣  GET USER LOCATION (Optional: Use browser geolocation)
// ─────────────────────────────────────────────────────────────────────────────
function getUserLocation() {
    if (!navigator.geolocation) {
        console.warn("⚠️ Geolocation not supported in this browser");
        // Fall back to default
        displayUserLocation(28.6139, 77.209);
        return;
    }

    console.log("📍 Requesting user's current location...");

    navigator.geolocation.getCurrentPosition(
        // Success callback
        (position) => {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            const accuracy = position.coords.accuracy;
            
            console.log("✅ Got user location: " + lat.toFixed(4) + ", " + lng.toFixed(4));
            console.log("   Accuracy: ±" + accuracy.toFixed(0) + " meters");
            
            displayUserLocation(lat, lng);
        },
        // Error callback
        (error) => {
            console.warn("⚠️ Could not get user location:", error.message);
            console.log("   Using default location (New Delhi)");
            // Fall back to default location
            displayUserLocation(28.6139, 77.209);
        },
        // Options
        {
            enableHighAccuracy: false,
            timeout: 5000,
            maximumAge: 0
        }
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// 5️⃣  EXPORT FUNCTIONS (Make available to other scripts)
// ─────────────────────────────────────────────────────────────────────────────

// ✅ Create global object with all map functions
globalThis.mapFunctions = {
    // Initialize the map
    init: function() {
        initializeMap();
    },
    
    // Show user's location on map
    displayUserLocation: function(lat, lng) {
        displayUserLocation(lat, lng);
    },
    
    // Show buyers on map
    displayBuyers: function(buyers) {
        displayBuyers(buyers);
    },
    
    // Get user's GPS location
    getUserLocation: function() {
        getUserLocation();
    },
    
    // Get map object (for advanced usage)
    getMap: function() {
        return map;
    },
    
    // Clear all markers
    clearMarkers: function() {
        if (userMarker && map.hasLayer(userMarker)) {
            map.removeLayer(userMarker);
        }
        buyerMarkers.forEach(marker => {
            if (map.hasLayer(marker)) {
                map.removeLayer(marker);
            }
        });
        buyerMarkers = [];
        console.log("✅ All markers cleared");
    }
};

console.log("✅ map.js loaded successfully");
console.log("   Use: mapFunctions.init(), mapFunctions.displayBuyers(), etc.");