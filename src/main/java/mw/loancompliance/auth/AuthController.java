package mw.loancompliance.auth;

import jakarta.validation.Valid;
import mw.loancompliance.auth.dto.*;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/activate")
    public MessageResponse activate(
            @Valid @RequestBody ActivateAccountRequest request) {
        return authService.activateAccount(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}