package com.pridepin.pridepin.dto.request;

import com.pridepin.pridepin.enums.FlagResolution;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for resolving a flag (PUT /api/v1/flags/admin/{id}/resolve). */
@Data
public class ResolveFlagRequest {

    @NotNull(message = "Resolution action is required")
    private FlagResolution action;
}
