-- Permit the in-app payment methods currently accepted by RepaymentService.
ALTER TABLE repayments
    DROP CONSTRAINT IF EXISTS chk_repayments_payment_method;

ALTER TABLE repayments
    ADD CONSTRAINT chk_repayments_payment_method
    CHECK (payment_method IN (
        'DIRECT_PAYMENT',
        'SALARY_DEDUCTION',
        'OTHER',
        'MOBILE_BANKING',
        'BANK_TRANSFER',
        'PAYCHANGU',
        'AIRTEL_MONEY',
        'TNM_MPAMBA'
    ));
