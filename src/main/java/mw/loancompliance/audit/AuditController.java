package mw.loancompliance.audit;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final AuditRepository repo;

    public AuditController(AuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public PageResponse<AuditRecord> all(
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AuditRecord> records = studentId == null
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findStudentHistory(studentId);
        return PageResponse.from(records, page, size);
    }

    @GetMapping("/{id}")
    public AuditRecord one(@PathVariable Long id) {
        return repo.findById(id).orElseThrow();
    }

}
