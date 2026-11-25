package mw.loancompliance.employment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, Long> {
    @EntityGraph(attributePaths = {"student", "student.user", "organization"})
    List<EmploymentRecord> findAllByStudent_User_EmailIgnoreCaseOrderByStartDateDesc(String email);

    List<EmploymentRecord> findAllByOrganization_IdAndStatusOrderByStartDateAsc(Long id, String status);

    @EntityGraph(attributePaths = {"student", "student.user", "organization"})
    List<EmploymentRecord> findAllByOrganization_IdAndEndDateIsNullOrderByStartDateDesc(Long id);

    @EntityGraph(attributePaths = {"student", "student.user", "organization"})
    List<EmploymentRecord> findAllByOrderByStartDateDesc();

    @EntityGraph(attributePaths = {"student", "student.user", "organization"})
    @Query("select employmentRecord from EmploymentRecord employmentRecord "
            + "where employmentRecord.student is not null "
            + "order by employmentRecord.startDate desc")
    List<EmploymentRecord> findAllForLoanBeneficiaries();

    List<EmploymentRecord> findAllByStatusOrderByStartDateDesc(String status);

    Optional<EmploymentRecord> findByIdAndStudent_User_EmailIgnoreCase(Long id, String email);

    boolean existsByOrganization_IdAndNationalIdAndEndDateIsNull(Long organizationId, String nationalId);

    Optional<EmploymentRecord> findFirstByStudent_IdAndStatusAndEndDateIsNull(Long studentId, String status);

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "organization"})
    Optional<EmploymentRecord> findById(Long id);

    boolean existsByStudent_IdAndEndDateIsNull(Long studentId);
}
