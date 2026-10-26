package mw.loancompliance.user.dto;

import jakarta.validation.constraints.NotNull;
import mw.loancompliance.user.entity.UserAccountStatus;

public record UpdateUserStatusRequest(
        @NotNull(message = "Account status is required")
        UserAccountStatus accountStatus
) {
}
