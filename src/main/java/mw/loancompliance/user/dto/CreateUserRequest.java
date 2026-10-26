
package mw.loancompliance.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Role is required")
        @Pattern(
                regexp = "LOAN_BOARD_ADMIN|STUDENT|EMPLOYER_ADMIN",
                message = "Role must be LOAN_BOARD_ADMIN, STUDENT, or EMPLOYER_ADMIN"
        )
        String role,

        @Positive(message = "Organization ID must be positive")
        Long organizationId

) {
}
