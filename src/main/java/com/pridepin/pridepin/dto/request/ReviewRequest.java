package com.pridepin.pridepin.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for creating or updating a review (POST/PUT .../reviews). Rating 1–5; comment optional. */
@Data
public class ReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
