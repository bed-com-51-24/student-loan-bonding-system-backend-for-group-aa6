package mw.loancompliance.employment;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class EmploymentController {
    private final EmploymentService service;

    public EmploymentController(EmploymentService service) {
        this.service = service;
    }

    public record AddEmployeeRequest(@NotBlank String nationalId, @NotBlank String employeeName,
            String jobTitle, @NotNull LocalDate startDate) { }
    public record EndDateRequest(@NotNull LocalDate endDate) { }
    public record EmployeeView(Long id, Long studentId, String employeeName,
            boolean loanBeneficiary, Long organizationId, String organizationName,
            String jobTitle, LocalDate startDate, LocalDate endDate, String status) {
        static EmployeeView from(EmploymentRecord record) {
            return new EmployeeView(record.getId(),
                    record.getStudent() == null ? null : record.getStudent().getId(),
                    record.getEmployeeName(), record.getStudent() != null,
                    record.getOrganization() == null ? null : record.getOrganization().getId(),
                    record.getOrganization() == null ? null : record.getOrganization().getLegalName(),
                    record.getJobTitle(), record.getStartDate(), record.getEndDate(), record.getStatus());
        }
    }
    public record EmployeeRosterResponse(long loanBeneficiaryCount, long otherEmployeeCount,
            PageResponse<EmployeeView> employees) { }

    @PostMapping("/organizations/me/employees")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeView addEmployee(@Valid @RequestBody AddEmployeeRequest request,
            Authentication authentication) {
        return EmployeeView.from(service.addEmployee(authentication.getName(),
                request.nationalId(), request.employeeName(), request.jobTitle(), request.startDate()));
    }

    @GetMapping("/employment-records")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'EMPLOYER_ADMIN', 'STUDENT')")
    public EmployeeRosterResponse listEmploymentRecords(
            @RequestParam(required = false) @Positive Long organizationId,
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) @Positive Long recordId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean admin = hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN");
        boolean employerAdmin = hasRole(authentication, "ROLE_EMPLOYER_ADMIN");
        List<EmploymentRecord> records;
        if (admin) {
            records = service.all();
            if (organizationId != null) {
                records = records.stream().filter(record -> record.getOrganization() != null
                        && organizationId.equals(record.getOrganization().getId())).toList();
            }
            if (studentId != null) {
                records = records.stream().filter(record -> record.getStudent() != null
                        && studentId.equals(record.getStudent().getId())).toList();
            }
            if (active != null) {
                records = records.stream().filter(record -> isActive(record) == active).toList();
            }
            if (recordId != null) {
                records = records.stream().filter(record -> recordId.equals(record.getId())).toList();
            }
        } else {
            if (organizationId != null || studentId != null || recordId != null || active != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only Loan Board administrators may filter employment records");
            }
            records = employerAdmin
                    ? service.employerEmployees(authentication.getName()).employees()
                    : service.own(authentication.getName());
        }

        long matched = records.stream().filter(record -> record.getStudent() != null).count();
        List<EmployeeView> views = records.stream().map(EmployeeView::from).toList();
        return new EmployeeRosterResponse(matched, records.size() - matched,
                PageResponse.from(views, page, size));
    }

    @PatchMapping("/employment-records/{id}")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    public EmployeeView update(@PathVariable @Positive Long id,
            @Valid @RequestBody EndDateRequest request,
            Authentication authentication) {
        return EmployeeView.from(service.end(id, authentication.getName(), request.endDate()));
    }

    private boolean isActive(EmploymentRecord record) {
        return record.getEndDate() == null && "VERIFIED".equals(record.getStatus());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
