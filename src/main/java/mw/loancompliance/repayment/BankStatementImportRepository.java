package mw.loancompliance.repayment;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BankStatementImportRepository extends JpaRepository<BankStatementImport,Long> {
}
