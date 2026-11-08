package mw.loancompliance.user.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mw.loancompliance.auth.AuthService;
import mw.loancompliance.user.dto.CreatePendingUserCommand;
import mw.loancompliance.user.dto.UpdateUserStatusRequest;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.entity.UserAccountStatus;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private AuthService authService;
    private ValidatorFactory validatorFactory;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = mock(AuthService.class);
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        userService = new UserService(userRepository, authService, validator);
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void createsPendingStudentAccountAndRequestsActivationEmail() {
        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userService.createPendingUser(
                new CreatePendingUserCommand("  Student Name  ", " Student@Example.com "),
                UserRole.STUDENT,
                null);

        assertEquals("Student Name", result.fullName());
        assertEquals("student@example.com", result.email());
        assertEquals(UserRole.STUDENT.name(), result.role());
        assertEquals(UserAccountStatus.PENDING_ACTIVATION.name(), result.accountStatus());
        assertNull(result.organizationId());
        verify(userRepository).save(any(User.class));
        verify(authService).sendActivationEmail(any(User.class));
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingAccountOrSendingEmail() {
        when(userRepository.existsByEmailIgnoreCase("student@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.createPendingUser(
                        new CreatePendingUserCommand("Student Name", "STUDENT@example.com"),
                        UserRole.STUDENT,
                        null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
        verify(authService, never()).sendActivationEmail(any(User.class));
    }

    @Test
    void rejectsInvalidOnboardingRole() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.createPendingUser(
                        new CreatePendingUserCommand("Admin Name", "admin@example.com"),
                        UserRole.LOAN_BOARD_ADMIN,
                        null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsEmployerAdminWithoutOrganization() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.createPendingUser(
                        new CreatePendingUserCommand("Employer Admin", "employer@example.com"),
                        UserRole.EMPLOYER_ADMIN,
                        null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refusesToChangePendingAccountStatus() {
        User pendingUser = new User();
        pendingUser.setAccountStatus(UserAccountStatus.PENDING_ACTIVATION.name());
        when(userRepository.findById(5L)).thenReturn(Optional.of(pendingUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.updateUserStatus(
                        5L,
                        new UpdateUserStatusRequest(UserAccountStatus.ACTIVE)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }
}
