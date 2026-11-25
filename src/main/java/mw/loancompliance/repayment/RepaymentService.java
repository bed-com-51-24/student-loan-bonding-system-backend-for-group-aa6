package mw.loancompliance.repayment;

import mw.loancompliance.audit.AuditService;
import mw.loancompliance.common.BusinessEvent;
import mw.loancompliance.common.BusinessEventPublisher;
import mw.loancompliance.employment.EmploymentRecord;
import mw.loancompliance.employment.EmploymentRecordRepository;
import mw.loancompliance.loan.LoanAccount;
import mw.loancompliance.loan.LoanAccountRepository;
import mw.loancompliance.loan.LoanAccountService;
import mw.loancompliance.loan.LoanStatus;
import mw.loancompliance.notification.NotificationService;
import mw.loancompliance.organization.OrganizationRepository;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RepaymentService {
    private final RepaymentRecordRepository repayments;
    private final LoanAccountRepository loans;
    private final LoanAccountService loanAccountService;
    private final StudentRepository students;
    private final UserRepository users;
    private final EmploymentRecordRepository employment;
    private final BusinessEventPublisher events;

    public RepaymentService(RepaymentRecordRepository repayments, LoanAccountRepository loans,
            LoanAccountService loanAccountService,
            StudentRepository students, UserRepository users,
            EmploymentRecordRepository employment, BusinessEventPublisher events) {
        this.repayments = repayments;
        this.loans = loans;
        this.loanAccountService = loanAccountService;
        this.students = students;
        this.users = users;
        this.employment = employment;
        this.events = events;
    }

    @Transactional
    public List<LoanAccount> accounts(String email) {
        return loanAccountService.getMyLoanAccounts(email);
    }

    @Transactional(readOnly = true)
    public String repaymentIdentifier(String email) {
        return students.findByUser_EmailIgnoreCase(email)
                .map(Student::getRepaymentIdentifier)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student profile not found"));
    }

    @Transactional(readOnly = true)
    public List<RepaymentRecord> ownRepayments(String email) {
        Student student = studentForEmail(email);
        return repayments.findAllByStudentIdOrderByPaymentDateDesc(student.getId());
    }

    @Transactional(readOnly = true)
    public List<RepaymentRecord> repayments() {
        return repayments.findAllByOrderByPaymentDateDesc();
    }

    public StudentRepaymentResponse postOwnRepayment(String email, BigDecimal amount,
            String paymentMethod) {
        Student student = studentForEmail(email);
        Long actorId = student.getUser().getId();
        return applyPayment(student, email, actorId, amount, paymentMethod, "Student");
    }

    public StudentRepaymentResponse postEmployerRepayment(String email, Long employmentRecordId,
            BigDecimal amount, String paymentMethod) {
        EmploymentRecord record = employerLoanBeneficiary(email, employmentRecordId);
        Long actorId = users.findByEmailIgnoreCase(email).map(user -> user.getId()).orElse(null);
        return applyPayment(record.getStudent(), email, actorId, amount, paymentMethod,
                "Employer admin");
    }

    @Transactional
    public BigDecimal employerMinimumPayment(String email, Long employmentRecordId) {
        EmploymentRecord record = employerLoanBeneficiary(email, employmentRecordId);
        if (loans.findAllByStudent_IdOrderByAcademicYearAsc(record.getStudent().getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student has no loan account");
        }
        return loanAccountService.minimumMonthlyPayment(record.getStudent().getId());
    }

    private EmploymentRecord employerLoanBeneficiary(String email, Long employmentRecordId) {
        Long organizationId = users.findByEmailIgnoreCase(email)
                .map(user -> user.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No organization is assigned");
        }
        EmploymentRecord record = employment.findById(employmentRecordId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employment record not found"));
        if (record.getOrganization() == null
                || !organizationId.equals(record.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Employee belongs to another organization");
        }
        if (record.getStudent() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This employee is not matched to a loan beneficiary");
        }
        if (!"VERIFIED".equals(record.getStatus()) || record.getEndDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Repayment is allowed only for a currently employed student");
        }
        return record;
    }

    private StudentRepaymentResponse applyPayment(Student student, String actorEmail,
            Long actorId, BigDecimal amount, String paymentMethod, String actorDescription) {
        BigDecimal minimumMonthlyPayment = loanAccountService.minimumMonthlyPayment(student.getId());
        if (amount.compareTo(minimumMonthlyPayment) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Repayment must be at least MWK " + minimumMonthlyPayment.toPlainString()
                            + " to meet the current minimum monthly installment");
        }
        List<LoanAccount> studentLoans = loans
                .findAllByStudent_IdOrderByAcademicYearAsc(student.getId());
        BigDecimal totalOutstanding = studentLoans.stream()
                .map(LoanAccount::getOutstandingBalance)
                .filter(balance -> balance.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalOutstanding.signum() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student has no outstanding loan balance");
        }
        if (amount.compareTo(totalOutstanding) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Repayment amount exceeds the student's total outstanding balance");
        }

        BigDecimal remaining = amount;
        List<StudentRepaymentResponse.Allocation> allocations = new ArrayList<>();
        for (LoanAccount loan : studentLoans) {
            if (remaining.signum() == 0) break;
            BigDecimal balance = loan.getOutstandingBalance();
            if (balance.signum() <= 0) continue;
            BigDecimal allocationAmount = balance.min(remaining);
            loan.setOutstandingBalance(balance.subtract(allocationAmount));
            if (loan.getOutstandingBalance().signum() == 0) loan.setLoanStatus(LoanStatus.PAID);

            RepaymentRecord repayment = repayments.save(RepaymentRecord.fromInAppPayment(
                    student, loan, allocationAmount, paymentMethod, actorId,
                    "APP-" + UUID.randomUUID()));
            allocations.add(new StudentRepaymentResponse.Allocation(repayment.getId(),
                    loan.getId(), loan.getPaymentReference(), allocationAmount));
            remaining = remaining.subtract(allocationAmount);
        }

        boolean fullyPaid = loans.findAllByStudent_IdOrderByAcademicYearAsc(student.getId())
                .stream().allMatch(loan -> loan.getOutstandingBalance().signum() == 0);
        String message = actorDescription + " recorded a repayment of MWK "
                + amount.toPlainString() + " for your student loan.";
        if (fullyPaid) message += " All your loan balances are now paid in full.";
        events.publish(new BusinessEvent(actorEmail, "REPAYMENT_POSTED", "REPAYMENT",
                allocations.get(0).repaymentId(), actorDescription + " posted MWK "
                        + amount.toPlainString() + " for student " + student.getId(),
                student.getUser().getId(), "Repayment recorded", message));
        return new StudentRepaymentResponse(amount, paymentMethod, "CONFIRMED",
                List.copyOf(allocations));
    }

    public RepaymentRecord reverse(Long id, String email, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reversal reason is required");
        }
        RepaymentRecord repayment = repayments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment not found"));
        if (!"CONFIRMED".equals(repayment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repayment is already reversed");
        }
        LoanAccount account = loans.findById(repayment.getLoanAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan account not found"));
        account.setOutstandingBalance(account.getOutstandingBalance().add(repayment.getAmount()));
        if (account.getLoanStatus() == LoanStatus.PAID) account.setLoanStatus(LoanStatus.ACTIVE);
        Long actorId = users.findByEmailIgnoreCase(email).map(user -> user.getId()).orElse(null);
        repayment.reverse(actorId, reason);
        students.findById(repayment.getStudentId()).ifPresent(student -> {
            if ("COMPLETED_REPAYMENT".equals(student.getLastNotifiedComplianceStatus())) {
                student.setLastNotifiedComplianceStatus(null);
            }
            events.publish(new BusinessEvent(email, "REPAYMENT_REVERSED", "REPAYMENT",
                    repayment.getId(), "Repayment reversed: " + reason, student.getUser().getId(),
                    "Repayment reversed", "A repayment of MWK " + repayment.getAmount()
                            + " was reversed. Reason: " + reason));
        });
        return repayment;
    }

    private Student studentForEmail(String email) {
        return students.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student profile not found"));
    }
}
