package mw.loancompliance.student;

import jakarta.persistence.*;
import mw.loancompliance.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_number", nullable = false, unique = true, length = 50)
    private String studentNumber;

    @Column(name = "repayment_identifier", nullable = false, length = 5)
    private String repaymentIdentifier;

    @Column(nullable = false, length = 200)
    private String university;

    @Column(nullable = false, length = 200)
    private String program;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_status", nullable = false, length = 20)
    private StudyStatus studyStatus = StudyStatus.ENROLLED;

    @Column(name = "study_status_date")
    private LocalDate studyStatusDate;

    @Column(name = "last_notified_compliance_status", length = 40)
    private String lastNotifiedComplianceStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Student() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getRepaymentIdentifier() {
        return repaymentIdentifier;
    }

    public void setRepaymentIdentifier(String repaymentIdentifier) {
        this.repaymentIdentifier = repaymentIdentifier;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public LocalDate getGraduationDate() {
        return graduationDate;
    }

    public void setGraduationDate(LocalDate graduationDate) {
        this.graduationDate = graduationDate;
    }

    public StudyStatus getStudyStatus() {
        return studyStatus;
    }

    public void setStudyStatus(StudyStatus studyStatus) {
        this.studyStatus = studyStatus;
    }

    public LocalDate getStudyStatusDate() {
        return studyStatusDate;
    }

    public void setStudyStatusDate(LocalDate studyStatusDate) {
        this.studyStatusDate = studyStatusDate;
    }

    public String getLastNotifiedComplianceStatus() {
        return lastNotifiedComplianceStatus;
    }

    public void setLastNotifiedComplianceStatus(String lastNotifiedComplianceStatus) {
        this.lastNotifiedComplianceStatus = lastNotifiedComplianceStatus;
    }

    public LocalDate calculateRepaymentStartDate() {
        return switch (studyStatus) {
            case WITHDRAWN, EXPELLED -> studyStatusDate;
            case GRADUATED -> {
                LocalDate completedOn = studyStatusDate != null ? studyStatusDate : graduationDate;
                yield completedOn == null ? null : completedOn.plusYears(2);
            }
            case ENROLLED -> graduationDate == null ? null : graduationDate.plusYears(2);
        };
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
