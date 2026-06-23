package com.pridepin.pridepin.service;

import com.pridepin.pridepin.dto.request.LoginRequest;
import com.pridepin.pridepin.dto.request.RegisterRequest;
import com.pridepin.pridepin.dto.response.AuthResponse;
import com.pridepin.pridepin.dto.response.MessageResponse;
import com.pridepin.pridepin.dto.response.UserResponse;
import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.enums.Role;
import com.pridepin.pridepin.exception.BadRequestException;
import com.pridepin.pridepin.exception.ResourceNotFoundException;
import com.pridepin.pridepin.exception.UserAlreadyExistsException;
import com.pridepin.pridepin.repository.UserRepository;
import com.pridepin.pridepin.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles registration, login, email verification, and resend verification.
 * When email verification is enabled, unverified users cannot log in (enforced via UserDetailsServiceImpl).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final MailService mailService;

    @Value("${app.email-verification.enabled:false}")
    private boolean emailVerificationEnabled;

    // ── Register ───────────────────────────────────────────────────────────

    /**
     * Registers a new user. Rejects if username or email already exists. If email verification is off,
     * user is auto-verified and a JWT is returned; otherwise a verification email is sent and a message is returned.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        boolean autoVerify = !emailVerificationEnabled;

        User.UserBuilder builder = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .verified(autoVerify);

        if (!autoVerify) {
            String token = generateVerificationToken();
            builder
                .verificationToken(token)
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        }

        User user = userRepository.save(builder.build());

        if (!autoVerify) {
            mailService.sendVerificationEmail(user.getEmail(), user.getUsername(), user.getVerificationToken());
            return AuthResponse.builder()
                    .message("Registration successful! Please check your email to verify your account.")
                    .user(toUserResponse(user))
                    .build();
        }

        // Auto-verified (dev mode) — issue JWT immediately
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userDetails))
                .user(toUserResponse(user))
                .build();
    }

    // ── Login ──────────────────────────────────────────────────────────────

    /**
     * Authenticates via Spring's AuthenticationManager (which uses UserDetailsServiceImpl; unverified users are disabled).
     * On success, loads user and returns JWT plus user DTO.
     */
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager calls UserDetailsServiceImpl, which sets enabled=isVerified().
        // If unverified, Spring Security throws DisabledException before we get here.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsernameAndActiveTrue(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userDetails))
                .user(toUserResponse(user))
                .build();
    }

    // ── Verify email ───────────────────────────────────────────────────────

    /**
     * Consumes the verification token from the email link: finds user by token, checks expiry,
     * marks verified and clears token, then sends a welcome email. Idempotent if already verified.
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link."));

        if (user.isVerified()) {
            return; // idempotent — already verified is fine
        }

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "This verification link has expired. Please request a new one.");
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        mailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        log.info("User {} verified their email", user.getUsername());
    }

    // ── Resend verification ────────────────────────────────────────────────

    /**
     * If the email belongs to an active, unverified user, generates a new token and sends a verification email.
     * Always returns the same message to prevent email enumeration.
     */
    @Transactional
    public MessageResponse resendVerification(String email) {
        // Always return the same message to avoid email enumeration
        String genericMessage = "If that email is registered and unverified, a new link has been sent.";

        userRepository.findByEmailAndActiveTrue(email).ifPresent(user -> {
            if (!user.isVerified()) {
                String token = generateVerificationToken();
                user.setVerificationToken(token);
                user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
                userRepository.save(user);
                mailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
            }
        });

        return new MessageResponse(genericMessage);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Generates a random hex string used as the email verification token. */
    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "")
             + UUID.randomUUID().toString().replace("-", "");
    }

    /** Maps a User entity to the API response DTO. */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
