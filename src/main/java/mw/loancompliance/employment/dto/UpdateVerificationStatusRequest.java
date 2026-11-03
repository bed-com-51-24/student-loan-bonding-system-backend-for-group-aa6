package mw.loancompliance.employment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateVerificationStatusRequest(
        @NotBlank
        @Pattern(regexp = "PENDING|VERIFIED|REJECTED", message = "Status must be PENDING, VERIFIED, or REJECTED")
        String verificationStatus
) {
}
