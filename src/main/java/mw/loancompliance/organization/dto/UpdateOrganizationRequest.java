package mw.loancompliance.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 200) String legalName,
        @Size(max = 100) String registrationNumber,
        @Size(max = 100) String sector,
        String address,
        @Size(max = 30) String phone,
        @Email @Size(max = 255) String email
) {
}
