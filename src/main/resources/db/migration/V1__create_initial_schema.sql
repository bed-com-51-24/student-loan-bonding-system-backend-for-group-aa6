-- ============================================================
-- V1: INITIAL DATABASE SCHEMA
-- Student Loan Compliance and Monitoring System
-- Database: PostgreSQL
-- ============================================================


-- ============================================================
-- 1. ORGANIZATIONS
-- Stores registered organizations that can employ graduates.
-- ============================================================

CREATE TABLE organizations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    legal_name VARCHAR(200) NOT NULL,
    registration_number VARCHAR(100) UNIQUE,

    sector VARCHAR(100),
    address TEXT,
    phone VARCHAR(30),
    email VARCHAR(255),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. USERS
-- Stores login accounts and their system roles.
-- ============================================================

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL
        CHECK (
            role IN (
                'LOAN_BOARD_ADMIN',
                'STUDENT',
                'EMPLOYER_ADMIN'
            )
        ),

    account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ACTIVATION'
        CHECK (
            account_status IN (
                'PENDING_ACTIVATION',
                'ACTIVE',
                'DISABLED'
            )
        ),

    organization_id BIGINT
        REFERENCES organizations(id)
        ON DELETE RESTRICT,

    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Prevent duplicate email addresses, ignoring letter case.
CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));

CREATE INDEX idx_users_role
    ON users (role);

CREATE INDEX idx_users_account_status
    ON users (account_status);

CREATE INDEX idx_users_organization
    ON users (organization_id);


-- ============================================================
-- 3. STUDENTS
-- Stores the graduate's academic profile.
-- The login account remains in the users table.
-- ============================================================

CREATE TABLE students (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL UNIQUE
        REFERENCES users(id)
        ON DELETE RESTRICT,

    student_number VARCHAR(50) NOT NULL UNIQUE,

    university VARCHAR(200) NOT NULL,
    program VARCHAR(200) NOT NULL,

    graduation_date DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. LOAN ACCOUNTS
-- Each record represents a student's loan for one academic year.
-- A graduate may therefore have multiple loan accounts.
-- ============================================================

CREATE TABLE loan_accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    student_id BIGINT NOT NULL
        REFERENCES students(id)
        ON DELETE RESTRICT,

    academic_year INTEGER NOT NULL
        CHECK (academic_year BETWEEN 1900 AND 2200),

    principal_amount NUMERIC(14, 2) NOT NULL
        CHECK (principal_amount >= 0),

    outstanding_balance NUMERIC(14, 2) NOT NULL
        CHECK (
            outstanding_balance >= 0
            AND outstanding_balance <= principal_amount
        ),

    disbursement_date DATE,

    loan_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (
            loan_status IN (
                'ACTIVE',
                'PAID',
                'SUSPENDED'
            )
        ),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_student_academic_year
        UNIQUE (student_id, academic_year)
);

CREATE INDEX idx_loan_accounts_student
    ON loan_accounts (student_id);

CREATE INDEX idx_loan_accounts_status
    ON loan_accounts (loan_status);


-- ============================================================
-- 5. EMPLOYMENT RECORDS
-- Tracks graduates' employment with registered organizations.
-- Multiple records allow employment history to be preserved.
-- ============================================================

CREATE TABLE employment_records (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    student_id BIGINT NOT NULL
        REFERENCES students(id)
        ON DELETE RESTRICT,

    organization_id BIGINT NOT NULL
        REFERENCES organizations(id)
        ON DELETE RESTRICT,

    job_title VARCHAR(150),

    monthly_salary NUMERIC(14, 2)
        CHECK (monthly_salary >= 0),

    employment_start_date DATE NOT NULL,
    employment_end_date DATE,

    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (
            verification_status IN (
                'PENDING',
                'VERIFIED',
                'REJECTED'
            )
        ),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_employment_dates
        CHECK (
            employment_end_date IS NULL
            OR employment_end_date >= employment_start_date
        )
);

CREATE INDEX idx_employment_student
    ON employment_records (student_id);

CREATE INDEX idx_employment_organization
    ON employment_records (organization_id);

CREATE INDEX idx_employment_verification
    ON employment_records (verification_status);


-- ============================================================
-- 6. ACCOUNT ACTIVATION TOKENS
-- Stores hashed tokens used to activate accounts.
-- Never store the raw activation token.
-- ============================================================

CREATE TABLE account_activation_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    token_hash VARCHAR(255) NOT NULL UNIQUE,

    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activation_tokens_user
    ON account_activation_tokens (user_id);


-- ============================================================
-- 7. PASSWORD RESET TOKENS
-- Stores hashed tokens used to reset forgotten passwords.
-- ============================================================

CREATE TABLE password_reset_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    token_hash VARCHAR(255) NOT NULL UNIQUE,

    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens (user_id);


-- ============================================================
-- 8. REPAYMENTS
-- Records payments made by graduates.
-- A payment can later be allocated across multiple loan accounts.
-- ============================================================

CREATE TABLE repayments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    student_id BIGINT NOT NULL
        REFERENCES students(id)
        ON DELETE RESTRICT,

    amount NUMERIC(14, 2) NOT NULL
        CHECK (amount > 0),

    payment_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payment_method VARCHAR(30) NOT NULL
        CHECK (
            payment_method IN (
                'DIRECT_PAYMENT',
                'SALARY_DEDUCTION',
                'OTHER'
            )
        ),

    payment_reference VARCHAR(150),

    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (
            payment_status IN (
                'PENDING',
                'CONFIRMED',
                'FAILED',
                'REVERSED'
            )
        ),

    recorded_by BIGINT
        REFERENCES users(id)
        ON DELETE SET NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_repayments_student
    ON repayments (student_id);

CREATE INDEX idx_repayments_date
    ON repayments (payment_date);

CREATE INDEX idx_repayments_status
    ON repayments (payment_status);

CREATE UNIQUE INDEX uq_repayments_reference
    ON repayments (payment_reference)
    WHERE payment_reference IS NOT NULL;


-- ============================================================
-- 9. REPAYMENT ALLOCATIONS
-- Records how much of a confirmed repayment is assigned
-- to each annual loan account.
-- ============================================================

CREATE TABLE repayment_allocations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    repayment_id BIGINT NOT NULL
        REFERENCES repayments(id)
        ON DELETE RESTRICT,

    loan_account_id BIGINT NOT NULL
        REFERENCES loan_accounts(id)
        ON DELETE RESTRICT,

    allocated_amount NUMERIC(14, 2) NOT NULL
        CHECK (allocated_amount > 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_repayment_loan_allocation
        UNIQUE (repayment_id, loan_account_id)
);

CREATE INDEX idx_allocations_loan_account
    ON repayment_allocations (loan_account_id);


-- ============================================================
-- 10. AUDIT LOGS
-- Records important actions for accountability and investigation.
-- ============================================================

CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    actor_user_id BIGINT
        REFERENCES users(id)
        ON DELETE SET NULL,

    action VARCHAR(100) NOT NULL,

    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,

    details JSONB,

    ip_address INET,
    user_agent TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_actor
    ON audit_logs (actor_user_id);

CREATE INDEX idx_audit_entity
    ON audit_logs (entity_type, entity_id);

CREATE INDEX idx_audit_created_at
    ON audit_logs (created_at);


-- ============================================================
-- 11. NOTIFICATIONS
-- Stores messages sent or waiting to be sent to users.
-- ============================================================

CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,

    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP'
        CHECK (
            channel IN (
                'IN_APP',
                'EMAIL'
            )
        ),

    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (
            delivery_status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        ),

    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user
    ON notifications (user_id);

CREATE INDEX idx_notifications_status
    ON notifications (delivery_status);

CREATE INDEX idx_notifications_unread
    ON notifications (user_id, created_at)
    WHERE read_at IS NULL;