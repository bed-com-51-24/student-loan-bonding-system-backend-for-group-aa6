package mw.loancompliance.user.service;

import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.entity.UserAccountStatus;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Creates the initial administrator when bootstrap credentials are configured. */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Set both ADMIN_EMAIL and ADMIN_PASSWORD to bootstrap an administrator");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        User admin = new User();
        admin.setFullName(normalizedEmail);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.LOAN_BOARD_ADMIN.name());
        admin.setAccountStatus(UserAccountStatus.ACTIVE.name());
        admin.setEmailVerifiedAt(java.time.Instant.now());
        userRepository.save(admin);
    }
}
