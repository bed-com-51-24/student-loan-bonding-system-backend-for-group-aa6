package mw.loancompliance.notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NotificationRepository extends JpaRepository<NotificationRecord,Long> {
    List<NotificationRecord> findAllByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<NotificationRecord> findAllByOrderByCreatedAtDesc();
    Optional<NotificationRecord> findByIdAndUser_EmailIgnoreCase(Long id,String email);
    boolean existsByUser_IdAndEventTypeAndMessageContaining(Long userId, String eventType, String text);
}
