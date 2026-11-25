package mw.loancompliance.user.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.user.dto.UpdateUserStatusRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.UserAccountStatus;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserAccountStatus status,
            @RequestParam(required = false) @Positive Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UserResponse> users = userService.getAllUsers().stream()
                .filter(user -> userId == null || userId.equals(user.id()))
                .filter(user -> role == null || role.name().equals(user.role()))
                .filter(user -> status == null || status.name().equals(user.accountStatus()))
                .filter(user -> organizationId == null
                        || organizationId.equals(user.organizationId()))
                .toList();
        if (userId != null && users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found with ID: " + userId);
        }
        return PageResponse.from(users, page, size);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateUserStatus(id, request);
    }
}
