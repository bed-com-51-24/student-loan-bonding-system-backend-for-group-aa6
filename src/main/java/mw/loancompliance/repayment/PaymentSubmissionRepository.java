package mw.loancompliance.repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentSubmissionRepository extends JpaRepository<PaymentSubmission,Long> {
    List<PaymentSubmission> findAllByStudent_User_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<PaymentSubmission> findAllByOrderByCreatedAtDesc();
    Optional<PaymentSubmission> findByIdAndStudent_User_EmailIgnoreCase(Long id,String email);
    Optional<PaymentSubmission> findByTransactionReferenceAndPaymentReference(String tx,String ref);
    boolean existsByTransactionReference(String tx);
    List<PaymentSubmission> findAllByStatus(String status);
}
