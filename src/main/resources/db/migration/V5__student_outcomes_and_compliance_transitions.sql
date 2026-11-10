ALTER TABLE students
    ADD COLUMN study_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    ADD COLUMN study_status_date DATE,
    ADD COLUMN last_notified_compliance_status VARCHAR(40);

ALTER TABLE students
    ADD CONSTRAINT chk_students_study_status
        CHECK (study_status IN ('ENROLLED', 'GRADUATED', 'WITHDRAWN', 'EXPELLED')),
    ADD CONSTRAINT chk_students_study_status_date
        CHECK (study_status = 'ENROLLED' OR study_status_date IS NOT NULL);

UPDATE students
SET study_status = 'GRADUATED',
    study_status_date = graduation_date
WHERE graduation_date IS NOT NULL
  AND graduation_date <= CURRENT_DATE;

UPDATE loan_accounts loan
SET repayment_start_date = CASE
    WHEN student.study_status IN ('WITHDRAWN', 'EXPELLED')
        THEN student.study_status_date
    WHEN student.study_status = 'GRADUATED'
        THEN COALESCE(student.study_status_date, student.graduation_date) + INTERVAL '2 years'
    WHEN student.graduation_date IS NOT NULL
        THEN student.graduation_date + INTERVAL '2 years'
    ELSE NULL
END::date
FROM students student
WHERE loan.student_id = student.id;
