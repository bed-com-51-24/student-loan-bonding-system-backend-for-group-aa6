package mw.loancompliance.repayment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="bank_transactions") public class BankTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="import_id") Long importId;
    @Column(name="transaction_reference") String transactionReference;
    @Column(name="payment_reference") String paymentReference;
    BigDecimal amount;
    @Column(name="payment_date") LocalDate paymentDate;
    @Column(name="bank_name") String bankName;
    String status;
    @Column(name="payment_submission_id") Long paymentSubmissionId;
    protected BankTransaction() {
    }
    public BankTransaction(Long i,String t,String p,BigDecimal a,LocalDate d,String b) {
        importId=i;
        transactionReference=t;
        paymentReference=p;
        amount=a;
        paymentDate=d;
        bankName=b;
    }
    public Long getId() {
        return id;
    }
    public String getTransactionReference() {
        return transactionReference;
    }
    public String getPaymentReference() {
        return paymentReference;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public String getStatus() {
        return status;
    }
    public Long getPaymentSubmissionId() {
        return paymentSubmissionId;
    }
    public void setStatus(String v) {
        status=v;
    }
    public void setPaymentSubmissionId(Long v) {
        paymentSubmissionId=v;
    }
}
