package com.pridepin.pridepin.service;

import com.pridepin.pridepin.dto.request.LocationRequest;
import com.pridepin.pridepin.dto.response.LocationResponse;
import com.pridepin.pridepin.entity.Location;
import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.Role;
import com.pridepin.pridepin.enums.SafetyTag;
import com.pridepin.pridepin.exception.ResourceNotFoundException;
import com.pridepin.pridepin.exception.UnauthorizedException;
import com.pridepin.pridepin.repository.LocationRepository;
import com.pridepin.pridepin.repository.ReviewRepository;
import com.pridepin.pridepin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

/**
 * Location CRUD: list/get are read-only; create/update/delete enforce ownership or ADMIN.
 * Location responses include computed average rating and review count.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /** Returns a paginated list of active locations, optionally filtered by category and/or safety tag. */
    @Transactional(readOnly = true)
    public Page<LocationResponse> getAllLocations(LocationCategory category, SafetyTag tag, Pageable pageable) {
        Page<Location> page;
        if (category != null && tag != null) {
            page = locationRepository.findAllByActiveTrueAndCategoryAndTag(category, tag, pageable);
        } else if (category != null) {
            page = locationRepository.findAllByActiveTrueAndCategory(category, pageable);
        } else if (tag != null) {
            page = locationRepository.findAllByActiveTrueAndTag(tag, pageable);
        } else {
            page = locationRepository.findAllByActiveTrue(pageable);
        }
        return page.map(this::toResponse);
    }

    /** Returns a single active location by ID, or throws if not found. */
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(UUID id) {
        Location location = locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        return toResponse(location);
    }

    /** Creates a new location with the authenticated user as the owner. */
    @Transactional
    public LocationResponse createLocation(LocationRequest request, String username) {
        User user = findActiveUser(username);

        Location location = Location.builder()
                .name(request.getName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .category(request.getCategory())
                .address(request.getAddress())
                .tags(request.getTags() != null ? new HashSet<>(request.getTags()) : new HashSet<>())
                .addedBy(user)
                .active(true)
                .build();

        return toResponse(locationRepository.save(location));
    }

    /** Updates the location. Allowed only for the user who added it or ADMIN. */
    @Transactional
    public LocationResponse updateLocation(UUID id, LocationRequest request, String username) {
        Location location = locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        assertOwnerOrAdmin(location.getAddedBy(), username, "update this location");

        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setCategory(request.getCategory());
        location.setAddress(request.getAddress());
        location.setTags(request.getTags() != null ? new HashSet<>(request.getTags()) : new HashSet<>());

        return toResponse(locationRepository.save(location));
    }

    /** Soft-deletes the location. Allowed only for the user who added it or ADMIN. */
    @Transactional
    public void deleteLocation(UUID id, String username) {
        Location location = locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        assertOwnerOrAdmin(location.getAddedBy(), username, "delete this location");

        location.setActive(false);
        locationRepository.save(location);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Finds an active user by username or throws ResourceNotFoundException. */
    private User findActiveUser(String username) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /** Throws UnauthorizedException if the requesting user is neither the owner nor an ADMIN. */
    private void assertOwnerOrAdmin(User owner, String requestingUsername, String action) {
        User requesting = findActiveUser(requestingUsername);
        boolean isOwner = owner.getId().equals(requesting.getId());
        boolean isAdmin = requesting.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorised to " + action);
        }
    }

    /** Maps Location entity to LocationResponse DTO, including average rating and review count from the DB. */
    private LocationResponse toResponse(Location location) {
        Double avgRating = reviewRepository.findAverageRatingByLocationId(location.getId());
        Long reviewCount = reviewRepository.countActiveByLocationId(location.getId());

        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .description(location.getDescription())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .category(location.getCategory())
                .address(location.getAddress())
                .tags(location.getTags() != null ? location.getTags() : new HashSet<>())
                .addedByUsername(location.getAddedBy().getUsername())
                .addedById(location.getAddedBy().getId())
                .averageRating(avgRating)
                .reviewCount(reviewCount != null ? reviewCount : 0L)
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
