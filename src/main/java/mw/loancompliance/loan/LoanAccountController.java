package mw.loancompliance.loan;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import mw.loancompliance.loan.dto.CreateLoanAccountRequest;
import mw.loancompliance.loan.dto.LoanAccountResponse;
import mw.loancompliance.loan.dto.UpdateLoanAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/loan-accounts")
@SecurityRequirement(name = "bearerAuth")
public class LoanAccountController {
    private final LoanAccountService loanService;

    public LoanAccountController(LoanAccountService loanService) { this.loanService = loanService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public LoanAccountResponse create(@Valid @RequestBody CreateLoanAccountRequest request) {
        return LoanAccountResponse.from(loanService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public List<LoanAccountResponse> findByStudent(@RequestParam Long studentId) {
        return loanService.findByStudent(studentId).stream().map(LoanAccountResponse::from).toList();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<LoanAccountResponse> findMine(Authentication authentication) {
        return loanService.findMine(authentication.getName()).stream().map(LoanAccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public LoanAccountResponse findById(@PathVariable Long id, Authentication authentication) {
        LoanAccount loan = isLoanBoardAdmin(authentication)
                ? loanService.findById(id)
                : loanService.findByIdForOwner(id, authentication.getName());
        return LoanAccountResponse.from(loan);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public LoanAccountResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLoanAccountRequest request) {
        return LoanAccountResponse.from(loanService.update(id, request));
    }

    private boolean isLoanBoardAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LOAN_BOARD_ADMIN"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler(LoanAccountAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(LoanAccountAccessDeniedException exception) { return Map.of("error", exception.getMessage()); }
}
