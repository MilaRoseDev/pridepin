package com.pridepin.pridepin.dto.response;

import com.pridepin.pridepin.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** User profile data returned by auth, profile, and admin endpoints. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}
