package com.pridepin.pridepin.repository;

import com.pridepin.pridepin.entity.Location;
import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.SafetyTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Location entities. All read methods filter by active = true (soft-delete).
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    /** Paginated list of all active locations. */
    Page<Location> findAllByActiveTrue(Pageable pageable);

    /** Paginated list of active locations in the given category. */
    Page<Location> findAllByActiveTrueAndCategory(LocationCategory category, Pageable pageable);

    /** Paginated list of active locations carrying the given safety tag. */
    @Query("SELECT l FROM Location l WHERE l.active = true AND :tag MEMBER OF l.tags")
    Page<Location> findAllByActiveTrueAndTag(@Param("tag") SafetyTag tag, Pageable pageable);

    /** Paginated list of active locations in the given category carrying the given safety tag. */
    @Query("SELECT l FROM Location l WHERE l.active = true AND l.category = :category AND :tag MEMBER OF l.tags")
    Page<Location> findAllByActiveTrueAndCategoryAndTag(
            @Param("category") LocationCategory category,
            @Param("tag") SafetyTag tag,
            Pageable pageable);

    /** Single active location by ID, or empty if not found or inactive. */
    Optional<Location> findByIdAndActiveTrue(UUID id);
}
