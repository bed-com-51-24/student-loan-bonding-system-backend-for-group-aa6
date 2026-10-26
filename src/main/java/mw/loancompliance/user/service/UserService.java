
package mw.loancompliance.user.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import mw.loancompliance.auth.AuthService;
import mw.loancompliance.user.dto.CreatePendingUserCommand;
import mw.loancompliance.user.dto.UpdateUserStatusRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.entity.UserAccountStatus;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final Validator validator;

    public UserService(
            UserRepository userRepository,
            AuthService authService,
            Validator validator) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.validator = validator;
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

    /**
     * Creates a pending shared account for a trusted Student or Organization
     * onboarding workflow. This is an internal service operation, not an API
     * endpoint. The caller supplies the role and organization from its own
     * server-side workflow; privileged administrator accounts are not allowed.
     */
    @Transactional
    public UserResponse createPendingUser(
            CreatePendingUserCommand command,
            UserRole role,
            Long organizationId) {

        validateCreatePendingUserCommand(command);
        validateOnboardingAssignment(role, organizationId);

        String fullName = command.fullName().trim();
        String email = command.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role.name());
        user.setAccountStatus(UserAccountStatus.PENDING_ACTIVATION.name());
        user.setOrganizationId(organizationId);
        user.setPasswordHash(null);

        User savedUser = userRepository.save(user);
        authService.sendActivationEmail(savedUser);

        return toUserResponse(savedUser);
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

    private void validateCreatePendingUserCommand(CreatePendingUserCommand command) {
        if (command == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User details are required");
        }

        Set<ConstraintViolation<CreatePendingUserCommand>> violations =
                validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    violations.iterator().next().getMessage());
        }
    }

    private void validateOnboardingAssignment(UserRole role, Long organizationId) {
        if (role != UserRole.STUDENT && role != UserRole.EMPLOYER_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Onboarding can only create STUDENT or EMPLOYER_ADMIN accounts");
        }

        if (role == UserRole.STUDENT && organizationId != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student accounts cannot be assigned to an organization");
        }

        if (role == UserRole.EMPLOYER_ADMIN
                && (organizationId == null || organizationId <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A positive organization ID is required for employer administrators");
        }
    }
}
