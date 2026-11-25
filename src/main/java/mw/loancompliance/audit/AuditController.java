package mw.loancompliance.audit;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Blantyre");

    private final AuditRepository repo;

    public AuditController(AuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public PageResponse<AuditRecord> list(
            @RequestParam(required = false) @Positive Long auditId,
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateFrom must be on or before dateTo");
        }

        List<AuditRecord> records = studentId == null
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findStudentHistory(studentId);

        Instant fromInclusive = dateFrom == null ? null
                : dateFrom.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = dateTo == null ? null
                : dateTo.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        List<AuditRecord> filtered = records.stream()
                .filter(record -> auditId == null || auditId.equals(record.getId()))
                .filter(record -> matches(record.getAction(), action))
                .filter(record -> matches(record.getEntityType(), entityType))
                .filter(record -> fromInclusive == null || (record.getCreatedAt() != null
                        && !record.getCreatedAt().isBefore(fromInclusive)))
                .filter(record -> toExclusive == null || (record.getCreatedAt() != null
                        && record.getCreatedAt().isBefore(toExclusive)))
                .toList();

        return PageResponse.from(filtered, page, size);
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank()
                || (value != null && value.equalsIgnoreCase(filter.trim()));
    }
}
