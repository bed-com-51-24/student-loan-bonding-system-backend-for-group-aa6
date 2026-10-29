package mw.loancompliance.employment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmploymentRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Positive Long organizationId,
        @Size(max = 150) String jobTitle,
        @DecimalMin(value = "0.00") BigDecimal monthlySalary,
        @NotNull LocalDate employmentStartDate,
        LocalDate employmentEndDate
) {
}
