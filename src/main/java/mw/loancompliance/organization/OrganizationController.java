package mw.loancompliance.organization;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.organization.dto.CreateOrganizationAdminRequest;
import mw.loancompliance.organization.dto.CreateOrganizationRequest;
import mw.loancompliance.organization.dto.OrganizationResponse;
import mw.loancompliance.organization.dto.UpdateOrganizationRequest;
import mw.loancompliance.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        return OrganizationResponse.from(
                organizationService.createOrganization(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'EMPLOYER_ADMIN')")
    public PageResponse<OrganizationResponse> getOrganizations(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long organizationId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean loanBoardAdmin = hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN");
        if (loanBoardAdmin && scope != null && !"ALL".equalsIgnoreCase(scope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Loan Board administrators may use scope=ALL or omit scope");
        }
        List<Organization> organizations = loanBoardAdmin
                ? organizationService.getOrganizations(active, true).stream()
                    .filter(organization -> organizationId == null
                            || organizationId.equals(organization.getId()))
                    .toList()
                : employerOrganizations(authentication.getName(), scope, organizationId, active);
        return PageResponse.from(organizations.stream()
                        .map(OrganizationResponse::from)
                        .toList(), page, size);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public OrganizationResponse updateOrganization(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            Authentication authentication) {
        return OrganizationResponse.from(
                organizationService.updateOrganization(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/admins")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createOrganizationAdmin(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateOrganizationAdminRequest request,
            Authentication authentication) {
        return organizationService.createOrganizationAdmin(id, request, authentication.getName());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private List<Organization> employerOrganizations(
            String email, String scope, Long organizationId, Boolean active) {
        if (!"SELF".equalsIgnoreCase(scope)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Employer admins must use scope=SELF");
        }
        if (organizationId != null || active != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employer admins cannot filter organizations; their assigned organization is used");
        }
        return List.of(organizationService.getMyOrganization(email));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }
}
