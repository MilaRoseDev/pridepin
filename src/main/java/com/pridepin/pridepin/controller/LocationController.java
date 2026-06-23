package com.pridepin.pridepin.controller;

import com.pridepin.pridepin.dto.request.LocationRequest;
import com.pridepin.pridepin.dto.response.LocationResponse;
import com.pridepin.pridepin.dto.response.MessageResponse;
import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.SafetyTag;
import com.pridepin.pridepin.service.LocationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CRUD for safe-space locations. GET list and GET by ID are public; create/update/delete require JWT.
 * Update/delete allowed for the user who added the location or ADMIN.
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Safe-space location management")
public class LocationController {

    private final LocationService locationService;

    /** Returns a paginated list of active locations, optionally filtered by category and/or safety tag. Public. */
    @GetMapping
    @Operation(summary = "List all active locations (public), optionally filtered by category and/or safety tag")
    public ResponseEntity<Page<LocationResponse>> getAllLocations(
            @RequestParam(required = false) LocationCategory category,
            @RequestParam(required = false) SafetyTag tag,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(locationService.getAllLocations(category, tag, pageable));
    }

    /** Returns a single active location by ID. Public. */
    @GetMapping("/{id}")
    @Operation(summary = "Get a single location by ID (public)")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    /** Creates a new location; the authenticated user is set as the owner. */
    @PostMapping
    @Operation(
        summary  = "Add a new safe-space location",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<LocationResponse> createLocation(
            @Valid @RequestBody LocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createLocation(request, userDetails.getUsername()));
    }

    /** Updates a location. Allowed for the user who added it or ADMIN. */
    @PutMapping("/{id}")
    @Operation(
        summary  = "Update a location (owner or ADMIN)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody LocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(locationService.updateLocation(id, request, userDetails.getUsername()));
    }

    /** Soft-deletes a location. Allowed for the user who added it or ADMIN. */
    @DeleteMapping("/{id}")
    @Operation(
        summary  = "Soft-delete a location (owner or ADMIN)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MessageResponse> deleteLocation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        locationService.deleteLocation(id, userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Location deleted successfully"));
    }
}
