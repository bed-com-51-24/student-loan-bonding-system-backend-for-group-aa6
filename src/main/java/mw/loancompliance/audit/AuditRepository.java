package mw.loancompliance.audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface AuditRepository extends JpaRepository<AuditRecord,Long> {
    List<AuditRecord> findAllByOrderByCreatedAtDesc();
    List<AuditRecord> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(String type,Long id);
    @Query(value = "SELECT DISTINCT a.* FROM audit_logs a " +
            "LEFT JOIN payment_submissions ps " +
            "ON a.entity_type = 'PAYMENT_SUBMISSION' AND ps.id = a.entity_id " +
            "WHERE (a.entity_type = 'STUDENT' AND a.entity_id = :studentId) " +
            "OR ps.student_id = :studentId " +
            "ORDER BY a.created_at DESC", nativeQuery = true)
    List<AuditRecord> findStudentHistory(@Param("studentId") Long studentId);
}
