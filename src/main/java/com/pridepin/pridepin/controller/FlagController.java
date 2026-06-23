package com.pridepin.pridepin.controller;

import com.pridepin.pridepin.dto.request.FlagRequest;
import com.pridepin.pridepin.dto.request.ResolveFlagRequest;
import com.pridepin.pridepin.dto.response.FlagResponse;
import com.pridepin.pridepin.enums.FlagStatus;
import com.pridepin.pridepin.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Flag (report) endpoints.
 * Any authenticated user can submit a flag against a location.
 * Admin-only endpoints live under /flags/admin.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Flags", description = "Location reporting and admin moderation queue")
@SecurityRequirement(name = "bearerAuth")
public class FlagController {

    private final FlagService flagService;

    /** Submits a flag against a location. One open flag per user per location is enforced. */
    @PostMapping("/api/v1/locations/{locationId}/flags")
    @Operation(summary = "Flag a location for admin review")
    public ResponseEntity<FlagResponse> submitFlag(
            @PathVariable UUID locationId,
            @Valid @RequestBody FlagRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flagService.submitFlag(locationId, request, userDetails.getUsername()));
    }

    /** Returns a paginated list of flags filtered by status. Defaults to OPEN. ADMIN only. */
    @GetMapping("/api/v1/flags/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List flags by status — ADMIN only")
    public ResponseEntity<Page<FlagResponse>> listFlags(
            @RequestParam(required = false) FlagStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(flagService.listFlags(status, pageable));
    }

    /** Resolves a flag: DISMISS (no action on location) or DEACTIVATE (soft-delete location). ADMIN only. */
    @PutMapping("/api/v1/flags/admin/{flagId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve a flag — ADMIN only")
    public ResponseEntity<FlagResponse> resolveFlag(
            @PathVariable UUID flagId,
            @Valid @RequestBody ResolveFlagRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(flagService.resolveFlag(flagId, request, userDetails.getUsername()));
    }
}
