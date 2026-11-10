package mw.loancompliance.repayment;
import mw.loancompliance.loan.*;
import mw.loancompliance.student.*;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.audit.AuditService;
import mw.loancompliance.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Service @Transactional public class RepaymentService {
    final PaymentSubmissionRepository submissions;
    final BankTransactionRepository transactions;
    final BankStatementImportRepository imports;
    final RepaymentRecordRepository repayments;
    final LoanAccountRepository loans;
    final StudentRepository students;
    final UserRepository users;
    final AuditService audit;
    final NotificationService notifications;
    public RepaymentService(PaymentSubmissionRepository s,BankTransactionRepository t,BankStatementImportRepository i,RepaymentRecordRepository r,LoanAccountRepository l,StudentRepository st,UserRepository u,AuditService au,NotificationService no) {
        submissions=s;
        transactions=t;
        imports=i;
        repayments=r;
        loans=l;
        students=st;
        users=u;
        audit=au;
        notifications=no;
    }
    public PaymentSubmission submit(String email,BigDecimal amount,LocalDate date,String method,String bank,String tx,String ref,Long studentId,String month,boolean employer) {
        if(amount==null||amount.signum()<=0||date==null||tx==null||tx.isBlank()||ref==null||ref.isBlank())throw bad("Amount, date, transaction reference and payment reference are required");
        if(submissions.existsByTransactionReference(tx))throw new ResponseStatusException(HttpStatus.CONFLICT,"Duplicate payment submission");
        LoanAccount loan=loans.findByPaymentReference(ref).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan account not found for payment reference"));
        Student student=employer?students.findById(studentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found")):students.findByUser_EmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not found"));
        if(!employer&&!loan.getStudent().getId().equals(student.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Payment reference does not belong to you");
        if(employer&&!loan.getStudent().getId().equals(student.getId()))throw bad("Payment reference does not belong to the reported student");
        PaymentSubmission s=new PaymentSubmission();
        s.init(student,loan,amount,date,method,bank,tx,ref,employer,null,month);
        var saved=submissions.save(s);
        audit.record(email,employer?"EMPLOYER_PAYMENT_REPORTED":"PAYMENT_SUBMITTED","PAYMENT_SUBMISSION",saved.getId(),"Payment report submitted for verification");
        notifications.inApp(student.getUser(),employer?"EMPLOYER_PAYMENT_REPORTED":"PAYMENT_SUBMITTED","Payment submission received","Your payment information was submitted and is awaiting verification.");
        return saved;
    }
    public BankStatementImport importCsv(MultipartFile file,String email) {
        if(file==null||file.isEmpty())throw bad("CSV file is empty");
        BankStatementImport imp=imports.save(new BankStatementImport(file.getOriginalFilename()==null?"statement.csv":file.getOriginalFilename(),users.findByEmailIgnoreCase(email).map(x->x.getId()).orElse(null)));
        try(BufferedReader br=new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header=br.readLine();
            if(header==null)throw bad("CSV file is empty");
            String[] cols=header.toLowerCase(Locale.ROOT).split(",");
            Map<String,Integer> ix=new HashMap<>();
            for(int j=0;j<cols.length;j++)ix.put(cols[j].trim(),j);
            for(String required:List.of("transaction_reference","payment_reference","amount","payment_date","bank_name"))if(!ix.containsKey(required))throw bad("CSV required column missing: "+required);
            String line;
            while((line=br.readLine())!=null) {
                if(line.isBlank())continue;
                String[] v=line.split(",",-1);
                try {
                    String tx=v[ix.get("transaction_reference")].trim(),ref=v[ix.get("payment_reference")].trim();
                    BigDecimal amount=new BigDecimal(v[ix.get("amount")].trim());
                    LocalDate date=LocalDate.parse(v[ix.get("payment_date")].trim());
                    String bank=v[ix.get("bank_name")].trim();
                    if(tx.isEmpty()||ref.isEmpty()||bank.isEmpty()||amount.signum()<=0)throw new IllegalArgumentException();
                    if(transactions.findByTransactionReference(tx).isPresent()) {
                        imp.duplicate();
                        continue;
                    }
                    PaymentSubmission sub=submissions.findByTransactionReferenceAndPaymentReference(tx,ref).orElse(null);
                    BankTransaction bt=new BankTransaction(imp.getId(),tx,ref,amount,date,bank);
                    if(sub==null) {
                        bt.setStatus("UNMATCHED");
                        imp.addRow(false);
                    }
                    else {
                        boolean amountsMatch=sub.getAmount().compareTo(amount)==0;
                        bt.setStatus(amountsMatch?"MATCHED":"UNMATCHED");
                        if(amountsMatch) {
                            sub.setStatus("MATCHED");
                            imp.addRow(true);
                        }
                        else {
                            imp.addRow(false);
                        }
                    }
                    BankTransaction saved=transactions.save(bt);
                    if(sub!=null&&"MATCHED".equals(saved.getStatus())) {
                        sub.setMatchedBankTransactionId(saved.getId());
                        saved.setPaymentSubmissionId(sub.getId());
                    }
                }
                catch(RuntimeException ex) {
                    throw bad("CSV contains an invalid row");
                }
            }
        }
        catch(IOException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Could not read CSV",ex);
        }
        return imp;
    }
    public PaymentSubmission match(Long id,Long transactionId,String reason) {
        var s=submission(id);
        var t=transactions.findById(transactionId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Bank transaction not found"));
        if(t.getPaymentSubmissionId()!=null&&!t.getPaymentSubmissionId().equals(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Transaction already allocated");
        if(!s.getPaymentReference().equals(t.getPaymentReference()))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Reference mismatch");
        s.setMatchedBankTransactionId(transactionId);
        s.setStatus(s.getAmount().compareTo(t.getAmount())==0?"MATCHED":"AMOUNT_MISMATCH");
        s.setDecisionReason(reason);
        t.setPaymentSubmissionId(id);
        t.setStatus("MATCHED");
        return s;
    }
    public RepaymentRecord verify(Long id,String email,String note) {
        var s=submission(id);
        if(!"MATCHED".equals(s.getStatus())||s.getMatchedBankTransactionId()==null)throw bad("Submission must be matched before verification");
        var t=transactions.findById(s.getMatchedBankTransactionId()).orElseThrow();
        if(t.getPaymentSubmissionId()==null||!t.getPaymentSubmissionId().equals(id)||!t.getPaymentReference().equals(s.getPaymentReference())||t.getAmount().compareTo(s.getAmount())!=0)throw bad("Matched transaction does not validate");
        if(s.getAmount().compareTo(s.getLoanAccount().getOutstandingBalance())>0)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Payment exceeds outstanding balance");
        s.setStatus("VERIFIED");
        s.setVerificationNote(note);
        s.getLoanAccount().setOutstandingBalance(s.getLoanAccount().getOutstandingBalance().subtract(s.getAmount()));
        if(s.getLoanAccount().getOutstandingBalance().signum()==0)s.getLoanAccount().setLoanStatus(LoanStatus.PAID);
        t.setStatus("ALLOCATED");
        notifyStudentWhenFullyRepaid(s.getStudent());
        RepaymentRecord repayment = repayments.save(
                RepaymentRecord.from(s,users.findByEmailIgnoreCase(email).map(x->x.getId()).orElse(null)));
        audit.record(email,"PAYMENT_VERIFIED","PAYMENT_SUBMISSION",s.getId(),
                "Payment submission verified against an imported bank transaction");
        notifyPaymentVerified(s);
        return repayment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissingVerificationNotifications() {
        submissions.findAllByStatus("VERIFIED").forEach(this::notifyPaymentVerified);
    }

    private void notifyPaymentVerified(PaymentSubmission submission) {
        String referenceText = "Transaction reference: " + submission.getTransactionReference();
        notifications.inAppOnce(
                submission.getStudent().getUser(),
                "PAYMENT_VERIFIED",
                "Repayment confirmed",
                "Your payment has been verified and applied to your loan balance. " + referenceText,
                referenceText);
    }

    private void notifyStudentWhenFullyRepaid(Student student) {
        List<LoanAccount> studentLoans =
                loans.findAllByStudent_IdOrderByAcademicYearDesc(student.getId());
        boolean fullyRepaid = !studentLoans.isEmpty()
                && studentLoans.stream().allMatch(loan -> loan.getOutstandingBalance().signum() == 0);
        if (!fullyRepaid || "COMPLETED_REPAYMENT".equals(student.getLastNotifiedComplianceStatus())) {
            return;
        }

        notifications.inApp(student.getUser(), "COMPLIANCE_STATUS_CHANGED",
                "Repayment completed", "Your repayment status is now completed repayment. "
                        + "All loan accounts have been repaid in full.");
        student.setLastNotifiedComplianceStatus("COMPLETED_REPAYMENT");
    }
    public PaymentSubmission reject(Long id,String why) {
        var s=submission(id);
        if("VERIFIED".equals(s.getStatus())||"REJECTED".equals(s.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Submission already processed");
        s.setStatus("REJECTED");
        s.setDecisionReason(why);
        return s;
    }
    public List<PaymentSubmission> own(String email) {
        return submissions.findAllByStudent_User_EmailIgnoreCaseOrderByCreatedAtDesc(email);
    }
    public List<PaymentSubmission> all() {
        return submissions.findAllByOrderByCreatedAtDesc();
    }
    public List<BankTransaction> transactions() {
        return transactions.findAllByOrderByIdDesc();
    }
    public List<BankStatementImport> history() {
        return imports.findAll();
    }
    public List<RepaymentRecord> repayments() {
        return repayments.findAllByOrderByPaymentDateDesc();
    }
    public List<LoanAccount> accounts(String email) {
        return loans.findAllByStudent_User_EmailIgnoreCaseOrderByAcademicYearDesc(email);
    }
    public List<RepaymentRecord> ownRepayments(String email) {
        var student=students.findByUser_EmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not found"));
        return repayments.findAllByStudentIdOrderByPaymentDateDesc(student.getId());
    }
    public RepaymentRecord reverse(Long id,String email,String reason) {
        if(reason==null||reason.isBlank())throw bad("A reversal reason is required");
        var r=repayments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Repayment not found"));
        if(!"CONFIRMED".equals(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Repayment is already reversed");
        var account=loans.findById(r.getLoanAccountId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan account not found"));
        account.setOutstandingBalance(account.getOutstandingBalance().add(r.getAmount()));
        if(account.getLoanStatus()==LoanStatus.PAID)account.setLoanStatus(LoanStatus.ACTIVE);
        r.reverse(users.findByEmailIgnoreCase(email).map(u->u.getId()).orElse(null),reason);
        return r;
    }
    private PaymentSubmission submission(Long id) {
        return submissions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment submission not found"));
    }
    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);
    }
}
