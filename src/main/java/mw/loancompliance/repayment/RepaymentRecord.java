package mw.loancompliance.repayment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import mw.loancompliance.loan.LoanAccount;
import mw.loancompliance.student.Student;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "repayments")
public class RepaymentRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "student_id") Long studentId;
    @Column(name = "amount") BigDecimal amount;
    @Column(name = "payment_date") Instant paymentDate;
    @Column(name = "payment_method") String paymentMethod;
    @Column(name = "payment_reference") String paymentReference;
    @Column(name = "payment_status") String paymentStatus;
    @Column(name = "recorded_by") Long recordedBy;
    @Column(name = "loan_account_id") Long loanAccountId;
    String status;
    @Column(name = "reversed_at") Instant reversedAt;
    @Column(name = "reversal_reason") String reversalReason;
    @Column(name = "reversed_by") Long reversedBy;
    @Column(name = "created_at", insertable = false, updatable = false) Instant createdAt;

    protected RepaymentRecord() { }

    public static RepaymentRecord fromInAppPayment(Student student, LoanAccount loan,
            BigDecimal amount, String paymentMethod, Long actorId, String paymentReference) {
        RepaymentRecord repayment = new RepaymentRecord();
        repayment.studentId = student.getId();
        repayment.amount = amount;
        repayment.paymentDate = Instant.now();
        repayment.paymentMethod = paymentMethod;
        repayment.paymentReference = paymentReference;
        repayment.paymentStatus = "CONFIRMED";
        repayment.recordedBy = actorId;
        repayment.loanAccountId = loan.getId();
        repayment.status = "CONFIRMED";
        return repayment;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaymentDate() { return paymentDate; }
    public String getStatus() { return status; }
    public Long getLoanAccountId() { return loanAccountId; }
    public void reverse(Long actor, String reason) {
        status = "REVERSED";
        reversedAt = Instant.now();
        reversedBy = actor;
        reversalReason = reason;
    }
}
