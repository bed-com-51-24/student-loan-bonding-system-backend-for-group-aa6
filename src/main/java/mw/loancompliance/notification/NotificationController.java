package mw.loancompliance.notification;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationController(NotificationRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    public record Create(@NotNull @Positive Long recipientUserId, @NotBlank String subject,
            @NotBlank String message, String channel) { }
    public record UpdateNotificationRequest(@NotNull Boolean read) { }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<NotificationRecord> list(
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean admin = hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN");
        List<NotificationRecord> records;
        if (admin) {
            records = repo.findAllByOrderByCreatedAtDesc();
            if (userId != null) {
                records = records.stream().filter(record -> record.getUser() != null
                        && userId.equals(record.getUser().getId())).toList();
            }
        } else {
            if (userId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only Loan Board administrators may filter notifications by userId");
            }
            records = repo.findAllByUser_EmailIgnoreCaseOrderByCreatedAtDesc(authentication.getName());
        }
        if (read != null) {
            records = records.stream().filter(record -> (record.getReadAt() != null) == read).toList();
        }
        return PageResponse.from(records, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecord one(@PathVariable @Positive Long id, Authentication authentication) {
        if (hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN")) {
            return repo.findById(id).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Notification not found"));
        }
        return repo.findByIdAndUser_EmailIgnoreCase(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecord update(@PathVariable @Positive Long id,
            @RequestBody @Valid UpdateNotificationRequest request,
            Authentication authentication) {
        NotificationRecord notification = hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN")
                ? repo.findById(id).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found"))
                : repo.findByIdAndUser_EmailIgnoreCase(id, authentication.getName())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Notification not found"));
        if (request.read()) {
            notification.markRead();
        } else {
            notification.markUnread();
        }
        return repo.save(notification);
    }

    @PostMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationRecord create(@RequestBody @Valid Create request) {
        var recipient = users.findById(request.recipientUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        NotificationRecord notification = new NotificationRecord();
        notification.user = recipient;
        notification.title = request.subject();
        notification.message = request.message();
        notification.channel = request.channel() == null ? "IN_APP" : request.channel();
        notification.deliveryStatus = "PENDING";
        return repo.save(notification);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
