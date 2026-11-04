package mw.loancompliance.loan.dto;

import mw.loancompliance.loan.LoanAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LoanAccountResponse(Long id, Long studentId, Integer academicYear,
        BigDecimal principalAmount, BigDecimal outstandingBalance,
        LocalDate disbursementDate, String loanStatus, Instant createdAt, Instant updatedAt) {
    public static LoanAccountResponse from(LoanAccount loan) {
        return new LoanAccountResponse(loan.getId(), loan.getStudent().getId(), loan.getAcademicYear(),
                loan.getPrincipalAmount(), loan.getOutstandingBalance(), loan.getDisbursementDate(),
                loan.getLoanStatus(), loan.getCreatedAt(), loan.getUpdatedAt());
    }
}
