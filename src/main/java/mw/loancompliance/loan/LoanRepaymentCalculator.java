package mw.loancompliance.loan;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class LoanRepaymentCalculator {
    public static final BigDecimal INTEREST_RATE = new BigDecimal("0.15");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int REPAYMENT_MONTHS = 60;
    private static final int CURRENCY_SCALE = 2;

    private LoanRepaymentCalculator() { }

    public static BigDecimal oneTimeInterest(BigDecimal principal) {
        return principal.multiply(INTEREST_RATE)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal minimumMonthlyPayment(
            BigDecimal balance, LocalDate repaymentStartDate, LocalDate today) {
        if (balance == null || balance.signum() <= 0 || repaymentStartDate == null
                || today.isBefore(repaymentStartDate)) {
            return BigDecimal.ZERO.setScale(CURRENCY_SCALE);
        }

        int remainingMonths = remainingInstallments(repaymentStartDate, today);
        if (remainingMonths == 0) {
            return balance.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal installment = balance.divide(BigDecimal.valueOf(remainingMonths),
                MathContext.DECIMAL128).setScale(CURRENCY_SCALE, RoundingMode.CEILING);
        return installment.min(balance).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private static int remainingInstallments(LocalDate startDate, LocalDate today) {
        LocalDate deadline = startDate.plusYears(5);
        if (today.isAfter(deadline)) {
            return 0;
        }

        int remaining = 0;
        for (int installment = 1; installment <= REPAYMENT_MONTHS; installment++) {
            LocalDate dueDate = startDate.plusMonths(installment);
            if (!dueDate.isBefore(today)) {
                remaining++;
            }
        }
        return remaining;
    }

}
