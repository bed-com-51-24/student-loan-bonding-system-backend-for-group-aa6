-- Store employee identity details for employer rosters, including employees
-- who do not have a student-loan record.
ALTER TABLE employment_records
    ADD COLUMN employee_name VARCHAR(255),
    ADD COLUMN national_id VARCHAR(255);

-- Existing employment rows require a student_id in V1, so copy the real
-- identity details from the linked student and shared user account.
UPDATE employment_records employment
SET employee_name = users.full_name,
    national_id = students.national_id
FROM students
JOIN users ON users.id = students.user_id
WHERE employment.student_id = students.id;

-- Fail safely rather than inventing identity values if the existing database
-- contains rows that cannot be linked to a student account.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM employment_records
        WHERE employee_name IS NULL OR national_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot backfill employee identity for every existing employment record';
    END IF;
END $$;

ALTER TABLE employment_records
    ALTER COLUMN employee_name SET NOT NULL,
    ALTER COLUMN national_id SET NOT NULL,
    ALTER COLUMN student_id DROP NOT NULL;

-- Employer rosters can include employees that are not loan beneficiaries.
ALTER TABLE employment_records
    DROP CONSTRAINT IF EXISTS chk_employment_category;

ALTER TABLE employment_records
    ADD CONSTRAINT chk_employment_category
    CHECK (
        employment_category IS NULL
        OR employment_category IN (
            'EMPLOYED',
            'SELF_EMPLOYED',
            'UNREGISTERED_EMPLOYER',
            'NON_LOAN_EMPLOYEE'
        )
    );
