package mw.loancompliance.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLoanAccountRequest(
        @Min(1900) @Max(2200) Integer academicYear,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal principalAmount,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal outstandingBalance,
        LocalDate disbursementDate,
        @Pattern(regexp = "ACTIVE|PAID|SUSPENDED") String loanStatus
) {}
