-- The policy is a single 15% charge on principal, not monthly compounding.
-- NULL marks balances that the application must rebuild from principal and
-- confirmed repayments the next time the student loan balance is refreshed.
ALTER TABLE loan_accounts
    ADD COLUMN interest_applied_on DATE;

-- Remove the old principal ceiling while preserving the nonnegative balance rule.
ALTER TABLE loan_accounts
    DROP CONSTRAINT IF EXISTS loan_accounts_outstanding_balance_check;

ALTER TABLE loan_accounts
    DROP CONSTRAINT IF EXISTS loan_accounts_check;

ALTER TABLE loan_accounts
    DROP CONSTRAINT IF EXISTS chk_loan_outstanding_balance;

ALTER TABLE loan_accounts
    ADD CONSTRAINT chk_loan_outstanding_balance_nonnegative
    CHECK (outstanding_balance >= 0);

-- Explicitly mark all existing balances for the one-time application rebuild.
UPDATE loan_accounts
SET interest_applied_on = NULL;

COMMENT ON COLUMN loan_accounts.principal_amount IS
    'Original amount disbursed; does not include interest.';

COMMENT ON COLUMN loan_accounts.outstanding_balance IS
    'Current amount owed including the one-time interest charge and after confirmed repayments.';

COMMENT ON COLUMN loan_accounts.interest_applied_on IS
    'Date the one-time 15% principal interest was applied; NULL means the saved balance requires a rebuild from the confirmed repayment ledger.';
