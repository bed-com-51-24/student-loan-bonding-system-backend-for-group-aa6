package mw.loancompliance.loan;

import mw.loancompliance.loan.dto.CreateLoanAccountRequest;
import mw.loancompliance.loan.dto.UpdateLoanAccountRequest;
import mw.loancompliance.repayment.RepaymentRecord;
import mw.loancompliance.repayment.RepaymentRecordRepository;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoanAccountService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Blantyre");

    private final LoanAccountRepository loanAccountRepository;
    private final StudentRepository studentRepository;
    private final RepaymentRecordRepository repaymentRepository;

    public LoanAccountService(LoanAccountRepository loanAccountRepository,
            StudentRepository studentRepository, RepaymentRecordRepository repaymentRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.studentRepository = studentRepository;
        this.repaymentRepository = repaymentRepository;
    }

    @Transactional
    public LoanAccount createLoanAccount(CreateLoanAccountRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + request.studentId()));

        if (loanAccountRepository.existsByStudent_IdAndAcademicYear(
                request.studentId(), request.academicYear())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A loan account already exists for this student and academic year");
        }

        LoanAccount account = new LoanAccount();
        account.setStudent(student);
        account.setAcademicYear(request.academicYear());
        account.setPrincipalAmount(request.principalAmount().setScale(2, RoundingMode.HALF_UP));
        account.setOutstandingBalance(account.getPrincipalAmount().add(
                LoanRepaymentCalculator.oneTimeInterest(account.getPrincipalAmount())));
        account.setDisbursementDate(request.disbursementDate());
        account.setLoanStatus(request.loanStatus());
        account.setPaymentReference("TMP-" + java.util.UUID.randomUUID()
                .toString().replace("-", "").substring(0, 24));
        account.setRepaymentStartDate(student.calculateRepaymentStartDate());
        account.setInterestAppliedOn(request.disbursementDate() == null
                ? LocalDate.now(BUSINESS_ZONE) : request.disbursementDate());

        LoanAccount saved = loanAccountRepository.save(account);
        saved.setPaymentReference(String.format(java.util.Locale.ROOT, "SLC-%05d", saved.getId()));
        refreshInterest(saved, LocalDate.now(BUSINESS_ZONE));
        return loanAccountRepository.save(saved);
    }

    @Transactional
    public List<LoanAccount> getLoanAccountsForStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Student not found with ID: " + studentId);
        }
        return refreshStudentLoans(studentId);
    }

    @Transactional
    public List<LoanAccount> getMyLoanAccounts(String email) {
        List<LoanAccount> accounts = loanAccountRepository
                .findAllByStudent_User_EmailIgnoreCaseOrderByAcademicYearDesc(email);
        refreshInterest(accounts, LocalDate.now(BUSINESS_ZONE));
        return accounts;
    }

    @Transactional
    public List<LoanAccount> getLoanAccounts(String email, boolean loanBoardAdmin, Long studentId) {
        if (!loanBoardAdmin) {
            Student student = studentRepository.findByUser_EmailIgnoreCase(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Student profile not found"));
            if (studentId != null && !student.getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Students may only view their own loan records");
            }
            return getMyLoanAccounts(email);
        }
        if (studentId != null) {
            return getLoanAccountsForStudent(studentId);
        }

        List<LoanAccount> accounts = loanAccountRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(
                        (LoanAccount account) -> account.getStudent().getId())
                        .thenComparing(LoanAccount::getAcademicYear,
                                java.util.Comparator.reverseOrder()))
                .toList();
        refreshInterest(accounts, LocalDate.now(BUSINESS_ZONE));
        return accounts;
    }

    @Transactional
    public LoanAccount getLoanAccount(Long id, String email, boolean loanBoardAdmin) {
        LoanAccount account = findLoanAccount(id);
        if (!loanBoardAdmin
                && !account.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You may only view your own loan accounts");
        }
        refreshInterest(account, LocalDate.now(BUSINESS_ZONE));
        return account;
    }

    @Transactional
    public LoanAccount updateLoanAccount(Long id, UpdateLoanAccountRequest request) {
        LoanAccount account = findLoanAccount(id);
        if (request.academicYear() == null
                && request.principalAmount() == null
                && request.disbursementDate() == null
                && request.repaymentStartDate() == null
                && request.loanStatus() == null) {
            throw new IllegalArgumentException("At least one loan field is required");
        }

        Integer academicYear = request.academicYear() == null
                ? account.getAcademicYear() : request.academicYear();
        if (request.academicYear() != null
                && loanAccountRepository.existsByStudent_IdAndAcademicYearAndIdNot(
                        account.getStudent().getId(), academicYear, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A loan account already exists for this student and academic year");
        }

        if (request.principalAmount() != null
                && request.principalAmount().compareTo(account.getPrincipalAmount()) != 0) {
            if (repaymentRepository.existsByLoanAccountIdAndStatus(id, "CONFIRMED")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Principal cannot be changed after a confirmed repayment; create a correction workflow instead");
            }
            account.setPrincipalAmount(request.principalAmount().setScale(2, RoundingMode.HALF_UP));
            account.setOutstandingBalance(account.getPrincipalAmount().add(
                    LoanRepaymentCalculator.oneTimeInterest(account.getPrincipalAmount())));
            account.setInterestAppliedOn(LocalDate.now(BUSINESS_ZONE));
        }

        account.setAcademicYear(academicYear);
        if (request.disbursementDate() != null) {
            account.setDisbursementDate(request.disbursementDate());
        }
        if (request.repaymentStartDate() != null
                && !request.repaymentStartDate().equals(account.getRepaymentStartDate())) {
            account.setRepaymentStartDate(request.repaymentStartDate());
        }
        if (request.loanStatus() != null) {
            account.setLoanStatus(request.loanStatus());
        }
        refreshInterest(account, LocalDate.now(BUSINESS_ZONE));
        return loanAccountRepository.save(account);
    }

    @Transactional
    public List<LoanAccount> refreshStudentLoans(Long studentId) {
        List<LoanAccount> accounts = loanAccountRepository
                .findAllByStudent_IdOrderByAcademicYearAsc(studentId);
        refreshInterest(accounts, LocalDate.now(BUSINESS_ZONE));
        return accounts;
    }

    @Transactional
    public BigDecimal minimumMonthlyPayment(Long studentId) {
        List<LoanAccount> accounts = refreshStudentLoans(studentId);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (LoanAccount account : accounts) {
            if (account.getOutstandingBalance().signum() == 0
                    || (account.getRepaymentStartDate() != null
                    && today.isBefore(account.getRepaymentStartDate()))) {
                continue;
            }
            if (account.getRepaymentStartDate() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An outstanding loan has no repayment start date; the minimum cannot be calculated");
            }
            BigDecimal minimum = LoanRepaymentCalculator.minimumMonthlyPayment(
                    account.getOutstandingBalance(), account.getRepaymentStartDate(), today);
            account.setMonthlyInstallment(minimum.signum() == 0 ? null : minimum);
            total = total.add(minimum);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void refreshAllLoanBalances() {
        refreshInterest(loanAccountRepository.findAll(), LocalDate.now(BUSINESS_ZONE));
    }

    private void refreshInterest(List<LoanAccount> accounts, LocalDate today) {
        accounts.forEach(account -> refreshInterest(account, today));
    }

    private void refreshInterest(LoanAccount account, LocalDate today) {
        LocalDate start = account.getRepaymentStartDate();
        if (account.getInterestAppliedOn() == null) {
            rebuildLegacyBalance(account, today);
        }
        if (start == null) {
            account.setMonthlyInstallment(null);
            return;
        }

        LocalDate currentStartDate = account.getRepaymentStartDate();
        BigDecimal minimum = LoanRepaymentCalculator.minimumMonthlyPayment(
                account.getOutstandingBalance(), currentStartDate, today);
        account.setMonthlyInstallment(minimum.signum() == 0 ? null : minimum);
        if (account.getOutstandingBalance().signum() == 0) {
            account.setLoanStatus(LoanStatus.PAID);
        } else if (account.getLoanStatus() == LoanStatus.PAID) {
            account.setLoanStatus(LoanStatus.ACTIVE);
        }
    }

    private void rebuildLegacyBalance(LoanAccount account, LocalDate today) {
        List<RepaymentRecord> confirmedPayments = repaymentRepository
                .findAllByLoanAccountIdAndStatusOrderByPaymentDateAsc(account.getId(), "CONFIRMED");
        BigDecimal balance = account.getPrincipalAmount().add(
                LoanRepaymentCalculator.oneTimeInterest(account.getPrincipalAmount()));
        for (RepaymentRecord payment : confirmedPayments) {
            LocalDate paymentDate = payment.getPaymentDate().atZone(BUSINESS_ZONE).toLocalDate();
            if (!paymentDate.isAfter(today)) {
                balance = subtractPayment(balance, payment.getAmount());
            }
        }

        account.setOutstandingBalance(balance.setScale(2, RoundingMode.HALF_UP));
        LocalDate appliedOn = account.getDisbursementDate();
        if (appliedOn == null && account.getCreatedAt() != null) {
            appliedOn = account.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();
        }
        account.setInterestAppliedOn(appliedOn == null ? today : appliedOn);
    }

    private BigDecimal subtractPayment(BigDecimal balance, BigDecimal payment) {
        return balance.subtract(payment).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private LoanAccount findLoanAccount(Long id) {
        return loanAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Loan account not found with ID: " + id));
    }

}
