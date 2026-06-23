package com.pridepin.pridepin.dto.request;

import com.pridepin.pridepin.enums.LocationCategory;
import com.pridepin.pridepin.enums.SafetyTag;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

/** Request body for creating or updating a location (POST/PUT /api/v1/locations). */
@Data
public class LocationRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Category is required")
    private LocationCategory category;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    /** Optional safety tags; null is treated as an empty set. */
    private Set<SafetyTag> tags;
}
