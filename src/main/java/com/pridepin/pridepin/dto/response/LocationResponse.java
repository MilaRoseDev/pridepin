package com.pridepin.pridepin.dto.response;

import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.SafetyTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/** Location data returned by list/get/create/update; includes computed averageRating and reviewCount. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    private UUID id;
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private LocationCategory category;
    private String address;
    private String addedByUsername;
    private UUID addedById;
    private Set<SafetyTag> tags;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
