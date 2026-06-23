package com.pridepin.pridepin.repository;

import com.pridepin.pridepin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for User entities. Query methods use "active = true" to respect soft-deletes.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Finds an active user by username (used for login and SecurityContext). */
    Optional<User> findByUsernameAndActiveTrue(String username);

    /** Finds an active user by email (e.g. for resend verification). */
    Optional<User> findByEmailAndActiveTrue(String email);

    /** Finds an active user by ID. */
    Optional<User> findByIdAndActiveTrue(UUID id);

    /** True if any user (active or not) has this username. */
    boolean existsByUsername(String username);

    /** True if any user (active or not) has this email. */
    boolean existsByEmail(String email);

    /** Finds user by the email verification token (for the verify link). */
    Optional<User> findByVerificationToken(String verificationToken);
}
