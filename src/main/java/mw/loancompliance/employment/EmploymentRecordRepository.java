package mw.loancompliance.employment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord,Long> {
    List<EmploymentRecord> findAllByStudent_User_EmailIgnoreCaseOrderByStartDateDesc(String email);
    List<EmploymentRecord> findAllByOrganization_IdAndStatusOrderByStartDateAsc(Long id,String status);
    List<EmploymentRecord> findAllByOrderByStartDateDesc();
    List<EmploymentRecord> findAllByStatusOrderByStartDateDesc(String status);
    Optional<EmploymentRecord> findByIdAndStudent_User_EmailIgnoreCase(Long id,String email);
}
