package mw.loancompliance.organization;

import mw.loancompliance.organization.dto.CreateOrganizationAdminRequest;
import mw.loancompliance.organization.dto.CreateOrganizationRequest;
import mw.loancompliance.organization.dto.UpdateOrganizationRequest;
import mw.loancompliance.user.dto.CreatePendingUserCommand;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            UserService userService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public Organization createOrganization(CreateOrganizationRequest request) {
        String registrationNumber = normalize(request.registrationNumber());
        if (registrationNumber != null
                && organizationRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An organization with this registration number already exists");
        }

        Organization organization = new Organization();
        organization.setLegalName(request.legalName().trim());
        organization.setRegistrationNumber(registrationNumber);
        organization.setSector(normalize(request.sector()));
        organization.setAddress(normalize(request.address()));
        organization.setPhone(normalize(request.phone()));
        organization.setEmail(normalizeEmail(request.email()));
        organization.setActive(true);
        return organizationRepository.save(organization);
    }

    public List<Organization> getActiveOrganizations() {
        return organizationRepository.findAllByActiveTrueOrderByLegalNameAsc();
    }

    public Organization getOrganization(Long id, String email, boolean loanBoardAdmin) {
        Organization organization = findOrganization(id);
        if (!loanBoardAdmin && !isAssignedTo(email, id)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You may only view your assigned organization");
        }
        return organization;
    }

    public Organization getMyOrganization(String email) {
        Long organizationId = userRepository.findByEmailIgnoreCase(email)
                .map(user -> user.getOrganizationId())
                .orElse(null);
        if (organizationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No organization is assigned to this account");
        }
        return findOrganization(organizationId);
    }

    @Transactional
    public Organization updateOrganization(Long id, UpdateOrganizationRequest request) {
        Organization organization = findOrganization(id);
        if (request.legalName() == null
                && request.registrationNumber() == null
                && request.sector() == null
                && request.address() == null
                && request.phone() == null
                && request.email() == null) {
            throw new IllegalArgumentException("At least one organization field is required");
        }

        if (request.legalName() != null) {
            if (request.legalName().isBlank()) {
                throw new IllegalArgumentException("Legal name must not be blank");
            }
            organization.setLegalName(request.legalName().trim());
        }

        if (request.registrationNumber() != null) {
            String registrationNumber = normalize(request.registrationNumber());
            if (registrationNumber != null
                    && organizationRepository.existsByRegistrationNumberAndIdNot(
                            registrationNumber, id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An organization with this registration number already exists");
            }
            organization.setRegistrationNumber(registrationNumber);
        }
        if (request.sector() != null) {
            organization.setSector(normalize(request.sector()));
        }
        if (request.address() != null) {
            organization.setAddress(normalize(request.address()));
        }
        if (request.phone() != null) {
            organization.setPhone(normalize(request.phone()));
        }
        if (request.email() != null) {
            organization.setEmail(normalizeEmail(request.email()));
        }
        return organizationRepository.save(organization);
    }

    @Transactional
    public Organization updateOrganizationStatus(Long id, boolean active) {
        Organization organization = findOrganization(id);
        organization.setActive(active);
        return organizationRepository.save(organization);
    }

    @Transactional
    public UserResponse createOrganizationAdmin(
            Long organizationId,
            CreateOrganizationAdminRequest request) {
        findOrganization(organizationId);
        return userService.createPendingUser(
                new CreatePendingUserCommand(request.fullName(), request.email()),
                UserRole.EMPLOYER_ADMIN,
                organizationId);
    }

    public boolean isAssignedTo(String email, Long organizationId) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> organizationId.equals(user.getOrganizationId())
                        && UserRole.EMPLOYER_ADMIN.name().equals(user.getRole()))
                .orElse(false);
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found with ID: " + id));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
