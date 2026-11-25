package mw.loancompliance.loan;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import mw.loancompliance.common.PageResponse;
import mw.loancompliance.loan.dto.CreateLoanAccountRequest;
import mw.loancompliance.loan.dto.LoanAccountResponse;
import mw.loancompliance.loan.dto.UpdateLoanAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-records")
@SecurityRequirement(name = "bearerAuth")
public class LoanAccountController {

    private final LoanAccountService loanAccountService;

    public LoanAccountController(LoanAccountService loanAccountService) {
        this.loanAccountService = loanAccountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanAccountResponse createLoanAccount(@Valid @RequestBody CreateLoanAccountRequest request) {
        return LoanAccountResponse.from(loanAccountService.createLoanAccount(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public PageResponse<LoanAccountResponse> getLoanAccounts(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long loanId,
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) @Min(1900) Integer academicYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean loanBoardAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LOAN_BOARD_ADMIN"::equals);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }

        if (!loanBoardAdmin) {
            if (!"SELF".equalsIgnoreCase(scope)) {
                throw new IllegalArgumentException("Students must use scope=SELF");
            }
            if (studentId != null || loanId != null) {
                throw new IllegalArgumentException("Students cannot filter loan records by loanId or studentId");
            }
        } else if (scope != null && !"ALL".equalsIgnoreCase(scope)) {
            throw new IllegalArgumentException("Loan Board administrators may use scope=ALL or omit scope");
        }

        var accounts = loanAccountService
                .getLoanAccounts(authentication.getName(), loanBoardAdmin, studentId).stream()
                .filter(account -> loanId == null || account.getId().equals(loanId))
                .filter(account -> status == null || account.getLoanStatus() == status)
                .filter(account -> academicYear == null || account.getAcademicYear().equals(academicYear))
                .map(LoanAccountResponse::from)
                .toList();

        if (loanId != null && accounts.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Loan record not found with ID: " + loanId);
        }
        return PageResponse.from(accounts, page, size);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public LoanAccountResponse updateLoanAccount(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateLoanAccountRequest request) {
        return LoanAccountResponse.from(loanAccountService.updateLoanAccount(id, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }
}
