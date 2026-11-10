package mw.loancompliance.loan.dto;

import mw.loancompliance.loan.LoanAccount;
import mw.loancompliance.loan.LoanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoanAccountResponse(Long id, Long studentId, Integer academicYear,
        BigDecimal principalAmount, BigDecimal outstandingBalance,
        LocalDate disbursementDate, LoanStatus loanStatus, LocalDate repaymentStartDate,
        BigDecimal monthlyInstallment, String paymentReference, Instant createdAt, Instant updatedAt) {
    public static LoanAccountResponse from(LoanAccount loan) {
        return new LoanAccountResponse(loan.getId(), loan.getStudent().getId(), loan.getAcademicYear(),
                loan.getPrincipalAmount(), loan.getOutstandingBalance(), loan.getDisbursementDate(),
                loan.getLoanStatus(), loan.getRepaymentStartDate(), loan.getMonthlyInstallment(),
                loan.getPaymentReference(), loan.getCreatedAt(), loan.getUpdatedAt());
    }
}
