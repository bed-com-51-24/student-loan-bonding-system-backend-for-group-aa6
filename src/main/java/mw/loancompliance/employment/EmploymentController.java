package mw.loancompliance.employment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import mw.loancompliance.organization.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/employment-records") @SecurityRequirement(name = "bearerAuth")
public class EmploymentController {
    private final EmploymentService service;
    private final OrganizationService orgs;
    public EmploymentController(EmploymentService s,OrganizationService o) {
        service=s;
        orgs=o;
    }
    public record CreateEmploymentRequest(@NotBlank String employmentType,@NotBlank String employmentCategory,Long organizationId,String employerName,String jobTitle,@NotNull LocalDate startDate) {
    }
    public record Decision(String verificationNote,String reason,String reviewStatus,String reviewNote) {
    }
    public record EndDate(@NotNull LocalDate endDate) {
    }
    public record Update(String jobTitle,LocalDate startDate) {
    }
    public record View(Long id,Long studentId,String studentName,Long organizationId,String organizationName,String employmentType,String employmentCategory,String employerName,String jobTitle,LocalDate startDate,LocalDate endDate,String status,String verificationNote) {
        static View from(EmploymentRecord r) {
            return new View(r.getId(),r.getStudent().getId(),r.getStudent().getUser().getFullName(),r.getOrganization()==null?null:r.getOrganization().getId(),r.getOrganization()==null?null:r.getOrganization().getLegalName(),r.getEmploymentType(),r.getEmploymentCategory(),r.getEmployerName(),r.getJobTitle(),r.getStartDate(),r.getEndDate(),r.getStatus(),r.getVerificationNote());
        }
    }
    @PostMapping @PreAuthorize("hasRole('STUDENT')") public View submit(@RequestBody @jakarta.validation.Valid CreateEmploymentRequest q,Authentication a) {
        return View.from(service.submit(a.getName(),q.employmentType(),q.employmentCategory(),q.organizationId(),q.employerName(),q.jobTitle(),q.startDate()));
    }
    @GetMapping("/me") @PreAuthorize("hasRole('STUDENT')") public List<View> own(Authentication a) {
        return service.own(a.getName()).stream().map(View::from).toList();
    }
    @GetMapping @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<View> all() {
        return service.all().stream().map(View::from).toList();
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN','EMPLOYER_ADMIN')") public View get(@PathVariable Long id,Authentication a) {
        boolean admin=a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_LOAN_BOARD_ADMIN"));
        return View.from(service.get(id,a.getName(),admin));
    }
    @GetMapping("/employer/pending") @PreAuthorize("hasRole('EMPLOYER_ADMIN')") public List<View> pending(Authentication a) {
        return service.pending(a.getName()).stream().map(View::from).toList();
    }
    @PatchMapping("/{id}/verify") @PreAuthorize("hasRole('EMPLOYER_ADMIN')") public View verify(@PathVariable Long id,@RequestBody Decision d,Authentication a) {
        return View.from(service.decide(id,a.getName(),false,"VERIFIED",d.verificationNote()));
    }
    @PatchMapping("/{id}/reject") @PreAuthorize("hasRole('EMPLOYER_ADMIN')") public View reject(@PathVariable Long id,@RequestBody Decision d,Authentication a) {
        return View.from(service.decide(id,a.getName(),false,"REJECTED",d.reason()));
    }
    @PatchMapping("/{id}/review") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public View review(@PathVariable Long id,@RequestBody Decision d) {
        return View.from(service.decide(id,"",true,d.reviewStatus(),d.reviewNote()));
    }
    @PatchMapping("/{id}") @PreAuthorize("hasRole('STUDENT')") public View update(@PathVariable Long id,@RequestBody Update d,Authentication a) {
        return View.from(service.updateOwn(id,a.getName(),d.jobTitle(),d.startDate()));
    }
    @PatchMapping("/{id}/end-date") @PreAuthorize("hasAnyRole('STUDENT','EMPLOYER_ADMIN')") public View end(@PathVariable Long id,@RequestBody @jakarta.validation.Valid EndDate d,Authentication a) {
        boolean emp=a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_EMPLOYER_ADMIN"));
        return View.from(service.end(id,a.getName(),emp,d.endDate()));
    }
}
