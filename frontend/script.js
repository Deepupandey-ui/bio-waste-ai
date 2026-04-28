const ANALYZE_ENDPOINT = 'http://localhost:8080/api/analyze-image';
const PROFILE_STORAGE_KEY = 'bioWasteProfile';
const DEFAULT_PROFILE_AVATAR = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'%3E%3Cdefs%3E%3ClinearGradient id='g' x1='0' x2='1' y1='0' y2='1'%3E%3Cstop offset='0' stop-color='%23e8f6ee'/%3E%3Cstop offset='1' stop-color='%23d4efe0'/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width='120' height='120' rx='60' fill='url(%23g)'/%3E%3Ccircle cx='60' cy='45' r='22' fill='%2327ae60' opacity='0.85'/%3E%3Cpath d='M24 102c5-18 20-28 36-28s31 10 36 28' fill='%2327ae60' opacity='0.85'/%3E%3C/svg%3E";

let analysisLat = null;
let analysisLng = null;
let selectedImageFile = null;

document.addEventListener('DOMContentLoaded', () => {
    renderNavLinks();
    updateWelcomeText();
    renderUploadCard();
    hydrateProfileUI();

    const profileForm = document.getElementById('profileForm');
    if (profileForm) {
        profileForm.addEventListener('submit', saveProfile);
    }

    const profilePicInput = document.getElementById('profilePicInput');
    if (profilePicInput) {
        profilePicInput.addEventListener('change', saveProfilePicture);
    }
});

function renderNavLinks() {
    const navLinks = document.getElementById('navLinks');
    if (!navLinks) {
        return;
    }

    const username = (localStorage.getItem('username') || '').trim();

    if (username) {
        const profile = getSavedProfile();
        const avatarSrc = profile.profilePic || DEFAULT_PROFILE_AVATAR;

        navLinks.innerHTML = `
            <a href="index.html" class="active">Home</a>
            <a href="#" class="nav-profile-link" onclick="openProfile(); return false;">
                <img class="nav-avatar" src="${avatarSrc}" alt="Profile">
                <span>Profile</span>
            </a>
            <a href="#" class="btn-outline" onclick="logoutUser(); return false;">Logout</a>
        `;
    } else {
        navLinks.innerHTML = `
            <a href="index.html" class="active">Home</a>
            <a href="login.html">Login</a>
            <a href="signup.html" class="btn-outline">Sign Up</a>
        `;
    }
}

function updateWelcomeText() {
    const welcomeText = document.getElementById('welcomeText');
    if (!welcomeText) {
        return;
    }

    const username = (localStorage.getItem('username') || 'Guest').trim();
    welcomeText.textContent = `Hii, ${username}! 👋`;
}

function renderUploadCard() {
    const uploadCard = document.getElementById('uploadCard');
    if (!uploadCard) {
        return;
    }

    uploadCard.innerHTML = `
        <h3 style="color: var(--dark-green); margin-top: 0; margin-bottom: 8px;">Upload Waste Image</h3>
        <p style="margin-top: 0; margin-bottom: 16px; color: var(--text-secondary);">Choose an image, run AI analysis, and get nearest verified buyers.</p>

        <div class="upload-dropzone" onclick="openImagePicker()">
            <div class="upload-dropzone-title">Choose Image</div>
            <div class="upload-dropzone-subtitle">JPG, PNG, WEBP supported</div>
            <div class="upload-file-pill" id="selectedFileName">No file selected</div>
        </div>

        <input type="file" id="imageInput" accept="image/*" onchange="uploadImage(event)" style="display:none;">
        <button class="btn-primary analyze-action" id="analyzeBtn" onclick="analyzeSelectedImage()">Analyze Image</button>
        <button class="btn-outline secondary-action" id="reanalyzeBtn" onclick="reanalyzeLastImage()" style="margin-top: 10px; display: none;">Analyze Again</button>
        <p id="uploadStatus" style="margin: 10px 0 0; color: var(--text-secondary); font-size: 0.92rem;">No image selected yet.</p>
    `;
}

function openImagePicker() {
    const input = document.getElementById('imageInput');
    if (input) {
        input.click();
    }
}

function uploadImage(event) {
    const file = event.target.files[0];
    const selectedFileName = document.getElementById('selectedFileName');

    if (!file) {
        selectedImageFile = null;
        if (selectedFileName) {
            selectedFileName.textContent = 'No file selected';
        }
        setUploadStatus('No image selected yet.', true);
        return;
    }

    selectedImageFile = file;
    if (selectedFileName) {
        selectedFileName.textContent = file.name;
    }
    renderImagePreview(file);
    setUploadStatus(`Selected: ${file.name}`);
}

function analyzeSelectedImage() {
    if (!selectedImageFile) {
        showError('Please choose an image first.');
        setUploadStatus('Choose image first, then click Analyze Image.', true);
        return;
    }

    setUploadStatus('Analyzing image, please wait...');
    showLoadingSpinner();
    getUserLocationForAnalysis();
}

function reanalyzeLastImage() {
    if (!selectedImageFile) {
        showError('No previously uploaded image found. Please choose an image first.');
        return;
    }

    showLoadingSpinner();
    getUserLocationForAnalysis();
}

function renderImagePreview(file) {
    const preview = document.getElementById('preview');
    if (!preview) {
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        preview.src = e.target.result;
        preview.style.display = 'block';
    };
    reader.readAsDataURL(file);
}

function getUserLocationForAnalysis() {
    if (!navigator.geolocation) {
        analyzeWithBackend(null, null);
        return;
    }

    navigator.geolocation.getCurrentPosition(
        (position) => {
            analysisLat = position.coords.latitude;
            analysisLng = position.coords.longitude;
            analyzeWithBackend(analysisLat, analysisLng);
        },
        () => {
            analysisLat = null;
            analysisLng = null;
            analyzeWithBackend(null, null);
        },
        {
            enableHighAccuracy: false,
            timeout: 8000,
            maximumAge: 60000
        }
    );
}

function analyzeWithBackend(lat, lng) {
    const fileInput = document.getElementById('imageInput');
    const file = selectedImageFile || (fileInput ? fileInput.files[0] : null);

    if (!file) {
        showError('Please select an image first.');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    if (lat != null && lng != null) {
        formData.append('lat', String(lat));
        formData.append('lng', String(lng));
    }

    fetch(ANALYZE_ENDPOINT, {
        method: 'POST',
        body: formData
    })
        .then(async (response) => {
            const data = await response.json();
            if (!response.ok && !data?.demo) {
                throw new Error(data.errorMessage || 'Analysis request failed.');
            }
            return data;
        })
        .then((data) => {
            displayAnalysisResults(data);
            displayMapWithBuyers(data);
            toggleReanalyzeButton(true);
            setUploadStatus('Analysis complete. You can choose a new image or analyze again.');
        })
        .catch((error) => {
            showError(error.message || 'Unable to analyze image right now.');
            hideMapSection();
            setUploadStatus('Analysis failed. Please retry with the same or a new image.', true);
        });
}

function displayAnalysisResults(data) {
    const resultsDiv = document.getElementById('results');
    if (!resultsDiv) {
        return;
    }

    const isFailed = data.error === true;
    const source = isFailed ? (data.demo || {}) : data;

    const badge = isFailed
        ? '<div style="background:#fff3cd;border:1px solid #ffe08a;color:#856404;padding:10px 12px;border-radius:10px;margin-bottom:12px;">Live analysis unavailable. Showing fallback demo output.</div>'
        : '<div style="background:#dcfce7;border:1px solid #86efac;color:#166534;padding:10px 12px;border-radius:10px;margin-bottom:12px;">Live Gemini analysis completed successfully.</div>';

    resultsDiv.innerHTML = `
        ${badge}
        <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;">
            <div><strong>Waste Type</strong><br>${source.wasteType || 'Unknown'}</div>
            <div><strong>Condition</strong><br>${source.condition || 'Unknown'}</div>
            <div><strong>Waste Percentage</strong><br>${source.waste || 'N/A'}</div>
            <div><strong>Best Use</strong><br>${source.use || 'N/A'}</div>
        </div>
        <div style="margin-top:14px;padding-top:10px;border-top:1px dashed rgba(0,0,0,0.2);">
            <strong>Products</strong><br>${source.products || 'Not specified'}
        </div>
    `;
}

function displayMapWithBuyers(data) {
    if (!data.buyers || data.buyers.length === 0) {
        hideMapSection();
        renderBuyerList([]);
        return;
    }

    const mapSection = document.getElementById('map-section');
    if (!mapSection) {
        return;
    }

    mapSection.style.display = 'block';

    try {
        if (!globalThis.mapFunctions) {
            throw new Error('Map library not loaded.');
        }

        globalThis.mapFunctions.init();
        if (analysisLat != null && analysisLng != null) {
            globalThis.mapFunctions.displayUserLocation(analysisLat, analysisLng);
        } else {
            globalThis.mapFunctions.displayUserLocation(28.6139, 77.209);
        }

        globalThis.mapFunctions.displayBuyers(data.buyers);
        renderBuyerList(data.buyers);
        mapSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (error) {
        showError('Map could not be loaded. Please refresh and try again.');
        hideMapSection();
        console.error(error);
    }
}


function showLoadingSpinner() {
    const resultsDiv = document.getElementById('results');
    if (!resultsDiv) {
        return;
    }

    resultsDiv.innerHTML = `
        <div style="text-align:center;padding:30px;">
            <div style="display:inline-block;width:40px;height:40px;border:4px solid #f3f3f3;border-top:4px solid #2ecc71;border-radius:50%;animation:spin 1s linear infinite;"></div>
            <p style="color:#27ae60;margin-top:14px;font-weight:600;">Analyzing image with AI...</p>
        </div>
        <style>
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        </style>
    `;
}

function hideMapSection() {
    const mapSection = document.getElementById('map-section');
    if (mapSection) {
        mapSection.style.display = 'none';
    }
}

function showError(message) {
    const resultsDiv = document.getElementById('results');
    if (!resultsDiv) {
        return;
    }

    resultsDiv.innerHTML = `
        <div class="error-card">
            <strong>Error</strong><br>
            ${message}
        </div>
    `;
}

function renderBuyerList(buyers) {
    const container = document.getElementById('buyers-list');
    if (!container) {
        return;
    }

    if (!buyers || buyers.length === 0) {
        container.innerHTML = '';
        return;
    }

    container.innerHTML = buyers.map((buyer) => {
        const hasDistance = buyer.distance_km !== undefined && buyer.distance_km !== null;
        const distanceText = hasDistance ? `${Number(buyer.distance_km).toFixed(1)} km` : 'Distance unavailable';
        const phone = buyer.contact || 'N/A';
        const safePhone = (buyer.contact || '').replaceAll(/\s+/g, '');
        const lat = buyer.lat;
        const lng = buyer.lng;
        const destination = String(lat) + ',' + String(lng);
        const routeUrl = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destination)}`;

        return `
            <article class="buyer-item">
                <h4>${buyer.name || 'Buyer'}</h4>
                <p class="buyer-meta"><strong>Type:</strong> ${buyer.type || 'Unknown'}</p>
                <p class="buyer-meta"><strong>Distance:</strong> ${distanceText}</p>
                <p class="buyer-meta"><strong>Contact:</strong> ${phone}</p>
                <div class="buyer-actions">
                    ${safePhone ? `<a class="btn-call" href="tel:${safePhone}">Call</a>` : ''}
                    <a class="btn-route" href="${routeUrl}" target="_blank" rel="noopener noreferrer">Get Route</a>
                </div>
            </article>
        `;
    }).join('');
}

function toggleReanalyzeButton(show) {
    const btn = document.getElementById('reanalyzeBtn');
    if (btn) {
        btn.style.display = show ? 'block' : 'none';
    }
}

function setUploadStatus(message, isMuted = false) {
    const status = document.getElementById('uploadStatus');
    if (!status) {
        return;
    }

    status.textContent = message;
    status.style.color = isMuted ? 'var(--text-secondary)' : 'var(--dark-green)';
}

function getSavedProfile() {
    const raw = localStorage.getItem(PROFILE_STORAGE_KEY);
    if (!raw) {
        return { fullName: '', phone: '', profilePic: '' };
    }

    try {
        return JSON.parse(raw);
    } catch {
        return { fullName: '', phone: '', profilePic: '' };
    }
}

function saveProfile(event) {
    event.preventDefault();
    const profile = getSavedProfile();
    profile.fullName = (document.getElementById('profFullName')?.value || '').trim();
    profile.phone = (document.getElementById('profPhone')?.value || '').trim();
    localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(profile));

    const message = document.getElementById('profileMessage');
    if (message) {
        message.style.color = 'var(--dark-green)';
        message.textContent = 'Profile updated successfully.';
    }

    hydrateProfileUI();
    toggleEditProfile(false);
}

function saveProfilePicture(event) {
    const file = event.target.files[0];
    if (!file) {
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        const profile = getSavedProfile();
        profile.profilePic = e.target.result;
        localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(profile));
        hydrateProfileUI();
        const msg = document.getElementById('profileMessage');
        if (msg) {
            msg.style.color = 'var(--dark-green)';
            msg.textContent = 'Profile photo updated.';
        }
    };
    reader.readAsDataURL(file);
}

function triggerProfilePhotoPicker() {
    const input = document.getElementById('profilePicInput');
    if (input) {
        input.click();
    }
}

function hydrateProfileUI() {
    const username = (localStorage.getItem('username') || '').trim();
    const profile = getSavedProfile();
    const profilePic = document.getElementById('profilePicPreview');
    const displayFullName = document.getElementById('displayFullName');
    const displayPhone = document.getElementById('displayPhone');
    const profFullName = document.getElementById('profFullName');
    const profPhone = document.getElementById('profPhone');

    if (profilePic) {
        profilePic.src = profile.profilePic || DEFAULT_PROFILE_AVATAR;
    }
    if (displayFullName) {
        displayFullName.textContent = profile.fullName || username || 'Guest User';
    }
    if (displayPhone) {
        displayPhone.textContent = profile.phone || 'Phone not added';
    }
    if (profFullName) {
        profFullName.value = profile.fullName || username;
    }
    if (profPhone) {
        profPhone.value = profile.phone || '';
    }

    renderNavLinks();
}

function openProfile() {
    if (!localStorage.getItem('username')) {
        globalThis.location.href = 'login.html';
        return;
    }

    hydrateProfileUI();
    toggleEditProfile(false);

    const modal = document.getElementById('profileModal');
    if (modal) {
        modal.style.display = 'block';
    }
}

function closeProfile() {
    const modal = document.getElementById('profileModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function toggleEditProfile(forceEdit) {
    const viewMode = document.getElementById('profileViewMode');
    const form = document.getElementById('profileForm');
    const profileMessage = document.getElementById('profileMessage');
    if (!viewMode || !form) {
        return;
    }

    const shouldEdit = typeof forceEdit === 'boolean' ? forceEdit : form.style.display === 'none';
    viewMode.style.display = shouldEdit ? 'none' : 'block';
    form.style.display = shouldEdit ? 'block' : 'none';
    if (profileMessage) {
        profileMessage.textContent = '';
    }
}

function logoutUser() {
    localStorage.removeItem('username');
    globalThis.location.href = 'login.html';
}

globalThis.onclick = function (event) {
    const modal = document.getElementById('profileModal');
    if (modal && event.target === modal) {
        closeProfile();
    }
};

globalThis.uploadImage = uploadImage;
globalThis.openImagePicker = openImagePicker;
globalThis.analyzeSelectedImage = analyzeSelectedImage;
globalThis.reanalyzeLastImage = reanalyzeLastImage;
globalThis.openProfile = openProfile;
globalThis.closeProfile = closeProfile;
globalThis.triggerProfilePhotoPicker = triggerProfilePhotoPicker;
globalThis.toggleEditProfile = toggleEditProfile;
globalThis.logoutUser = logoutUser;
