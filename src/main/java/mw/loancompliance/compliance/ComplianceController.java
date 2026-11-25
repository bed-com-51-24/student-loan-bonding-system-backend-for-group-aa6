package mw.loancompliance.compliance;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import mw.loancompliance.notification.NotificationService;
import mw.loancompliance.employment.EmploymentRecordRepository;
import mw.loancompliance.loan.LoanAccount;
import mw.loancompliance.loan.LoanAccountRepository;
import mw.loancompliance.repayment.RepaymentRecord;
import mw.loancompliance.repayment.RepaymentRecordRepository;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance")
@SecurityRequirement(name = "bearerAuth")
@Transactional
public class ComplianceController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Blantyre");

    private final StudentRepository students;
    private final LoanAccountRepository loans;
    private final EmploymentRecordRepository employment;
    private final RepaymentRecordRepository repayments;
    private final NotificationService notifications;

    public ComplianceController(
            StudentRepository students,
            LoanAccountRepository loans,
            EmploymentRecordRepository employment,
            RepaymentRecordRepository repayments,
            NotificationService notifications) {
        this.students = students;
        this.loans = loans;
        this.employment = employment;
        this.repayments = repayments;
        this.notifications = notifications;
    }

    public record StudentCompliance(
            Long studentId,
            String studentName,
            String employmentStatus,
            String employmentVerification,
            BigDecimal originalLoanAmount,
            BigDecimal totalRepaid,
            BigDecimal outstandingBalance,
            String complianceStatus,
            String complianceStatusNote) {
    }

    public record Dashboard(
            long totalStudents,
            long employedStudents,
            long selfEmployedStudents,
            long employmentUnderReview,
            BigDecimal totalOutstandingBalance,
            BigDecimal totalRepaid,
            String complianceStatusNote) {
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public Dashboard dashboard() {
        var allStudents = students.findAll();
        var allEmployment = employment.findAll();
        var allLoans = loans.findAll();
        var allRepayments = repayments.findAllByOrderByPaymentDateDesc();
        Set<Long> currentlyEmployedStudentIds = allEmployment.stream()
                .filter(this::isCurrentlyEmployed)
                .map(record -> record.getStudent().getId())
                .collect(Collectors.toSet());

        return new Dashboard(
                allStudents.size(),
                currentlyEmployedStudentIds.size(),
                Math.max(0, allStudents.size() - currentlyEmployedStudentIds.size()),
                allEmployment.stream()
                        .filter(record -> "REQUIRES_REVIEW".equals(record.getStatus())
                                || "PENDING_VERIFICATION".equals(record.getStatus()))
                        .count(),
                allLoans.stream().map(LoanAccount::getOutstandingBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                allRepayments.stream().filter(this::isConfirmed)
                        .map(RepaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                "Compliance totals use each loan's saved balance; status is calculated per student.");
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public List<StudentCompliance> all() {
        return students.findAll().stream().map(student -> view(student.getId())).toList();
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    public StudentCompliance one(@PathVariable Long studentId) {
        return view(studentId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentCompliance me(Authentication authentication) {
        Student student = students.findByUser_EmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student profile not found"));
        return view(student.getId());
    }

    private StudentCompliance view(Long studentId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));
        List<LoanAccount> studentLoans = loans.findAllByStudent_IdOrderByAcademicYearDesc(studentId);
        var employmentRecords = employment.findAllByStudent_User_EmailIgnoreCaseOrderByStartDateDesc(
                student.getUser().getEmail());
        List<RepaymentRecord> studentRepayments =
                repayments.findAllByStudentIdOrderByPaymentDateDesc(studentId);

        BigDecimal principal = studentLoans.stream().map(LoanAccount::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal repaid = studentRepayments.stream().filter(this::isConfirmed)
                .map(RepaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        boolean hasUnallocatedPayment = studentRepayments.stream()
                .anyMatch(payment -> isConfirmed(payment) && payment.getLoanAccountId() == null);
        ComplianceResult result = calculateCompliance(studentLoans, today, hasUnallocatedPayment);
        notifyOnStatusChange(student, result);
        var latestEmployment = employmentRecords.isEmpty() ? null : employmentRecords.get(0);
        var verifiedEmployment = employmentRecords.stream()
                .filter(this::isCurrentlyEmployed)
                .findFirst()
                .orElse(null);

        return new StudentCompliance(
                studentId,
                student.getUser().getFullName(),
                verifiedEmployment == null ? "SELF_EMPLOYED" : verifiedEmployment.getEmploymentCategory(),
                latestEmployment == null ? "NOT_SUBMITTED" : latestEmployment.getStatus(),
                principal,
                repaid,
                result.outstandingBalance(),
                result.status(),
                result.note());
    }

    private ComplianceResult calculateCompliance(
            List<LoanAccount> studentLoans, LocalDate today, boolean hasUnallocatedPayment) {
        if (studentLoans.isEmpty()) {
            return new ComplianceResult("REVIEW_REQUIRED", BigDecimal.ZERO,
                    "No loan account exists for this student.");
        }

        BigDecimal outstandingBalance = studentLoans.stream()
                .map(LoanAccount::getOutstandingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstandingBalance.signum() == 0) {
            return new ComplianceResult("COMPLETED_REPAYMENT", outstandingBalance,
                    "All loan accounts have been repaid in full.");
        }
        if (hasUnallocatedPayment) {
            return new ComplianceResult("REVIEW_REQUIRED", outstandingBalance,
                    "A confirmed repayment is not allocated to a loan account.");
        }

        boolean overdue = false;
        boolean active = false;

        for (LoanAccount loan : studentLoans) {
            if (loan.getOutstandingBalance().signum() == 0) {
                continue;
            }
            List<RepaymentRecord> loanPayments = repayments.findAllByLoanAccountId(loan.getId());
            BigDecimal loanRepaid = loanPayments.stream().filter(this::isConfirmed)
                    .map(RepaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (loanRepaid.compareTo(loan.getPrincipalAmount()) > 0) {
                return new ComplianceResult("REVIEW_REQUIRED", outstandingBalance,
                        "Confirmed repayments exceed the loan principal.");
            }
            if (loan.getRepaymentStartDate() == null) {
                return new ComplianceResult("REVIEW_REQUIRED", outstandingBalance,
                        "A repayment start date could not be determined for an outstanding loan.");
            }

            LocalDate repaymentDeadline = loan.getRepaymentStartDate().plusYears(5);
            if (!today.isBefore(repaymentDeadline)) {
                overdue = true;
            } else if (!today.isBefore(loan.getRepaymentStartDate())) {
                active = true;
            }
        }
        if (overdue) {
            return new ComplianceResult("OVERDUE", outstandingBalance,
                    "The five-year repayment period has ended and a balance remains.");
        }
        if (active) {
            return new ComplianceResult("ACTIVE_REPAYMENT_PERIOD", outstandingBalance,
                    "The loan is within its five-year repayment period.");
        }
        return new ComplianceResult("PENDING_REPAYMENT_START", outstandingBalance,
                "Repayment has not started yet.");
    }

    private void notifyOnStatusChange(Student student, ComplianceResult result) {
        if ("REVIEW_REQUIRED".equals(result.status())) {
            return;
        }
        if (result.status().equals(student.getLastNotifiedComplianceStatus())) {
            return;
        }

        notifications.inApp(
                student.getUser(),
                "COMPLIANCE_STATUS_CHANGED",
                "Repayment status changed",
                "Your repayment status is now " + displayStatus(result.status()) + ". " + result.note());
        student.setLastNotifiedComplianceStatus(result.status());
        students.save(student);
    }

    private String displayStatus(String status) {
        return switch (status) {
            case "PENDING_REPAYMENT_START" -> "pending repayment start";
            case "ACTIVE_REPAYMENT_PERIOD" -> "active repayment period";
            case "OVERDUE" -> "repayment overdue";
            case "COMPLETED_REPAYMENT" -> "completed repayment";
            default -> status;
        };
    }

    private boolean isConfirmed(RepaymentRecord repayment) {
        return "CONFIRMED".equals(repayment.getStatus());
    }

    private boolean isCurrentlyEmployed(mw.loancompliance.employment.EmploymentRecord record) {
        return "EMPLOYED".equals(record.getEmploymentCategory())
                && "VERIFIED".equals(record.getStatus());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshStatusesOnStartup() {
        students.findAll().forEach(student -> view(student.getId()));
    }

    private record ComplianceResult(String status, BigDecimal outstandingBalance, String note) {
    }
}
