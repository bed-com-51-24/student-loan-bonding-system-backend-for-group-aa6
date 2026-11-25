package mw.loancompliance.repayment;

import java.math.BigDecimal;
import java.util.List;

public record StudentRepaymentResponse(
        BigDecimal amount,
        String paymentMethod,
        String status,
        List<Allocation> allocations) {

    public record Allocation(
            Long repaymentId,
            Long loanAccountId,
            String paymentReference,
            BigDecimal amount) {
    }
}
