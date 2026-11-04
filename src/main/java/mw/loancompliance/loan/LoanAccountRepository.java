package mw.loancompliance.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    List<LoanAccount> findAllByStudent_Id(Long studentId);
    List<LoanAccount> findAllByStudent_User_EmailIgnoreCase(String email);
    boolean existsByStudent_IdAndAcademicYear(Long studentId, Integer academicYear);
    boolean existsByStudent_IdAndAcademicYearAndIdNot(Long studentId, Integer academicYear, Long id);
}
