package mw.loancompliance.notification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/notifications") @SecurityRequirement(name = "bearerAuth") public class NotificationController {
    final NotificationRepository repo;
    final UserRepository users;
    public NotificationController(NotificationRepository r,UserRepository u) {
        repo=r;
        users=u;
    }
    public record Create(@NotNull Long recipientUserId,@NotBlank String subject,@NotBlank String message,String channel) {
    }
    @GetMapping("/me") public List<NotificationRecord> own(Authentication a) {
        return repo.findAllByUser_EmailIgnoreCaseOrderByCreatedAtDesc(a.getName());
    }
    @GetMapping("/{id}") public NotificationRecord one(@PathVariable Long id,Authentication a) {
        return repo.findByIdAndUser_EmailIgnoreCase(id,a.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
    }
    @PatchMapping("/{id}/read") public NotificationRecord read(@PathVariable Long id,Authentication a) {
        var n=repo.findByIdAndUser_EmailIgnoreCase(id,a.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
        n.markRead();
        return n;
    }
    @GetMapping @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<NotificationRecord> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
    @PostMapping @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") @ResponseStatus(HttpStatus.CREATED) public NotificationRecord create(@RequestBody @Valid Create c) {
        var u=users.findById(c.recipientUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        NotificationRecord n=new NotificationRecord();
        n.user=u;
        n.title=c.subject();
        n.message=c.message();
        n.channel=c.channel()==null?"IN_APP":c.channel();
        n.deliveryStatus="PENDING";
        return repo.save(n);
    }
}
