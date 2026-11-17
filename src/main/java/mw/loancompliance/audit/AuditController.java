package mw.loancompliance.audit;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/audit-logs") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") @SecurityRequirement(name = "bearerAuth") public class AuditController {
    final AuditRepository repo;
    public AuditController(AuditRepository r) {
        repo=r;
    }
    @GetMapping public List<AuditRecord> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
    @GetMapping("/{id}") public AuditRecord one(@PathVariable Long id) {
        return repo.findById(id).orElseThrow();
    }
    @GetMapping("/students/{studentId}") public List<AuditRecord> student(@PathVariable Long studentId) {
        return repo.findStudentHistory(studentId);
    }
}
