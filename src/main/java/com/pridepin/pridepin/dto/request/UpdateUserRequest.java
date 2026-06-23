package com.pridepin.pridepin.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for PUT /api/v1/users/me. All fields are optional; only supplied fields are updated. */
@Data
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, numbers, and underscores")
    private String username;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
