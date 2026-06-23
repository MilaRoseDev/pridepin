package com.pridepin.pridepin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Simple JSON wrapper for a single message string (e.g. "Account deleted successfully"). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
