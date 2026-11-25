package mw.loancompliance.repayment;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.loan.LoanAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class RepaymentController {
    private final RepaymentService service;

    @Value("${app.payment.bank-name:}")
    private String bankName;
    @Value("${app.payment.bank-account-number:}")
    private String bankAccountNumber;

    public RepaymentController(RepaymentService service) {
        this.service = service;
    }

    public record PaymentInstruction(Long loanAccountId, String loanAccountNumber,
            String repaymentIdentifier, String paymentReference, String bankName,
            String bankAccountNumber, BigDecimal outstandingBalance, String currency) { }
    public record ReverseRequest(@NotBlank String reason) { }
    public record MinimumPaymentResponse(BigDecimal minimumMonthlyPayment, String currency) { }
    public record InAppPaymentRequest(@NotNull @Positive @Digits(integer = 12, fraction = 2)
            BigDecimal amount,
            @NotBlank @Pattern(regexp = "PAYCHANGU|AIRTEL_MONEY|TNM_MPAMBA") String paymentMethod) { }

    @GetMapping("/payment-instructions")
    @PreAuthorize("hasRole('STUDENT')")
    public List<PaymentInstruction> instructions(Authentication authentication) {
        String identifier = service.repaymentIdentifier(authentication.getName());
        return service.accounts(authentication.getName()).stream()
                .map(loan -> toPaymentInstruction(loan, identifier)).toList();
    }

    @GetMapping("/repayments")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public PageResponse<RepaymentRecord> repayments(
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LOAN_BOARD_ADMIN"::equals);
        List<RepaymentRecord> records;
        if (admin) {
            records = service.repayments();
            if (studentId != null) {
                records = records.stream()
                        .filter(record -> studentId.equals(record.getStudentId())).toList();
            }
        } else {
            if (studentId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Students cannot select a studentId; their account determines the scope");
            }
            records = service.ownRepayments(authentication.getName());
        }
        if (status != null && !status.isBlank()) {
            records = records.stream()
                    .filter(record -> status.equalsIgnoreCase(record.getStatus())).toList();
        }
        return PageResponse.from(records, page, size);
    }

    @PostMapping("/repayments")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentRepaymentResponse postMyRepayment(Authentication authentication,
            @Valid @RequestBody InAppPaymentRequest request) {
        return service.postOwnRepayment(authentication.getName(), request.amount(), request.paymentMethod());
    }

    @PostMapping("/employment-records/{employmentRecordId}/repayments")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    public StudentRepaymentResponse postEmployeeRepayment(Authentication authentication,
            @PathVariable @Positive Long employmentRecordId,
            @Valid @RequestBody InAppPaymentRequest request) {
        return service.postEmployerRepayment(authentication.getName(), employmentRecordId,
                request.amount(), request.paymentMethod());
    }

    @GetMapping("/employment-records/{employmentRecordId}/repayments/minimum-payment")
    @PreAuthorize("hasRole('EMPLOYER_ADMIN')")
    public MinimumPaymentResponse employeeMinimumPayment(Authentication authentication,
            @PathVariable @Positive Long employmentRecordId) {
        BigDecimal amount = service.employerMinimumPayment(authentication.getName(), employmentRecordId);
        return new MinimumPaymentResponse(amount.setScale(2, RoundingMode.HALF_UP), "MWK");
    }

    @PostMapping("/repayments/{id}/reverse")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public RepaymentRecord reverse(@PathVariable @Positive Long id,
            @Valid @RequestBody ReverseRequest request, Authentication authentication) {
        return service.reverse(id, authentication.getName(), request.reason());
    }

    private PaymentInstruction toPaymentInstruction(LoanAccount loan, String repaymentIdentifier) {
        return new PaymentInstruction(loan.getId(), String.format(Locale.ROOT, "LA-%05d", loan.getId()),
                repaymentIdentifier, loan.getPaymentReference(), blankToNull(bankName),
                blankToNull(bankAccountNumber), loan.getOutstandingBalance(), "MWK");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
