package com.pridepin.pridepin.repository;

import com.pridepin.pridepin.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Review entities. Read methods filter by active = true; one active review per user per location is enforced in the service layer.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Paginated list of active reviews for a location. */
    Page<Review> findAllByLocationIdAndActiveTrue(UUID locationId, Pageable pageable);

    /** Single active review by ID and location (for update/delete). */
    Optional<Review> findByIdAndLocationIdAndActiveTrue(UUID id, UUID locationId);

    /** True if this user already has an active review for this location (allows re-review after soft delete). */
    boolean existsByLocationIdAndUserIdAndActiveTrue(UUID locationId, UUID userId);

    /** Average star rating for active reviews on the location (null if none). */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.location.id = :locationId AND r.active = true")
    Double findAverageRatingByLocationId(@Param("locationId") UUID locationId);

    /** Count of active reviews for the location. */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.location.id = :locationId AND r.active = true")
    Long countActiveByLocationId(@Param("locationId") UUID locationId);
}
