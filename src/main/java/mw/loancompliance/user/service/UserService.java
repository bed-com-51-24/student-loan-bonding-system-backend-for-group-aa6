package mw.loancompliance.user.service;

import mw.loancompliance.user.dto.UpdateUserRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger LOGGER =
            Logger.getLogger(UserService.class.getName());

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(
            UserRepository userRepository,
            AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public UserResponse createPendingUser(String fullName, String email) {

        String cleanedName = fullName.trim();
        String cleanedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(cleanedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists");
        }

        User user = new User();
        user.setFullName(cleanedName);
        user.setEmail(cleanedEmail);
        user.setRole("STUDENT");
        user.setAccountStatus("PENDING_ACTIVATION");
        user.setOrganizationId(null);
        user.setPasswordHash(null);

        User savedUser = userRepository.save(user);

        // Send the activation email only after the database transaction commits.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            authService.sendActivationEmail(savedUser);
                        } catch (RuntimeException exception) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Pending user was created, but activation email could not be sent.",
                                    exception
                            );
                        }
                    }
                });

        return toUserResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found with ID: " + id));

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found with ID: " + id));

        user.setFullName(request.fullName());
        user.setOrganizationId(request.organizationId());

        User updatedUser = userRepository.save(user);

        return toUserResponse(updatedUser);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getOrganizationId(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
