package mw.loancompliance.repayment;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import mw.loancompliance.student.Student;
import mw.loancompliance.loan.LoanAccount;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="payment_submissions")
public class PaymentSubmission {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_id") Student student;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="loan_account_id") LoanAccount loanAccount;
    BigDecimal amount;
    @Column(name="payment_date") LocalDate paymentDate;
    @Column(name="payment_method") String paymentMethod;
    @Column(name="bank_name") String bankName;
    @Column(name="transaction_reference") String transactionReference;
    @Column(name="payment_reference") String paymentReference;
    String status;
    @Column(name="employer_report") boolean employerReport;
    @Column(name="organization_id") Long organizationId;
    @Column(name="deduction_month") String deductionMonth;
    @Column(name="matched_bank_transaction_id") Long matchedBankTransactionId;
    @Column(name="verification_note") String verificationNote;
    @Column(name="decision_reason") String decisionReason;
    @Column(name="created_at",insertable=false,updatable=false) Instant createdAt;
    protected PaymentSubmission() {
    }
    public Long getId() {
        return id;
    }
    public Student getStudent() {
        return student;
    }
    public LoanAccount getLoanAccount() {
        return loanAccount;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }
    public String getTransactionReference() {
        return transactionReference;
    }
    public String getPaymentReference() {
        return paymentReference;
    }
    public String getStatus() {
        return status;
    }
    public Long getMatchedBankTransactionId() {
        return matchedBankTransactionId;
    }
    public void setStatus(String v) {
        status=v;
    }
    public void setMatchedBankTransactionId(Long v) {
        matchedBankTransactionId=v;
    }
    public void setVerificationNote(String v) {
        verificationNote=v;
    }
    public void setDecisionReason(String v) {
        decisionReason=v;
    }
    public void init(Student s,LoanAccount l,BigDecimal a,LocalDate d,String m,String bank,String tx,String ref,boolean employer,Long org,String month) {
        student=s;
        loanAccount=l;
        amount=a;
        paymentDate=d;
        paymentMethod=m;
        bankName=bank;
        transactionReference=tx;
        paymentReference=ref;
        status="PENDING_VERIFICATION";
        employerReport=employer;
        organizationId=org;
        deductionMonth=month;
    }
}
