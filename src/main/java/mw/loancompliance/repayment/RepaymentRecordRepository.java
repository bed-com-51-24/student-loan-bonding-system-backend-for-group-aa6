package mw.loancompliance.repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RepaymentRecordRepository extends JpaRepository<RepaymentRecord,Long> {
    List<RepaymentRecord> findAllByOrderByPaymentDateDesc();
    List<RepaymentRecord> findAllByStudentIdOrderByPaymentDateDesc(Long id);
    List<RepaymentRecord> findAllByLoanAccountId(Long loanAccountId);
    boolean existsByLoanAccountIdAndStatus(Long loanAccountId, String status);
    List<RepaymentRecord> findAllByLoanAccountIdAndStatusOrderByPaymentDateAsc(
            Long loanAccountId, String status);
    Optional<RepaymentRecord> findByIdAndStudentId(Long id,Long studentId);
}
