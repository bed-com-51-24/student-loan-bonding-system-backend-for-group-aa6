package mw.loancompliance.organization;

import jakarta.persistence.EntityNotFoundException;
import mw.loancompliance.organization.dto.OrganizationAdminRequest;
import mw.loancompliance.organization.dto.OrganizationPatchRequest;
import mw.loancompliance.organization.dto.OrganizationRequest;
import mw.loancompliance.organization.dto.OrganizationResponse;
import mw.loancompliance.user.dto.CreatePendingUserCommand;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserService userService;

    public OrganizationService(OrganizationRepository organizationRepository, UserService userService) {
        this.organizationRepository = organizationRepository;
        this.userService = userService;
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        checkRegistrationNumberIsAvailable(request.getRegistrationNumber(), null);
        OffsetDateTime now = OffsetDateTime.now();
        Organization organization = new Organization();
        copyRequestToOrganization(request, organization);
        organization.setActive(true);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        return toResponse(organizationRepository.save(organization));
    }

    public List<OrganizationResponse> getActiveOrganizations() {
        return organizationRepository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    public OrganizationResponse getOrganizationById(Long id, String email, boolean boardAdmin) {
        Organization organization = findOrganizationById(id);
        if (!boardAdmin && !hasOrganizationAssignment(email, id)) {
            throw new AccessDeniedException("You cannot access this organization");
        }
        return toResponse(organization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationPatchRequest request) {
        Organization organization = findOrganizationById(id);
        if (request.getRegistrationNumber() != null) {
            checkRegistrationNumberIsAvailable(request.getRegistrationNumber(), id);
        }
        if (request.getLegalName() != null) {
            if (request.getLegalName().isBlank()) throw new IllegalArgumentException("Legal name must not be blank");
            organization.setLegalName(request.getLegalName());
        }
        if (request.getRegistrationNumber() != null) organization.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getSector() != null) organization.setSector(request.getSector());
        if (request.getAddress() != null) organization.setAddress(request.getAddress());
        if (request.getPhone() != null) organization.setPhone(request.getPhone());
        if (request.getEmail() != null) organization.setEmail(request.getEmail());
        organization.setUpdatedAt(OffsetDateTime.now());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationResponse updateStatus(Long id, boolean active) {
        Organization organization = findOrganizationById(id);
        organization.setActive(active);
        organization.setUpdatedAt(OffsetDateTime.now());
        return toResponse(organizationRepository.save(organization));
    }

    @Transactional
    public UserResponse createEmployerAdmin(Long id, OrganizationAdminRequest request) {
        findOrganizationById(id);
        return userService.createPendingUser(
                new CreatePendingUserCommand(request.fullName(), request.email()),
                UserRole.EMPLOYER_ADMIN,
                id);
    }

    public OrganizationResponse getMyOrganization(String email) {
        UserResponse user = findUser(email);
        if (!UserRole.EMPLOYER_ADMIN.name().equals(user.role()) || user.organizationId() == null) {
            throw new AccessDeniedException("No employer organization is assigned to this account");
        }
        return toResponse(findOrganizationById(user.organizationId()));
    }

    private boolean hasOrganizationAssignment(String email, Long id) {
        UserResponse user = findUser(email);
        return UserRole.EMPLOYER_ADMIN.name().equals(user.role()) && id.equals(user.organizationId());
    }

    private UserResponse findUser(String email) {
        return userService.getAllUsers().stream()
                .filter(user -> user.email() != null && user.email().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user was not found"));
    }

    private Organization findOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization with ID " + id + " was not found"));
    }

    private void checkRegistrationNumberIsAvailable(String registrationNumber, Long currentId) {
        if (registrationNumber == null || registrationNumber.isBlank()) return;
        boolean used = currentId == null
                ? organizationRepository.existsByRegistrationNumber(registrationNumber)
                : organizationRepository.existsByRegistrationNumberAndIdNot(registrationNumber, currentId);
        if (used) throw new IllegalArgumentException("Registration number is already used by another organization");
    }

    private void copyRequestToOrganization(OrganizationRequest request, Organization organization) {
        organization.setLegalName(request.getLegalName());
        organization.setRegistrationNumber(request.getRegistrationNumber());
        organization.setSector(request.getSector());
        organization.setAddress(request.getAddress());
        organization.setPhone(request.getPhone());
        organization.setEmail(request.getEmail());
    }

    private OrganizationResponse toResponse(Organization organization) {
        OrganizationResponse response = new OrganizationResponse();
        response.setId(organization.getId());
        response.setLegalName(organization.getLegalName());
        response.setRegistrationNumber(organization.getRegistrationNumber());
        response.setSector(organization.getSector());
        response.setAddress(organization.getAddress());
        response.setActive(organization.isActive());
        response.setPhone(organization.getPhone());
        response.setEmail(organization.getEmail());
        response.setCreatedAt(organization.getCreatedAt());
        response.setUpdatedAt(organization.getUpdatedAt());
        return response;
    }
}
