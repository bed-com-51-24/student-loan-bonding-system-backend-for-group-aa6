package mw.loancompliance.user.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.user.dto.UpdateUserStatusRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(userService.getAllUsers(), page, size);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable @Positive Long id) {
        return userService.getUserById(id);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateUserStatus(id, request);
    }
}
