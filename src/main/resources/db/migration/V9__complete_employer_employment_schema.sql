-- Normalize student National IDs before enforcing case-insensitive uniqueness.
UPDATE students
SET national_id = upper(trim(national_id));

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM students
        WHERE national_id IS NULL OR trim(national_id) = ''
    ) THEN
        RAISE EXCEPTION 'Cannot normalize a blank student National ID';
    END IF;

    IF EXISTS (
        SELECT lower(national_id)
        FROM students
        GROUP BY lower(national_id)
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot enforce case-insensitive uniqueness: duplicate student National IDs exist';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_students_national_id_ci
    ON students (lower(national_id));

-- Match the identity column sizes used by the playground employment schema.
ALTER TABLE employment_records
    ALTER COLUMN employee_name TYPE VARCHAR(150),
    ALTER COLUMN national_id TYPE VARCHAR(50);

UPDATE employment_records
SET national_id = upper(trim(national_id));

-- No verification queue is used in the employer-managed workflow.
UPDATE employment_records
SET verification_status = 'REJECTED',
    employment_end_date = COALESCE(employment_end_date, employment_start_date)
WHERE verification_status IN ('PENDING_VERIFICATION', 'REQUIRES_REVIEW');

ALTER TABLE employment_records
    ALTER COLUMN verification_status SET DEFAULT 'VERIFIED';

-- Keep one active record for each employee at an organization. Older active
-- duplicates become ended history records before adding the unique index.
WITH duplicate_active AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY organization_id, national_id
               ORDER BY id
           ) AS position
    FROM employment_records
    WHERE employment_end_date IS NULL
      AND organization_id IS NOT NULL
)
UPDATE employment_records employment
SET verification_status = 'ENDED',
    employment_end_date = employment.employment_start_date
FROM duplicate_active duplicates
WHERE employment.id = duplicates.id
  AND duplicates.position > 1;

CREATE INDEX idx_employment_national_id
    ON employment_records (national_id);

CREATE INDEX idx_employment_org_active
    ON employment_records (organization_id, employment_end_date);

CREATE UNIQUE INDEX uq_active_employment_org_national_id
    ON employment_records (organization_id, national_id)
    WHERE employment_end_date IS NULL
      AND organization_id IS NOT NULL;
