package mw.loancompliance.loan;

import mw.loancompliance.loan.dto.CreateLoanAccountRequest;
import mw.loancompliance.loan.dto.UpdateLoanAccountRequest;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class LoanAccountService {
    private final LoanAccountRepository loanRepository;
    private final StudentRepository studentRepository;

    public LoanAccountService(LoanAccountRepository loanRepository, StudentRepository studentRepository) {
        this.loanRepository = loanRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public LoanAccount create(CreateLoanAccountRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + request.studentId()));
        if (loanRepository.existsByStudent_IdAndAcademicYear(request.studentId(), request.academicYear())) {
            throw new IllegalArgumentException("A loan account already exists for this student and academic year.");
        }
        validateBalance(request.principalAmount(), request.outstandingBalance());
        LoanAccount loan = new LoanAccount();
        loan.setStudent(student);
        loan.setAcademicYear(request.academicYear());
        loan.setPrincipalAmount(request.principalAmount());
        loan.setOutstandingBalance(request.outstandingBalance());
        loan.setDisbursementDate(request.disbursementDate());
        loan.setLoanStatus(request.loanStatus());
        return loanRepository.save(loan);
    }

    public List<LoanAccount> findByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) throw new NoSuchElementException("Student not found with ID: " + studentId);
        return loanRepository.findAllByStudent_Id(studentId);
    }

    public List<LoanAccount> findMine(String email) {
        return loanRepository.findAllByStudent_User_EmailIgnoreCase(email);
    }

    public LoanAccount findById(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Loan account not found with ID: " + id));
    }

    public LoanAccount findByIdForOwner(Long id, String email) {
        LoanAccount loan = findById(id);
        if (!loan.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new LoanAccountAccessDeniedException("You can only access your own loan accounts.");
        }
        return loan;
    }

    @Transactional
    public LoanAccount update(Long id, UpdateLoanAccountRequest request) {
        LoanAccount loan = findById(id);
        if (request.academicYear() != null) loan.setAcademicYear(request.academicYear());
        if (request.principalAmount() != null) loan.setPrincipalAmount(request.principalAmount());
        if (request.outstandingBalance() != null) loan.setOutstandingBalance(request.outstandingBalance());
        if (request.disbursementDate() != null) loan.setDisbursementDate(request.disbursementDate());
        if (request.loanStatus() != null) loan.setLoanStatus(request.loanStatus());
        validateBalance(loan.getPrincipalAmount(), loan.getOutstandingBalance());
        if (loanRepository.existsByStudent_IdAndAcademicYearAndIdNot(
                loan.getStudent().getId(), loan.getAcademicYear(), loan.getId())) {
            throw new IllegalArgumentException("A loan account already exists for this student and academic year.");
        }
        return loanRepository.save(loan);
    }

    private void validateBalance(java.math.BigDecimal principal, java.math.BigDecimal balance) {
        if (balance.compareTo(principal) > 0) throw new IllegalArgumentException("Outstanding balance cannot exceed principal amount.");
    }
}
