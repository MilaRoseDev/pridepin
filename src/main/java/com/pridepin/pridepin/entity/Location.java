package com.pridepin.pridepin.entity;

import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.SafetyTag;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A safe-space location: name, description, coordinates, category, address, and the user who added it.
 * Soft-deleted via active = false; auditing fills createdAt/updatedAt.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LocationCategory category;

    @Column(length = 500)
    private String address;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "location_safety_tags", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "tag", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<SafetyTag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by", nullable = false)
    private User addedBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
