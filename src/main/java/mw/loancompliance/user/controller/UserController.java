
package mw.loancompliance.user.controller;

import mw.loancompliance.user.dto.UpdateUserRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    @PutMapping("/{id}")
public UserResponse updateUser(
        @PathVariable Long id,
        @jakarta.validation.Valid @RequestBody UpdateUserRequest request) {
    return userService.updateUser(id, request);
}
}
