-- Version 2 workflow fields and persisted records.
ALTER TABLE loan_accounts ADD COLUMN payment_reference VARCHAR(30);
UPDATE loan_accounts SET payment_reference = 'SLC-' || LPAD(id::text, 5, '0');
ALTER TABLE loan_accounts ALTER COLUMN payment_reference SET NOT NULL;
ALTER TABLE loan_accounts ADD CONSTRAINT uq_loan_accounts_payment_reference UNIQUE (payment_reference);

ALTER TABLE employment_records ADD COLUMN employment_type VARCHAR(30);
ALTER TABLE employment_records ADD COLUMN employment_category VARCHAR(30);
ALTER TABLE employment_records ADD COLUMN employer_name VARCHAR(200);
ALTER TABLE employment_records ADD COLUMN verification_note TEXT;
ALTER TABLE employment_records ADD COLUMN reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE employment_records ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE employment_records ALTER COLUMN verification_status SET DEFAULT 'PENDING_VERIFICATION';
ALTER TABLE employment_records DROP CONSTRAINT IF EXISTS employment_records_verification_status_check;
ALTER TABLE employment_records ADD CONSTRAINT chk_employment_verification_status
    CHECK (verification_status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED', 'REQUIRES_REVIEW', 'ENDED'));
ALTER TABLE employment_records ADD CONSTRAINT chk_employment_category
    CHECK (employment_category IS NULL OR employment_category IN ('EMPLOYED', 'SELF_EMPLOYED', 'UNREGISTERED_EMPLOYER'));
ALTER TABLE employment_records ADD CONSTRAINT chk_employment_type
    CHECK (employment_type IS NULL OR employment_type IN ('PERMANENT', 'CONTRACT', 'TEMPORARY', 'SELF_EMPLOYED', 'OTHER'));

CREATE TABLE payment_submissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    loan_account_id BIGINT NOT NULL REFERENCES loan_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    bank_name VARCHAR(150),
    transaction_reference VARCHAR(150) NOT NULL,
    payment_reference VARCHAR(30) NOT NULL REFERENCES loan_accounts(payment_reference),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION'
       CHECK (status IN ('PENDING_VERIFICATION','MATCHED','UNMATCHED','AMOUNT_MISMATCH','REFERENCE_MISMATCH','VERIFIED','REJECTED')),
    employer_report BOOLEAN NOT NULL DEFAULT FALSE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE RESTRICT,
    deduction_month VARCHAR(7),
    matched_bank_transaction_id BIGINT,
    verification_note TEXT,
    decision_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payment_submissions_student ON payment_submissions(student_id);
CREATE INDEX idx_payment_submissions_status ON payment_submissions(status);
CREATE UNIQUE INDEX uq_payment_submission_transaction ON payment_submissions(transaction_reference, payment_reference);

CREATE TABLE bank_statement_imports (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    imported_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    matched_count INTEGER NOT NULL DEFAULT 0,
    unmatched_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE bank_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    import_id BIGINT NOT NULL REFERENCES bank_statement_imports(id) ON DELETE RESTRICT,
    transaction_reference VARCHAR(150) NOT NULL,
    payment_reference VARCHAR(30) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    bank_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED'
       CHECK (status IN ('IMPORTED','MATCHED','UNMATCHED','ALLOCATED','DUPLICATE')),
    payment_submission_id BIGINT REFERENCES payment_submissions(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bank_transaction_reference UNIQUE (transaction_reference)
);
CREATE INDEX idx_bank_transactions_status ON bank_transactions(status);
ALTER TABLE payment_submissions ADD CONSTRAINT fk_submission_bank_transaction
    FOREIGN KEY (matched_bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE RESTRICT;

ALTER TABLE repayments ADD COLUMN loan_account_id BIGINT REFERENCES loan_accounts(id) ON DELETE RESTRICT;
ALTER TABLE repayments ADD COLUMN submission_id BIGINT UNIQUE REFERENCES payment_submissions(id) ON DELETE RESTRICT;
ALTER TABLE repayments ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
    CHECK (status IN ('CONFIRMED','REVERSED'));
ALTER TABLE repayments ADD COLUMN reversed_at TIMESTAMPTZ;
ALTER TABLE repayments ADD COLUMN reversal_reason TEXT;
ALTER TABLE repayments ADD COLUMN reversed_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE repayments ALTER COLUMN payment_method TYPE VARCHAR(30);
ALTER TABLE repayments DROP CONSTRAINT IF EXISTS repayments_payment_method_check;
ALTER TABLE repayments ADD CONSTRAINT chk_repayments_payment_method
    CHECK (payment_method IN ('DIRECT_PAYMENT','SALARY_DEDUCTION','OTHER','MOBILE_BANKING','BANK_TRANSFER'));

ALTER TABLE notifications ADD COLUMN event_type VARCHAR(80);

ALTER TABLE audit_logs ADD COLUMN actor_role VARCHAR(30);
ALTER TABLE audit_logs ADD COLUMN outcome VARCHAR(30) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_logs ADD COLUMN description TEXT;
