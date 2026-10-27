ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_password_hash_required_after_activation
    CHECK (
        account_status = 'PENDING_ACTIVATION'
        OR password_hash IS NOT NULL
    );