
package mw.loancompliance.user.service;

import mw.loancompliance.user.dto.UpdateUserStatusRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.entity.UserAccountStatus;
import mw.loancompliance.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Retrieve all users.
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    // Retrieve one user by ID.
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with ID: " + id));

        return toUserResponse(user);
    }

    // Change account status without exposing password or other secret fields.
    @Transactional
    public UserResponse updateUserStatus(
            Long id,
            UpdateUserStatusRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with ID: " + id));

        if (UserAccountStatus.PENDING_ACTIVATION.name()
                .equals(user.getAccountStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pending accounts must be activated through the auth workflow");
        }

        if (request.accountStatus() == UserAccountStatus.PENDING_ACTIVATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account status can only be changed to ACTIVE or DISABLED");
        }
        user.setAccountStatus(request.accountStatus().name());

        User updatedUser = userRepository.save(user);

        return toUserResponse(updatedUser);
    }

    // Convert the database entity into a safe API response.
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getOrganizationId(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
