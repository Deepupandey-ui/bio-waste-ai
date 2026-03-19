// 📸 Image Preview
const input = document.getElementById("imageInput");
const preview = document.getElementById("preview");
const resultDiv = document.getElementById("result");

input.addEventListener("change", function () {
    const file = this.files[0];

    if (file) {
        const reader = new FileReader();

        reader.onload = function (e) {
            preview.src = e.target.result;
            preview.style.display = "block";
        };

        reader.readAsDataURL(file);

        // Reset result when new image selected
        resultDiv.innerHTML = "";
    }
});


// 🚀 Analyze Function
async function analyze() {
    const file = input.files[0];

    if (!file) {
        resultDiv.innerHTML = "❌ Please upload an image first";
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    // Loading UI
    resultDiv.innerHTML = "⏳ Uploading & analyzing...";

    try {
        const response = await fetch("http://127.0.0.1:8080/analyze-image", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error("Server error");
        }

        const data = await response.json();

        // 🎯 MAIN RESULT UI
        resultDiv.innerHTML = `
            <div class="result-card">
                <h2>📊 Analysis Result</h2>

                <p><strong>Waste Level:</strong> ${data.waste}</p>
                <p><strong>Best Use:</strong> ${data.use}</p>
                <p><strong>Products:</strong> ${data.products}</p>
                <p><strong>Profit:</strong> <span class="profit">${data.profit}</span></p>
                <p><strong>CO₂ Saved:</strong> ${data.co2}</p>
            </div>
        `;

        // 📊 Progress Bar
        const wastePercent = parseInt(data.waste);

        resultDiv.innerHTML += `
            <div class="progress-bar">
                <div class="progress" style="width: ${wastePercent}%"></div>
            </div>
        `;

        // 📍 Nearby Buyers
        const buyers = [
            "ABC Biofuel Plant (5 km)",
            "XYZ Compost Unit (3 km)",
            "Green Energy Ltd (7 km)"
        ];

        let buyerHTML = `
            <div class="buyer-card">
                <h3>📍 Nearby Buyers</h3>
                <ul>
        `;

        buyers.forEach(b => {
            buyerHTML += `<li>${b}</li>`;
        });

        buyerHTML += `</ul></div>`;

        resultDiv.innerHTML += buyerHTML;

        // 🤖 Smart Recommendation
        let suggestion = "";

        if (parseInt(data.waste) > 50) {
            suggestion = "🔥 Best for Biofuel production";
        } else {
            suggestion = "🌱 Best for Composting";
        }

        resultDiv.innerHTML += `
            <div class="recommendation-card">
                <p><strong>Recommendation:</strong> ${suggestion}</p>
            </div>
        `;
        resultDiv.innerHTML += `
          <p style="margin-top:10px;">⚡ Efficiency Level</p>
              <div class="progress-bar">
            <div class="progress" style="width: ${wastePercent}%"></div>
         </div>
       `;

    } catch (error) {
        console.error(error);

        resultDiv.innerHTML = `
            <div class="error-card">
                ❌ Failed to connect backend
            </div>
        `;
    }
}