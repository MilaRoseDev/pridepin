package com.pridepin.pridepin.controller;

import com.pridepin.pridepin.dto.request.LoginRequest;
import com.pridepin.pridepin.dto.request.RegisterRequest;
import com.pridepin.pridepin.dto.request.ResendVerificationRequest;
import com.pridepin.pridepin.dto.response.AuthResponse;
import com.pridepin.pridepin.dto.response.MessageResponse;
import com.pridepin.pridepin.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for authentication: registration, login, email verification, and resend verification.
 * All endpoints under /api/v1/auth are public (no JWT required).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and email verification")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user. If email verification is enabled, returns a message and no token;
     * otherwise returns a JWT and user info immediately.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates the user and returns a JWT plus user info. Fails if unverified (when verification is enabled).
     */
    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Handles the verification link from the registration email. Marks the user verified and returns an HTML success page.
     */
    @GetMapping("/verify")
    @Operation(summary = "Verify email address via link sent in the registration email")
    public ResponseEntity<String> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(verifiedHtmlPage());
    }

    /**
     * Resends a verification email if the address is registered and unverified. Always returns the same message to avoid email enumeration.
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend a verification email (safe — always returns the same message)")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request.getEmail()));
    }

    // ── Inline HTML for the verify success page ────────────────────────────

    /** Returns the HTML page shown after successful email verification (with link back to the app). */
    private String verifiedHtmlPage() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>Email Verified — PridePin</title>
              <style>
                body { margin:0; background:#0d0d1a; font-family:'Segoe UI',sans-serif;
                       display:flex; align-items:center; justify-content:center; min-height:100vh; }
                .card { background:#120020; border:1px solid #3a1060; border-radius:16px;
                        padding:3rem 2.5rem; text-align:center; max-width:420px; }
                .icon { font-size:3.5rem; margin-bottom:1rem; }
                h1   { color:#c77dff; font-size:1.5rem; margin:0 0 0.75rem; }
                p    { color:#b090d0; line-height:1.6; margin:0 0 1.5rem; }
                a    { display:inline-block; padding:0.75rem 2rem; background:#7b2d8b;
                       color:#fff; text-decoration:none; border-radius:8px; font-weight:700; }
                a:hover { background:#9b3dab; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="icon">🏳️‍⚧️</div>
                <h1>Email Verified!</h1>
                <p>Your PridePin account is now active. You can log in and start exploring safe spaces.</p>
                <a href="/">Go to PridePin</a>
              </div>
            </body>
            </html>
            """;
    }
}
