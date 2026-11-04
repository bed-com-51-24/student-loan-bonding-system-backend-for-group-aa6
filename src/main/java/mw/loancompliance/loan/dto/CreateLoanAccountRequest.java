package mw.loancompliance.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanAccountRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Min(1900) @Max(2200) Integer academicYear,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal principalAmount,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal outstandingBalance,
        LocalDate disbursementDate,
        @NotBlank @Pattern(regexp = "ACTIVE|PAID|SUSPENDED") String loanStatus
) {}
