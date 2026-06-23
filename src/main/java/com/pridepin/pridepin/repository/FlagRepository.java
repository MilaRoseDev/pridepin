package com.pridepin.pridepin.repository;

import com.pridepin.pridepin.entity.Flag;
import com.pridepin.pridepin.enums.FlagStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Flag entities.
 */
@Repository
public interface FlagRepository extends JpaRepository<Flag, UUID> {

    /** Returns true if the user already has an open flag against this location. */
    boolean existsByLocationIdAndReporterIdAndStatus(UUID locationId, UUID reporterId, FlagStatus status);

    /** Paginated list of flags filtered by status, ordered by creation date ascending. */
    Page<Flag> findAllByStatusOrderByCreatedAtAsc(FlagStatus status, Pageable pageable);

    /** Single active flag by ID, or empty if not found or soft-deleted. */
    Optional<Flag> findByIdAndActiveTrue(UUID id);
}
