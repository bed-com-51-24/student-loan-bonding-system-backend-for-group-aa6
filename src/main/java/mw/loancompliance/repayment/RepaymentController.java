package mw.loancompliance.repayment;
import mw.loancompliance.loan.LoanAccount;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@RestController @SecurityRequirement(name = "bearerAuth") public class RepaymentController {
    final RepaymentService service;
    @Value("${app.payment.bank-name:}") String bankName;
    @Value("${app.payment.bank-account-number:}") String bankAccountNumber;
    public RepaymentController(RepaymentService s) {
        service=s;
    }
    public record SubmitPaymentRequest(@NotNull @Positive BigDecimal amount,@NotNull LocalDate paymentDate,@NotBlank String paymentMethod,String bankName,@NotBlank String transactionReference,@NotBlank String paymentReference) {
    }
    public record EmployerSubmit(@NotNull @Positive Long studentId,@NotNull @Positive BigDecimal amount,@NotNull LocalDate paymentDate,@NotBlank String paymentMethod,String bankName,@NotBlank String transactionReference,@NotBlank String paymentReference,String deductionMonth) {
    }
    public record Match(@NotNull Long bankTransactionId,@NotBlank String reason) {
    }
    public record Decision(String verificationNote,String reason) {
    }
    public record PaymentInstruction(Long loanAccountId,String loanAccountNumber,String paymentReference,String bankName,String bankAccountNumber,java.math.BigDecimal outstandingBalance,String currency) {
    }
    @GetMapping("/api/payments/instructions/me") @PreAuthorize("hasRole('STUDENT')") public List<PaymentInstruction> instructions(Authentication a) {
        return service.accounts(a.getName()).stream().map(x->new PaymentInstruction(x.getId(),String.format(java.util.Locale.ROOT,"LA-%05d",x.getId()),x.getPaymentReference(),blankToNull(bankName),blankToNull(bankAccountNumber),x.getOutstandingBalance(),"MWK")).toList();
    }
    private String blankToNull(String value) {
        return value==null||value.isBlank()?null:value;
    }
    @PostMapping("/api/payment-submissions") @PreAuthorize("hasRole('STUDENT')") public PaymentSubmission submit(@RequestBody @Valid SubmitPaymentRequest q,Authentication a) {
        return service.submit(a.getName(),q.amount(),q.paymentDate(),q.paymentMethod(),q.bankName(),q.transactionReference(),q.paymentReference(),null,null,false);
    }
    @PostMapping("/api/employer-payment-submissions") @PreAuthorize("hasRole('EMPLOYER_ADMIN')") public PaymentSubmission employer(@RequestBody @Valid EmployerSubmit q,Authentication a) {
        return service.submit(a.getName(),q.amount(),q.paymentDate(),q.paymentMethod(),q.bankName(),q.transactionReference(),q.paymentReference(),q.studentId(),q.deductionMonth(),true);
    }
    @GetMapping("/api/payment-submissions/me") @PreAuthorize("hasRole('STUDENT')") public List<PaymentSubmission> own(Authentication a) {
        return service.own(a.getName());
    }
    @GetMapping("/api/payment-submissions") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<PaymentSubmission> all() {
        return service.all();
    }
    @PostMapping(value="/api/bank-statements/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public BankStatementImport importCsv(@RequestPart("file") MultipartFile file,Authentication a) {
        return service.importCsv(file,a.getName());
    }
    @GetMapping("/api/bank-transactions") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<BankTransaction> transactions() {
        return service.transactions();
    }
    @GetMapping("/api/bank-statements") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<BankStatementImport> imports() {
        return service.history();
    }
    @PatchMapping("/api/payment-submissions/{id}/match") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public PaymentSubmission match(@PathVariable Long id,@RequestBody @Valid Match q) {
        return service.match(id,q.bankTransactionId(),q.reason());
    }
    @PatchMapping("/api/payment-submissions/{id}/verify") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public RepaymentRecord verify(@PathVariable Long id,@RequestBody Decision q,Authentication a) {
        return service.verify(id,a.getName(),q.verificationNote());
    }
    @PatchMapping("/api/payment-submissions/{id}/reject") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public PaymentSubmission reject(@PathVariable Long id,@RequestBody Decision q) {
        return service.reject(id,q.reason());
    }
    @GetMapping("/api/repayments") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public List<RepaymentRecord> repayments() {
        return service.repayments();
    }
    @GetMapping("/api/repayments/me") @PreAuthorize("hasRole('STUDENT')") public List<RepaymentRecord> myRepayments(Authentication a) {
        return service.ownRepayments(a.getName());
    }
    @PostMapping("/api/repayments/{id}/reverse") @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')") public RepaymentRecord reverse(@PathVariable Long id,@RequestBody Decision q,Authentication a) {
        return service.reverse(id,a.getName(),q.reason());
    }
}
