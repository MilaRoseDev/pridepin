package com.pridepin.pridepin.controller;

import com.pridepin.pridepin.dto.request.UpdateUserRequest;
import com.pridepin.pridepin.dto.response.MessageResponse;
import com.pridepin.pridepin.dto.response.UserResponse;
import com.pridepin.pridepin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User profile and admin endpoints. All require JWT except as noted in SecurityConfig.
 * /me endpoints operate on the authenticated user; /admin/* require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ── Own profile ────────────────────────────────────────────────────────

    /** Returns the profile of the currently authenticated user. */
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(userDetails.getUsername()));
    }

    /** Updates the authenticated user's profile; only provided fields are changed. */
    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile (all fields optional)")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updateCurrentUser(request, userDetails.getUsername()));
    }

    /** Soft-deletes the authenticated user's account (sets active = false). */
    @DeleteMapping("/me")
    @Operation(summary = "Soft-delete the authenticated user's account")
    public ResponseEntity<MessageResponse> deleteCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Account deleted successfully"));
    }

    // ── Admin endpoints ────────────────────────────────────────────────────

    /** Returns all users (including inactive). Restricted to ADMIN. */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users — ADMIN only")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** Returns a single user by ID. Restricted to ADMIN. */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by ID — ADMIN only")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** Soft-deletes a user by ID. Restricted to ADMIN. */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete any user by ID — ADMIN only")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }
}
