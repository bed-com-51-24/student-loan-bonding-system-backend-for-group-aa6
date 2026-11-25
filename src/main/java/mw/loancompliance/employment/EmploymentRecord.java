package mw.loancompliance.employment;
import jakarta.persistence.*;
import mw.loancompliance.organization.Organization;
import mw.loancompliance.student.Student;
import java.time.Instant;
import java.time.LocalDate;
@Entity @Table(name="employment_records")
public class EmploymentRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_id") private Student student;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="organization_id") private Organization organization;
    @Column(name="employee_name",nullable=false) private String employeeName;
    @Column(name="national_id",nullable=false) private String nationalId;
    @Column(name="employment_type") private String employmentType;
    @Column(name="employment_category") private String employmentCategory;
    @Column(name="employer_name") private String employerName;
    @Column(name="job_title") private String jobTitle;
    @Column(name="monthly_salary") private java.math.BigDecimal monthlySalary;
    @Column(name="employment_start_date",nullable=false) private LocalDate startDate;
    @Column(name="employment_end_date") private LocalDate endDate;
    @Column(name="verification_status",nullable=false) private String status="VERIFIED";
    @Column(name="created_at",insertable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",insertable=false,updatable=false) private Instant updatedAt;
    protected EmploymentRecord() {
    }
    public Long getId() {
        return id;
    }
    public Student getStudent() {
        return student;
    }
    public void setStudent(Student v) {
        student=v;
    }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { employeeName=v; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String v) { nationalId=v; }
    public Organization getOrganization() {
        return organization;
    }
    public void setOrganization(Organization v) {
        organization=v;
    }
    public String getEmploymentType() {
        return employmentType;
    }
    public void setEmploymentType(String v) {
        employmentType=v;
    }
    public String getEmploymentCategory() {
        return employmentCategory;
    }
    public void setEmploymentCategory(String v) {
        employmentCategory=v;
    }
    public String getEmployerName() {
        return employerName;
    }
    public void setEmployerName(String v) {
        employerName=v;
    }
    public String getJobTitle() {
        return jobTitle;
    }
    public void setJobTitle(String v) {
        jobTitle=v;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate v) {
        startDate=v;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate v) {
        endDate=v;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String v) {
        status=v;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}
