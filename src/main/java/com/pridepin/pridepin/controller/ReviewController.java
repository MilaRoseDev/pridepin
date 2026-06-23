package com.pridepin.pridepin.controller;

import com.pridepin.pridepin.dto.request.ReviewRequest;
import com.pridepin.pridepin.dto.response.MessageResponse;
import com.pridepin.pridepin.dto.response.ReviewResponse;
import com.pridepin.pridepin.service.ReviewService;
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
 * CRUD for reviews under a location. Listing is public; create/update/delete require JWT.
 * One review per user per location; update/delete allowed for review owner or ADMIN.
 */
@RestController
@RequestMapping("/api/v1/locations/{locationId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Location reviews and star ratings")
public class ReviewController {

    private final ReviewService reviewService;

    /** Returns a paginated list of active reviews for the given location. Public. */
    @GetMapping
    @Operation(summary = "List all reviews for a location (public)")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByLocation(
            @PathVariable UUID locationId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByLocation(locationId, pageable));
    }

    /** Creates a review for the location. One per user per location; requires auth. */
    @PostMapping
    @Operation(
        summary  = "Submit a review for a location (one per user per location)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID locationId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(locationId, request, userDetails.getUsername()));
    }

    /** Updates an existing review. Allowed for review owner or ADMIN. */
    @PutMapping("/{reviewId}")
    @Operation(
        summary  = "Update a review (owner or ADMIN)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID locationId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reviewService.updateReview(locationId, reviewId, request, userDetails.getUsername()));
    }

    /** Soft-deletes a review. Allowed for review owner or ADMIN. */
    @DeleteMapping("/{reviewId}")
    @Operation(
        summary  = "Soft-delete a review (owner or ADMIN)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MessageResponse> deleteReview(
            @PathVariable UUID locationId,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.deleteReview(locationId, reviewId, userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Review deleted successfully"));
    }
}
