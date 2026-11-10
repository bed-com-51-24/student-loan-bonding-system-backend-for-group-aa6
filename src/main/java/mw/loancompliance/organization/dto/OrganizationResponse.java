package mw.loancompliance.organization.dto;

import mw.loancompliance.organization.Organization;

import java.time.Instant;

public record OrganizationResponse(
        Long id,
        String legalName,
        String registrationNumber,
        String sector,
        String address,
        String phone,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getLegalName(),
                organization.getRegistrationNumber(),
                organization.getSector(),
                organization.getAddress(),
                organization.getPhone(),
                organization.getEmail(),
                organization.isActive(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}
