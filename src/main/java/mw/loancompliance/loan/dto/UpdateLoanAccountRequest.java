package mw.loancompliance.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import mw.loancompliance.loan.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLoanAccountRequest(
        @Min(1900) @Max(2200) Integer academicYear,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal principalAmount,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal outstandingBalance,
        LocalDate disbursementDate,
        LocalDate repaymentStartDate,
        @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal monthlyInstallment,
        LoanStatus loanStatus
) {}
