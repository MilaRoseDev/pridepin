package com.pridepin.pridepin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/v1/auth/resend-verification. */
@Data
public class ResendVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
}
