package mw.loancompliance.repayment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
@Entity @Table(name="repayments") public class RepaymentRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="student_id") Long studentId;
    @Column(name="amount") BigDecimal amount;
    @Column(name="payment_date") Instant paymentDate;
    @Column(name="payment_method") String paymentMethod;
    @Column(name="payment_reference") String paymentReference;
    @Column(name="payment_status") String paymentStatus;
    @Column(name="recorded_by") Long recordedBy;
    @Column(name="loan_account_id") Long loanAccountId;
    @Column(name="submission_id") Long submissionId;
    String status;
    @Column(name="reversed_at") Instant reversedAt;
    @Column(name="reversal_reason") String reversalReason;
    @Column(name="reversed_by") Long reversedBy;
    @Column(name="created_at",insertable=false,updatable=false) Instant createdAt;
    protected RepaymentRecord() {
    }
    public static RepaymentRecord from(PaymentSubmission s,Long admin) {
        RepaymentRecord r=new RepaymentRecord();
        r.studentId=s.getStudent().getId();
        r.amount=s.getAmount();
        r.paymentDate=s.getPaymentDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        r.paymentMethod=s.getPaymentMethod();
        r.paymentReference=s.getPaymentReference();
        r.paymentStatus="CONFIRMED";
        r.recordedBy=admin;
        r.loanAccountId=s.getLoanAccount().getId();
        r.submissionId=s.getId();
        r.status="CONFIRMED";
        return r;
    }
    public Long getId() {
        return id;
    }
    public Long getStudentId() {
        return studentId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public Instant getPaymentDate() {
        return paymentDate;
    }
    public String getStatus() {
        return status;
    }
    public Long getLoanAccountId() {
        return loanAccountId;
    }
    public void reverse(Long actor,String reason) {
        status="REVERSED";
        reversedAt=Instant.now();
        reversedBy=actor;
        reversalReason=reason;
    }
}
