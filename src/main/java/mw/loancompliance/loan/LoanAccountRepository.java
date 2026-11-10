package mw.loancompliance.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    Optional<LoanAccount> findByPaymentReference(String paymentReference);
    @EntityGraph(attributePaths = {"student", "student.user"})
    List<LoanAccount> findAllByStudent_IdOrderByAcademicYearDesc(Long studentId);
    @EntityGraph(attributePaths = {"student", "student.user"})
    List<LoanAccount> findAllByStudent_User_EmailIgnoreCaseOrderByAcademicYearDesc(String email);
    @EntityGraph(attributePaths = {"student", "student.user"})
    Optional<LoanAccount> findWithStudentById(Long id);
    boolean existsByStudent_IdAndAcademicYear(Long studentId, Integer academicYear);
    boolean existsByStudent_IdAndAcademicYearAndIdNot(Long studentId, Integer academicYear, Long id);
}
