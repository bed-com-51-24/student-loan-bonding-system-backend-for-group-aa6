package mw.loancompliance.organization;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.organization.dto.CreateOrganizationAdminRequest;
import mw.loancompliance.organization.dto.CreateOrganizationRequest;
import mw.loancompliance.organization.dto.OrganizationResponse;
import mw.loancompliance.organization.dto.UpdateOrganizationRequest;
import mw.loancompliance.organization.dto.UpdateOrganizationStatusRequest;
import mw.loancompliance.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody CreateOrganizationRequest request) {
        return OrganizationResponse.from(
                organizationService.createOrganization(request));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public List<OrganizationResponse> getActiveOrganizations() {
        return organizationService.getActiveOrganizations()
                .stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    public OrganizationResponse getMyOrganization(Authentication authentication) {
        return OrganizationResponse.from(
                organizationService.getMyOrganization(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'EMPLOYER_ADMIN')")
    public OrganizationResponse getOrganization(
            @PathVariable @Positive Long id,
            Authentication authentication) {
        boolean isLoanBoardAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LOAN_BOARD_ADMIN"::equals);
        return OrganizationResponse.from(organizationService.getOrganization(
                id, authentication.getName(), isLoanBoardAdmin));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public OrganizationResponse updateOrganization(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return OrganizationResponse.from(
                organizationService.updateOrganization(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public OrganizationResponse updateOrganizationStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        return OrganizationResponse.from(
                organizationService.updateOrganizationStatus(id, request.active()));
    }

    @PostMapping("/{id}/admins")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createOrganizationAdmin(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateOrganizationAdminRequest request) {
        return organizationService.createOrganizationAdmin(id, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> handleIllegalArgument(
            IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }
}
