package com.pridepin.pridepin.dto.response;

import com.pridepin.pridepin.enums.FlagReason;
import com.pridepin.pridepin.enums.FlagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Flag data returned to the reporter (on submit) and to admins (on list/resolve). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagResponse {

    private UUID id;
    private UUID locationId;
    private String locationName;
    private UUID reporterId;
    private String reporterUsername;
    private FlagReason reason;
    private String note;
    private FlagStatus status;
    private UUID resolvedById;
    private String resolvedByUsername;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
