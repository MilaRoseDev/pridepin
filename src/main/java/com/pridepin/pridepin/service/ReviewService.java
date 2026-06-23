package com.pridepin.pridepin.service;

import com.pridepin.pridepin.dto.request.ReviewRequest;
import com.pridepin.pridepin.dto.response.ReviewResponse;
import com.pridepin.pridepin.entity.Location;
import com.pridepin.pridepin.entity.Review;
import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.enums.Role;
import com.pridepin.pridepin.exception.ResourceNotFoundException;
import com.pridepin.pridepin.exception.UnauthorizedException;
import com.pridepin.pridepin.exception.UserAlreadyExistsException;
import com.pridepin.pridepin.repository.LocationRepository;
import com.pridepin.pridepin.repository.ReviewRepository;
import com.pridepin.pridepin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Review CRUD: one active review per user per location. Create/update/delete enforce ownership or ADMIN.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    /** Returns a paginated list of active reviews for the location. Location must exist and be active. */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByLocation(UUID locationId, Pageable pageable) {
        locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        return reviewRepository.findAllByLocationIdAndActiveTrue(locationId, pageable)
                .map(this::toResponse);
    }

    /** Creates a review for the location. Throws if the user already has an active review for this location. */
    @Transactional
    public ReviewResponse createReview(UUID locationId, ReviewRequest request, String username) {
        Location location = locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        User user = findActiveUser(username);

        if (reviewRepository.existsByLocationIdAndUserIdAndActiveTrue(locationId, user.getId())) {
            throw new UserAlreadyExistsException("You have already reviewed this location");
        }

        Review review = Review.builder()
                .location(location)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .active(true)
                .build();

        return toResponse(reviewRepository.save(review));
    }

    /** Updates rating and comment. Allowed only for the review owner or ADMIN. */
    @Transactional
    public ReviewResponse updateReview(UUID locationId, UUID reviewId, ReviewRequest request, String username) {
        locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        Review review = reviewRepository.findByIdAndLocationIdAndActiveTrue(reviewId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        assertOwnerOrAdmin(review.getUser(), username, "update this review");

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(reviewRepository.save(review));
    }

    /** Soft-deletes the review. Allowed only for the review owner or ADMIN. */
    @Transactional
    public void deleteReview(UUID locationId, UUID reviewId, String username) {
        locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        Review review = reviewRepository.findByIdAndLocationIdAndActiveTrue(reviewId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        assertOwnerOrAdmin(review.getUser(), username, "delete this review");

        review.setActive(false);
        reviewRepository.save(review);
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

    /** Maps Review entity to ReviewResponse DTO. */
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .locationId(review.getLocation().getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
