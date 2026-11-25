ALTER TABLE students
    ADD COLUMN repayment_identifier VARCHAR(5);

DO $$
DECLARE
    student_row RECORD;
    candidate VARCHAR(5);
BEGIN
    FOR student_row IN SELECT id FROM students ORDER BY id LOOP
        LOOP
            candidate := upper(substr(md5(gen_random_uuid()::text), 1, 5));
            EXIT WHEN NOT EXISTS (
                SELECT 1 FROM students WHERE repayment_identifier = candidate
            );
        END LOOP;

        UPDATE students
        SET repayment_identifier = candidate
        WHERE id = student_row.id;
    END LOOP;
END $$;

ALTER TABLE students
    ALTER COLUMN repayment_identifier SET NOT NULL;

CREATE UNIQUE INDEX uq_students_repayment_identifier
    ON students (repayment_identifier);
