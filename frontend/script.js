// Store Current Session
let currentUser = localStorage.getItem('username');
let editMode = false;
let userProfilePic = "https://via.placeholder.com/35/2ecc71/ffffff?text=" + (currentUser ? currentUser.charAt(0).toUpperCase() : "U");

document.addEventListener("DOMContentLoaded", async () => {
    // Attempt to load profile pic from backend proactively
    if (currentUser) {
        try {
            const res = await fetch("http://127.0.0.1:8080/api/auth/profile?username=" + currentUser);
            if(res.ok) {
                const data = await res.json();
                if(data.profilePic && data.profilePic.includes("data:image")) {
                    userProfilePic = data.profilePic;
                }
            }
        } catch(e) {}
    }
    updateNav();
    updateUploadUI();
});

// Update Navigation Links based on login
function updateNav() {
    const nav = document.getElementById("navLinks");
    if (!nav) return;
    if (currentUser) {
        nav.innerHTML = `
            <a href="index.html" class="active">Home</a>
            <a onclick="openProfile()" style="display:inline-flex; align-items:center; cursor:pointer;">
                <img id="navProfilePic" src="${userProfilePic}" style="width: 35px; height: 35px; border-radius: 50%; object-fit: cover; margin-right: 8px; border: 2px solid var(--primary-green);"> 
                Profile
            </a>
            <a onclick="logout()" class="btn-outline" style="color: #e74c3c; border-color: #e74c3c; margin-left: 15px;">🚪 Logout</a>
        `;
    } else {
        nav.innerHTML = `
            <a href="index.html" class="active">Home</a>
            <a href="login.html">Login</a>
            <a href="signup.html" class="btn-outline">Sign Up</a>
        `;
    }
}

// Ensure Uploads require Login
function updateUploadUI() {
    const uploadCard = document.getElementById("uploadCard");
    const welcomeText = document.getElementById("welcomeText");
    
    if (welcomeText) {
        welcomeText.innerText = currentUser ? `Hii, ${currentUser}! 👋` : "Join the Clean India Movement";
    }

    if (uploadCard) {
        if (currentUser) {
            uploadCard.innerHTML = `
                <h2>📸 Upload Image</h2>
                <div class="upload-area">
                    <input type="file" id="imageInput" accept="image/*">
                </div>
                <button onclick="analyze()">Analyze Waste</button>
            `;
            // Add listener to new input
            document.getElementById("imageInput").addEventListener("change", previewImage);
        } else {
            uploadCard.innerHTML = `
                <h2>📸 Upload Image Restricted</h2>
                <div class="empty-state" style="padding: 20px;">
                    <p style="margin-bottom: 15px; font-weight: 600;">You must be logged in safely to use the AI Analysis network.</p>
                    <button onclick="location.href='login.html'">Login Now 🚀</button>
                    <p style="margin-top: 15px; font-size: 0.9rem;">No account? <a href="signup.html" style="color: var(--dark-green); font-weight: bold;">Sign Up</a></p>
                </div>
            `;
        }
    }
}

function previewImage() {
    const file = this.files[0];
    const preview = document.getElementById("preview");
    const resultDiv = document.getElementById("result");
    if (file) {
        const reader = new FileReader();
        reader.onload = function (e) {
            preview.src = e.target.result;
            preview.style.display = "block";
        };
        reader.readAsDataURL(file);
        resultDiv.innerHTML = `
            <div class="empty-state glass-panel">
                <h3 style="color: var(--dark-green);">✅ Ready to Analyze</h3>
                <p>Click the <strong>Analyze Waste</strong> button to securely execute the assessment algorithm.</p>
            </div>
        `;
    }
}

function logout() {
    localStorage.removeItem('username');
    currentUser = null;
    userProfilePic = "https://via.placeholder.com/35/2ecc71/ffffff?text=U";
    updateNav();
    updateUploadUI();
    const resultDiv = document.getElementById("result");
    if(resultDiv) resultDiv.innerHTML = `
        <div class="empty-state glass-panel">
            <h3 style="color: var(--dark-green);">Dashboard Ready!</h3>
            <p>Log in and upload a waste image to see the AI analysis, efficiency levels, nearest buyers, and environmental impact.</p>
        </div>
    `;
    const preview = document.getElementById("preview");
    if(preview) preview.style.display = "none";
}

// ---------------- Profile Logic ---------------- 
const modal = document.getElementById("profileModal");

async function openProfile() {
    if(!modal) return;
    
    // Reset state
    editMode = false;
    document.getElementById("profileViewMode").style.display = "block";
    document.getElementById("profileForm").style.display = "none";
    document.getElementById("profilePicInput").style.display = "none";
    
    modal.style.display = "block";
    
    try {
        const res = await fetch("http://127.0.0.1:8080/api/auth/profile?username=" + currentUser);
        if(res.ok) {
            const data = await res.json();
            
            // View UI
            document.getElementById('displayFullName').innerText = data.fullName || "User Name Not Set";
            document.getElementById('displayPhone').innerText = data.phone || "Phone Not Provided";
            
            // Inputs UI
            if(data.fullName) document.getElementById('profFullName').value = data.fullName;
            if(data.phone) document.getElementById('profPhone').value = data.phone;
            
            // Image Logic
            if(data.profilePic && data.profilePic.includes("data:image")) {
                document.getElementById('profilePicPreview').src = data.profilePic;
                userProfilePic = data.profilePic;
                const navImg = document.getElementById("navProfilePic");
                if(navImg) navImg.src = userProfilePic;
            }
        }
    } catch(err) {
        console.error("Profile load error", err);
    }
}

function closeProfile() {
    if(modal) modal.style.display = "none";
}

function toggleEditProfile() {
    editMode = !editMode;
    document.getElementById("profileViewMode").style.display = editMode ? "none" : "block";
    document.getElementById("profileForm").style.display = editMode ? "block" : "none";
    document.getElementById("profilePicInput").style.display = editMode ? "inline-block" : "none";
}

if(document.getElementById("profilePicInput")) {
    document.getElementById("profilePicInput").addEventListener("change", function() {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                document.getElementById("profilePicPreview").src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    });
}

if(document.getElementById("profileForm")) {
    document.getElementById('profileForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = document.getElementById('profFullName').value;
        const p = document.getElementById('profPhone').value;
        const pic = document.getElementById('profilePicPreview').src;
        const msg = document.getElementById('profileMessage');
        
        msg.innerHTML = "Saving to Database...";
        msg.style.color = "var(--text-secondary)";
        
        try {
            const res = await fetch("http://127.0.0.1:8080/api/auth/profile", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({username: currentUser, fullName: f, phone: p, profilePic: pic})
            });
            if(res.ok) {
                msg.innerHTML = "✅ Saved securely!";
                msg.style.color = "var(--dark-green)";
                
                // Update views
                document.getElementById('displayFullName').innerText = f || "User Name Not Set";
                document.getElementById('displayPhone').innerText = p || "Phone Not Provided";
                
                // Live sync to navbar profile Image
                userProfilePic = pic;
                const navImg = document.getElementById("navProfilePic");
                if(navImg) navImg.src = userProfilePic;
                
                setTimeout(() => { 
                    msg.innerHTML = ""; 
                    toggleEditProfile(); 
                }, 1000);
            }
        } catch(err) {
            msg.innerHTML = "❌ Network error updating MySQL backend";
            msg.style.color = "red";
        }
    });
}

// ---------------- Analysis & Maps ---------------- 
function getUserLocation() {
    return new Promise((resolve) => {
        if ("geolocation" in navigator) {
            navigator.geolocation.getCurrentPosition(
                (position) => resolve({ lat: position.coords.latitude, lng: position.coords.longitude }),
                (error) => resolve(null) 
            );
        } else {
            resolve(null);
        }
    });
}

async function analyze() {
    const input = document.getElementById("imageInput");
    if(!input) return;
    const file = input.files[0];
    const resultDiv = document.getElementById("result");

    if (!file) {
        resultDiv.innerHTML = `<div class="error-card glass-panel">❌ Please upload an image first</div>`;
        return;
    }

    resultDiv.innerHTML = `
        <div class="empty-state glass-panel">
            <h3 style="color: var(--dark-green);">📍 Requesting GPS Location...</h3>
            <p>Scanning to match verified network buyers against your active coordinates.</p>
        </div>
    `;
    let location = await getUserLocation();

    const formData = new FormData();
    formData.append("file", file);
    if (location) {
        formData.append("lat", location.lat);
        formData.append("lng", location.lng);
    }

    // Loading UI
    resultDiv.innerHTML = `
    <div class="loader"></div>
    <p class="loading-text">Analyzing Waste...</p>
`;
    resultDiv.innerHTML = `
        <div class="empty-state glass-panel">
            <h3 style="color: var(--primary-green);">⏳ AI Neural Upload...</h3>
            <p>Our remote visual engine is classifying biological compositions.</p>
        </div>
    `;

    try {
        const response = await fetch("http://127.0.0.1:8080/analyze-image", {
            method: "POST",
            body: formData
        });

        if (!response.ok) throw new Error("Server error");
        const data = await response.json();

        const wastePercent = parseInt(data.waste);
        let suggestion = wastePercent > 50 ? "🔥 Best for Biofuel production" : "🌱 Best for Composting";

        let buyers = data.buyers || [];
        if (buyers.length === 0) {
             buyers = [
                {name: "Delhi MSW Solutions Limited", distance: 15.0, lat: 28.8400, lng: 77.1000, contact: "+91-11-40411234", mapLink: "https://www.google.com/maps/search/?api=1&query=Delhi+MSW"}
            ];
            if (!location) location = {lat: 28.6139, lng: 77.2090}; 
        }

        let buyerListHTML = buyers.map(b => `
            <li style="padding: 15px 0;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong>${b.name}</strong> 
                    <span style="color:var(--text-secondary);">${b.distance} km away</span>
                </div>
                <div style="font-size: 0.9rem; color: #555; margin-top: 5px;">
                    📞 Contact: <strong>${b.contact || "N/A"}</strong>
                    <a href="${b.mapLink || '#'}" target="_blank" class="map-btn" style="float:right; text-decoration:none; color:var(--primary-green); font-weight:bold; background: rgba(46, 204, 113, 0.1); padding: 4px 8px; border-radius: 4px;">
                       📍 View Details & Google Maps
                    </a>
                </div>
            </li>
        `).join('');

        // 🎯 MAIN RESULT UI
       resultDiv.innerHTML = `
    <div class="result-card">
        <h2>📊 Analysis Result</h2>

        <p><strong>Waste Type:</strong> ${data.type}</p>   <!-- 🔥 YAHAN ADD -->

        <p><strong>Waste Level:</strong> ${data.waste}</p>
        <p><strong>Best Use:</strong> ${data.use}</p>
        <p><strong>Products:</strong> ${data.products}</p>
        <p><strong>Profit:</strong> ${data.profit}</p>
        <p><strong>CO₂ Saved:</strong> ${data.co2}</p>
    </div>
`;
        resultDiv.innerHTML = `
            <div class="result-card glass-panel">
                <h2>📊 Analysis Result</h2>
                <div>
                    <p><strong>Waste Level:</strong> <span style="float:right;">${data.waste}</span></p>
                    <p><strong>Best Use:</strong> <span style="float:right;">${data.use}</span></p>
                    <p><strong>Products:</strong> <span style="float:right;">${data.products}</span></p>
                    <p><strong>Profit:</strong> <span style="float:right;" class="profit">${data.profit}</span></p>
                    <p><strong>CO₂ Saved:</strong> <span style="float:right;">${data.co2}</span></p>
                </div>
            </div>

            <div class="card glass-panel">
                <h3 style="margin-top:0; color:var(--dark-green);">⚡ Efficiency Level</h3>
                <div class="progress-bar">
                    <div class="progress" style="width: ${wastePercent}%"></div>
                </div>
            </div>

            <div class="recommendation-card glass-panel">
                <p style="margin:0;"><strong>System Recommendation:</strong> ${suggestion}</p>
            </div>

            <div class="buyer-card glass-panel">
                <h3>📍 Verified Indian Facility Network</h3>
                <ul>${buyerListHTML}</ul>
            </div>

            <div class="buyer-card glass-panel" style="margin-top: 20px;">
                <h3 style="margin-bottom: 15px;">🗺️ Geospatial Facility Network Map</h3>
                <div id="map" style="height: 350px; width: 100%; border-radius: 12px; z-index: 1;"></div>
            </div>
        `;

        setTimeout(() => {
            if (window.buyerMap) {
                window.buyerMap.remove();
            }
            window.buyerMap = L.map('map').setView([location.lat, location.lng], 10);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(window.buyerMap);

            const greenIcon = new L.Icon({
                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
            });
            const goldIcon = new L.Icon({
                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-gold.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
            });

            L.marker([location.lat, location.lng], {icon: greenIcon, title: 'Your Location'}).addTo(window.buyerMap).bindPopup("📍 Your Live GPS Location").openPopup();
                
            buyers.forEach((b, index) => {
                let emoji = index === 0 ? "🏭 " : index === 1 ? "♻️ " : "⚡ ";
                L.marker([b.lat, b.lng], {icon: goldIcon, title: b.name})
                    .addTo(window.buyerMap)
                    .bindPopup(emoji + "<b>" + b.name + "</b><br>📍 " + b.distance + " km away");
            });
            
            setTimeout(() => { window.buyerMap.invalidateSize(); }, 300);
        }, 100);

    } catch (error) {
    console.error(error);

    resultDiv.innerHTML = `
        <div class="error-card">
            <h3>⚠️ Connection Error</h3>
            <p>Backend not reachable. Please check server.</p>
        </div>
    `;
}
        console.error(error);
        resultDiv.innerHTML = `
            <div class="error-card glass-panel">
                <h3 style="margin:0;">❌ Error</h3>
                <p>Failed to connect to the backend server. Make sure Spring Boot is running.</p>
            </div>
        `;
    }
}