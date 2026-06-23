package com.pridepin.pridepin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/v1/auth/login. */
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
