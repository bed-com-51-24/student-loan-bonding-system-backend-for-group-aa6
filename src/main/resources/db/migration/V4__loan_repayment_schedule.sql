ALTER TABLE loan_accounts
    ADD COLUMN repayment_start_date DATE,
    ADD COLUMN monthly_installment NUMERIC(14, 2);

ALTER TABLE loan_accounts
    ADD CONSTRAINT chk_loan_monthly_installment
        CHECK (monthly_installment IS NULL OR monthly_installment > 0);

-- Reconcile saved balances with the confirmed repayment ledger. Payments
-- without a loan_account_id cannot safely be assigned and are left unallocated.
UPDATE loan_accounts loan
SET outstanding_balance = GREATEST(
    0,
    loan.principal_amount - COALESCE((
        SELECT SUM(repayment.amount)
        FROM repayments repayment
        WHERE repayment.loan_account_id = loan.id
          AND repayment.status = 'CONFIRMED'
    ), 0)
);
