package com.pridepin.pridepin.entity;

import com.pridepin.pridepin.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User account: credentials, role, email verification, and soft-delete flag.
 * Auditing fills createdAt/updatedAt; password must be stored encoded (BCrypt).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Soft-delete flag. Lombok generates isActive() / setActive(). */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** False until the user clicks the link in their verification email. */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    /** One-time token e-mailed to the user on registration. Cleared after use. */
    @Column(name = "verification_token", length = 128)
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "addedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Location> locations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
