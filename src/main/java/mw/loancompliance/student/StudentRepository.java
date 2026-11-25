package mw.loancompliance.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByNationalId(String nationalId);
    Optional<Student> findByNationalIdIgnoreCase(String nationalId);

    Optional<Student> findByRepaymentIdentifierIgnoreCase(String repaymentIdentifier);

    Optional<Student> findByUser_EmailIgnoreCase(String email);

    boolean existsByNationalId(String nationalId);
    boolean existsByNationalIdIgnoreCase(String nationalId);

    boolean existsByRepaymentIdentifier(String repaymentIdentifier);

    boolean existsByUser_Id(Long userId);
}
