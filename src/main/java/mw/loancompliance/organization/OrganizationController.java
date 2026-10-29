package mw.loancompliance.organization;

import jakarta.validation.Valid;
import mw.loancompliance.organization.dto.*;
import mw.loancompliance.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public List<OrganizationResponse> getActiveOrganizations() {
        return organizationService.getActiveOrganizations();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    public OrganizationResponse getMyOrganization(Authentication authentication) {
        return organizationService.getMyOrganization(authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'EMPLOYER_ADMIN')")
    public OrganizationResponse getOrganizationById(@PathVariable Long id, Authentication authentication) {
        boolean boardAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_LOAN_BOARD_ADMIN"));
        return organizationService.getOrganizationById(id, authentication.getName(), boardAdmin);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public OrganizationResponse updateOrganization(@PathVariable Long id,
            @Valid @RequestBody OrganizationPatchRequest request) {
        return organizationService.updateOrganization(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public OrganizationResponse updateStatus(@PathVariable Long id,
            @RequestBody OrganizationStatusRequest request) {
        return organizationService.updateStatus(id, request.isActive());
    }

    @PostMapping("/{id}/admins")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public ResponseEntity<UserResponse> createEmployerAdmin(@PathVariable Long id,
            @Valid @RequestBody OrganizationAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createEmployerAdmin(id, request));
    }
}
