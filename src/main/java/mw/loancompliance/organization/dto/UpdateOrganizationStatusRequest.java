package mw.loancompliance.organization.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationStatusRequest(
        @NotNull Boolean active
) {
}
