package mw.loancompliance.repayment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="bank_statement_imports") public class BankStatementImport {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    String filename;
    @Column(name="imported_by") Long importedBy;
    @Column(name="imported_at",insertable=false,updatable=false) Instant importedAt;
    @Column(name="transaction_count") int transactionCount;
    @Column(name="matched_count") int matchedCount;
    @Column(name="unmatched_count") int unmatchedCount;
    @Column(name="duplicate_count") int duplicateCount;
    @Column(name="error_count") int errorCount;
    protected BankStatementImport() {
    }
    public BankStatementImport(String f,Long by) {
        filename=f;
        importedBy=by;
    }
    public Long getId() {
        return id;
    }
    public String getFilename() {
        return filename;
    }
    public Instant getImportedAt() {
        return importedAt;
    }
    public int getTransactionCount() {
        return transactionCount;
    }
    public int getMatchedCount() {
        return matchedCount;
    }
    public int getUnmatchedCount() {
        return unmatchedCount;
    }
    public int getDuplicateCount() {
        return duplicateCount;
    }
    public void addRow(boolean matched) {
        transactionCount++;
        if(matched)matchedCount++;
        else unmatchedCount++;
    }
    public void duplicate() {
        duplicateCount++;
    }
}
