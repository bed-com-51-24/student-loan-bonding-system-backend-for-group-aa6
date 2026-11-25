package mw.loancompliance.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.loan.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanAccountRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Min(1900) @Max(2200) Integer academicYear,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal principalAmount,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal outstandingBalance,
        LocalDate disbursementDate,
        @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal monthlyInstallment,
        @NotNull LoanStatus loanStatus
) {}
