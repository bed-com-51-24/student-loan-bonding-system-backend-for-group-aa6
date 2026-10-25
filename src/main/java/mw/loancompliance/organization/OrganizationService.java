package mw.loancompliance.organization;

import jakarta.persistence.EntityNotFoundException;
import mw.loancompliance.organization.dto.OrganizationRequest;
import mw.loancompliance.organization.dto.OrganizationResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrganizationResponse createOrganization(OrganizationRequest request) {
        checkRegistrationNumberIsAvailable(request.getRegistrationNumber(), null);

        OffsetDateTime now = OffsetDateTime.now();
        Organization organization = new Organization();
        copyRequestToOrganization(request, organization);
        organization.setActive(true);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);

        Organization savedOrganization = organizationRepository.save(organization);
        return toResponse(savedOrganization);
    }

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrganizationResponse getOrganizationById(Long id) {
        Organization organization = findOrganizationById(id);
        return toResponse(organization);
    }

    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization organization = findOrganizationById(id);
        checkRegistrationNumberIsAvailable(request.getRegistrationNumber(), id);

        copyRequestToOrganization(request, organization);
        organization.setUpdatedAt(OffsetDateTime.now());

        Organization savedOrganization = organizationRepository.save(organization);
        return toResponse(savedOrganization);
    }

    public OrganizationResponse deactivateOrganization(Long id) {
        Organization organization = findOrganizationById(id);
        organization.setActive(false);
        organization.setUpdatedAt(OffsetDateTime.now());

        Organization savedOrganization = organizationRepository.save(organization);
        return toResponse(savedOrganization);
    }

    private Organization findOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization with ID " + id + " was not found"));
    }

    private void checkRegistrationNumberIsAvailable(String registrationNumber, Long currentOrganizationId) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return;
        }

        boolean registrationNumberIsUsed;
        if (currentOrganizationId == null) {
            registrationNumberIsUsed = organizationRepository.existsByRegistrationNumber(registrationNumber);
        } else {
            registrationNumberIsUsed = organizationRepository
                    .existsByRegistrationNumberAndIdNot(registrationNumber, currentOrganizationId);
        }

        if (registrationNumberIsUsed) {
            throw new IllegalArgumentException(
                    "Registration number is already used by another organization");
        }
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
