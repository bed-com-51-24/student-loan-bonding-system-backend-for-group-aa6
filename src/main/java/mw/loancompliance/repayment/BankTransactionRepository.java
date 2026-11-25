package mw.loancompliance.repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BankTransactionRepository extends JpaRepository<BankTransaction,Long> {
    Optional<BankTransaction> findByTransactionReference(String ref);
    List<BankTransaction> findAllByOrderByIdDesc();
    List<BankTransaction> findAllByStatusOrderByIdDesc(String status);
}
