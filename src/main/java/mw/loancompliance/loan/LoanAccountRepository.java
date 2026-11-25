package mw.loancompliance.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    java.util.Optional<LoanAccount> findByPaymentReference(String paymentReference);

    List<LoanAccount> findAllByStudent_IdOrderByAcademicYearDesc(Long studentId);

    List<LoanAccount> findAllByStudent_IdOrderByAcademicYearAsc(Long studentId);

    List<LoanAccount> findAllByStudent_User_EmailIgnoreCaseOrderByAcademicYearDesc(String email);

    boolean existsByStudent_IdAndAcademicYear(Long studentId, Integer academicYear);

    boolean existsByStudent_IdAndAcademicYearAndIdNot(
            Long studentId, Integer academicYear, Long id);

}
