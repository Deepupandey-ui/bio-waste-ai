package com.biowaste.backend.controller;

import com.biowaste.backend.model.User;
import com.biowaste.backend.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    // ─── SIGNUP ───────────────────────────────────────────────────────────────
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email    = request.get("email");

        if (username == null || password == null || email == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Username, email and password are required"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Username already exists!"));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Email already registered!"));
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(username, hashedPassword);
        user.setEmail(email);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && BCrypt.checkpw(password, userOpt.get().getPassword())) {
            return ResponseEntity.ok(Map.of("message", "Login successful", "username", username));
        } else {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Invalid username or password"));
        }
    }

    // ─── GET PROFILE ──────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam("username") String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "fullName",   u.getFullName()   != null ? u.getFullName()   : "",
                "phone",      u.getPhone()      != null ? u.getPhone()      : "",
                "profilePic", u.getProfilePic() != null ? u.getProfilePic() : "",
                "email",      u.getEmail()      != null ? u.getEmail()      : ""
            ));
        }
        return ResponseEntity.status(404).body(Map.of("message", "User not found"));
    }

    // ─── UPDATE PROFILE ───────────────────────────────────────────────────────
    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (request.containsKey("fullName"))   u.setFullName(request.get("fullName"));
            if (request.containsKey("phone"))       u.setPhone(request.get("phone"));
            if (request.containsKey("profilePic")) u.setProfilePic(request.get("profilePic"));
            if (request.containsKey("email"))       u.setEmail(request.get("email"));

            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        }
        return ResponseEntity.status(404).body(Map.of("message", "User not found"));
    }

    // ─── STEP 1: FORGOT PASSWORD — Send OTP ───────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Return a generic OK to prevent email enumeration attacks
            return ResponseEntity.ok(Map.of("message", "If this email is registered, you will receive an OTP."));
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        User user = userOpt.get();
        user.setOtpCode(otp);
        user.setOtpExpiry(expiry);
        userRepository.save(user);

        // Send OTP via Email
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("prasadnikhil4333@gmail.com");
            mail.setTo(email);
            mail.setSubject("🌱 BioWaste AI - Password Reset OTP");
            mail.setText(
                "Hello " + user.getUsername() + ",\n\n" +
                "You requested a password reset for your BioWaste AI account.\n\n" +
                "Your One-Time Password (OTP) is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for 5 minutes. Do not share it with anyone.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— BioWaste Intelligence Platform\n" +
                "Supported by the Swachh Bharat Mission 🇮🇳"
            );
            mailSender.send(mail);
            System.out.println("[OTP EMAIL SENT] " + email + " → " + otp);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] " + e.getMessage());
            // Still log OTP to console if email fails
            System.out.println("[OTP FALLBACK - CONSOLE] " + email + " → " + otp);
        }

        return ResponseEntity.ok(Map.of("message", "OTP sent to your registered email address."));
    }

    // ─── STEP 2: VERIFY OTP ───────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp   = request.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Email not found"));
        }

        User user = userOpt.get();

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid OTP. Please try again."));
        }

        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return ResponseEntity.status(400).body(Map.of("message", "OTP has expired. Please request a new one."));
        }

        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    // ─── STEP 3: RESET PASSWORD ───────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email       = request.get("email");
        String otp         = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Email, OTP and new password are required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Email not found"));
        }

        User user = userOpt.get();

        // Double-check OTP is still valid
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid OTP"));
        }
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return ResponseEntity.status(400).body(Map.of("message", "OTP has expired"));
        }

        // Hash and save new password, clear OTP
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully! You can now login."));
    }

    // ─── GOOGLE SIGN-IN ───────────────────────────────────────────────────────
    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID token is required"));
        }

        try {
            // Verify token with Google
            RestTemplate rest = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            @SuppressWarnings("unchecked")
            Map<String, Object> googleData = rest.getForObject(url, Map.class);

            if (googleData == null || !googleData.containsKey("email")) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid Google token"));
            }

            // Verify the token was issued for our app
            String aud = (String) googleData.get("aud");
            if (!"872276893176-49e7ikbt0qcl33e9cirv0tja1n3dinio.apps.googleusercontent.com".equals(aud)) {
                return ResponseEntity.status(401).body(Map.of("message", "Token not issued for this app"));
            }

            String email   = (String) googleData.get("email");
            String name    = (String) googleData.getOrDefault("name", "");
            String picture = (String) googleData.getOrDefault("picture", "");

            // Check if user already exists by email
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;

            if (userOpt.isPresent()) {
                // Existing user — just login
                user = userOpt.get();
                // Update profile pic from Google if not already set
                if (user.getProfilePic() == null || user.getProfilePic().isEmpty()) {
                    user.setProfilePic(picture);
                    userRepository.save(user);
                }
            } else {
                // New user — auto-register
                String username = email.split("@")[0]; // use email prefix as username
                // If username already taken, add random suffix
                if (userRepository.findByUsername(username).isPresent()) {
                    username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
                }
                String randomPassword = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12));
                user = new User(username, randomPassword);
                user.setEmail(email);
                user.setFullName(name);
                user.setProfilePic(picture);
                userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                "message", "Google login successful",
                "username", user.getUsername(),
                "email", email,
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "profilePic", user.getProfilePic() != null ? user.getProfilePic() : ""
            ));

        } catch (Exception e) {
            System.err.println("[GOOGLE AUTH ERROR] " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "Google authentication failed: " + e.getMessage()));
        }
    }

    // ─── GOOGLE SIGN-IN (Access Token Flow) ───────────────────────────────────
    @PostMapping("/google-direct")
    public ResponseEntity<?> googleDirectSignIn(@RequestBody Map<String, String> request) {
        String accessToken = request.get("accessToken");
        String email       = request.get("email");
        String name        = request.get("name");
        String picture     = request.get("picture");

        if (accessToken == null || email == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Access token and email are required"));
        }

        try {
            // Verify the access token is valid by calling Google's userinfo endpoint
            RestTemplate rest = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> googleData = rest.getForObject(
                "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken,
                Map.class
            );

            if (googleData == null || !email.equals(googleData.get("email"))) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid Google token"));
            }

            // Check if user exists
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;

            if (userOpt.isPresent()) {
                user = userOpt.get();
                if ((user.getProfilePic() == null || user.getProfilePic().isEmpty()) && picture != null) {
                    user.setProfilePic(picture);
                    userRepository.save(user);
                }
            } else {
                String username = email.split("@")[0];
                if (userRepository.findByUsername(username).isPresent()) {
                    username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
                }
                String randomPassword = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12));
                user = new User(username, randomPassword);
                user.setEmail(email);
                user.setFullName(name != null ? name : "");
                user.setProfilePic(picture != null ? picture : "");
                userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                "message", "Google login successful",
                "username", user.getUsername(),
                "email", email,
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "profilePic", user.getProfilePic() != null ? user.getProfilePic() : ""
            ));

        } catch (Exception e) {
            System.err.println("[GOOGLE DIRECT AUTH ERROR] " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "Google authentication failed: " + e.getMessage()));
        }
    }
}
