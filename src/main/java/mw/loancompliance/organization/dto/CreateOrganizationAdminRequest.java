package mw.loancompliance.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationAdminRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email
) {
}
