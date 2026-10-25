package mw.loancompliance.auth;

import mw.loancompliance.auth.dto.*;
import mw.loancompliance.auth.token.*;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountActivationTokenRepository activationRepository;
    private final PasswordResetTokenRepository resetRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountActivationTokenRepository activationRepository,
            PasswordResetTokenRepository resetRepository,
            JavaMailSender mailSender) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.activationRepository = activationRepository;
        this.resetRepository = resetRepository;
        this.mailSender = mailSender;
    }

    // LOGIN
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is not active");
        }

        user.setLastLoginAt(Instant.now());

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole());

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole());
    }

    // ACCOUNT ACTIVATION
    @Transactional
    public MessageResponse activateAccount(ActivateAccountRequest request) {

        String hash = hashToken(request.token());

        AccountActivationToken token = activationRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> invalidToken());

        if (token.getUsedAt() != null ||
                !token.getExpiresAt().isAfter(Instant.now())) {
            throw invalidToken();
        }

        User user = token.getUser();

        if ("DISABLED".equals(user.getAccountStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is disabled");
        }

        user.setAccountStatus("ACTIVE");
        user.setEmailVerifiedAt(Instant.now());
        token.setUsedAt(Instant.now());

        return new MessageResponse("Account activated successfully");
    }

    // FORGOT PASSWORD
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {

        userRepository.findByEmailIgnoreCase(request.email())
                .filter(user -> "ACTIVE".equals(user.getAccountStatus()))
                .ifPresent(user -> {
                    String rawToken = generateToken();
                    String hash = hashToken(rawToken);

                    resetRepository.save(new PasswordResetToken(
                            user,
                            hash,
                            Instant.now().plus(30, ChronoUnit.MINUTES)));

                    sendEmail(
                            user.getEmail(),
                            "Password reset",
                            "Use this link to reset your password:\n\n"
                                    + frontendUrl
                                    + "/reset-password?token="
                                    + rawToken
                                    + "\n\nThis link expires in 30 minutes.");
                });

        return new MessageResponse(
                "If an eligible account exists, password reset instructions will be sent.");
    }

    // RESET PASSWORD
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {

        String hash = hashToken(request.token());

        PasswordResetToken token = resetRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> invalidToken());

        if (token.getUsedAt() != null ||
                !token.getExpiresAt().isAfter(Instant.now())) {
            throw invalidToken();
        }

        User user = token.getUser();

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is not active");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(Instant.now());

        return new MessageResponse("Password reset successfully");
    }

    // Generate an activation token for the user-creation workflow.
    // Call this when an authorized user creates an account.
    @Transactional
    public void sendActivationEmail(User user) {

        String rawToken = generateToken();

        activationRepository.save(new AccountActivationToken(
                user,
                hashToken(rawToken),
                Instant.now().plus(24, ChronoUnit.HOURS)));

        sendEmail(
                user.getEmail(),
                "Activate your account",
                "Activate your account using this link:\n\n"
                        + frontendUrl
                        + "/activate?token="
                        + rawToken
                        + "\n\nThis link expires in 24 hours.");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash token", exception);
        }
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired token");
    }
}