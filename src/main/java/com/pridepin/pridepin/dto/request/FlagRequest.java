package com.pridepin.pridepin.dto.request;

import com.pridepin.pridepin.enums.FlagReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for submitting a location flag (POST /api/v1/locations/{id}/flags). */
@Data
public class FlagRequest {

    @NotNull(message = "Reason is required")
    private FlagReason reason;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}
