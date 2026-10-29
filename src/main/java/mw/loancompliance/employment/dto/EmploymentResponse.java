package mw.loancompliance.employment.dto;

import mw.loancompliance.employment.Employment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmploymentResponse(
        Long id,
        Long studentId,
        Long organizationId,
        String jobTitle,
        BigDecimal monthlySalary,
        LocalDate employmentStartDate,
        LocalDate employmentEndDate,
        String verificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmploymentResponse from(Employment employment) {
        return new EmploymentResponse(
                employment.getId(),
                employment.getStudent().getId(),
                employment.getOrganization().getId(),
                employment.getJobTitle(),
                employment.getMonthlySalary(),
                employment.getEmploymentStartDate(),
                employment.getEmploymentEndDate(),
                employment.getVerificationStatus(),
                employment.getCreatedAt(),
                employment.getUpdatedAt()
        );
    }
}
