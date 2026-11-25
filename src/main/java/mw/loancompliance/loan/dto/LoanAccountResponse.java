package mw.loancompliance.loan.dto;

import mw.loancompliance.loan.LoanAccount;
import mw.loancompliance.loan.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoanAccountResponse(
        Long id,
        Long studentId,
        Integer academicYear,
        BigDecimal principalAmount,
        BigDecimal outstandingBalance,
        String paymentReference,
        LocalDate disbursementDate,
        LoanStatus loanStatus,
        LocalDate repaymentStartDate,
        BigDecimal monthlyInstallment,
        Instant createdAt,
        Instant updatedAt
) {
    public static LoanAccountResponse from(LoanAccount account) {
        return new LoanAccountResponse(
                account.getId(),
                account.getStudent().getId(),
                account.getAcademicYear(),
                account.getPrincipalAmount(),
                account.getOutstandingBalance(),
                account.getPaymentReference(),
                account.getDisbursementDate(),
                account.getLoanStatus(),
                account.getRepaymentStartDate(),
                account.getMonthlyInstallment(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
