package mw.loancompliance.loan;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        boolean loanBoardAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LOAN_BOARD_ADMIN"::equals);
        return PageResponse.from(loanAccountService
                        .getLoanAccounts(authentication.getName(), loanBoardAdmin, studentId).stream()
                        .map(LoanAccountResponse::from)
                        .toList(), page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public LoanAccountResponse getLoanAccount(
            @PathVariable @Positive Long id,
            Authentication authentication) {
        boolean loanBoardAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_LOAN_BOARD_ADMIN"::equals);
        return LoanAccountResponse.from(loanAccountService.getLoanAccount(
                id, authentication.getName(), loanBoardAdmin));
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
