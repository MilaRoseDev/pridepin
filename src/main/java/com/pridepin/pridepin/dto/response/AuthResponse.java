package com.pridepin.pridepin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for login and register. When email verification is required and user is unverified,
 * message is set and token may be null; otherwise token and user are set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserResponse user;

    /** Populated instead of token when email verification is required. */
    private String message;
}
