package mw.loancompliance.employment;

import mw.loancompliance.common.BusinessEvent;
import mw.loancompliance.common.BusinessEventPublisher;
import mw.loancompliance.organization.Organization;
import mw.loancompliance.organization.OrganizationRepository;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EmploymentService {
    private final EmploymentRecordRepository records;
    private final StudentRepository students;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final BusinessEventPublisher events;

    public EmploymentService(EmploymentRecordRepository records, StudentRepository students,
            OrganizationRepository organizations, UserRepository users,
            BusinessEventPublisher events) {
        this.records = records;
        this.students = students;
        this.organizations = organizations;
        this.users = users;
        this.events = events;
    }

    public EmploymentRecord addEmployee(String email, String nationalId, String employeeName,
            String jobTitle, LocalDate startDate) {
        if (nationalId == null || nationalId.isBlank() || employeeName == null
                || employeeName.isBlank() || startDate == null) {
            throw bad("National ID, employee name, and employment start date are required");
        }
        if (startDate.isAfter(LocalDate.now())) {
            throw bad("Employment start date cannot be in the future");
        }

        var employerAdmin = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Long organizationId = employerAdmin.getOrganizationId();
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No organization is assigned");
        }
        Organization organization = organizations.findById(organizationId)
                .filter(Organization::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Assigned organization is unavailable"));

        String normalizedId = nationalId.trim().toUpperCase(java.util.Locale.ROOT);
        if (records.existsByOrganization_IdAndNationalIdAndEndDateIsNull(organizationId, normalizedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This employee already has an active record for your organization");
        }

        Student student = students.findByNationalIdIgnoreCase(normalizedId).orElse(null);
        if (student != null && records.findFirstByStudent_IdAndStatusAndEndDateIsNull(
                student.getId(), "VERIFIED").isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student already has an active employment record");
        }

        EmploymentRecord record = new EmploymentRecord();
        record.setStudent(student);
        record.setOrganization(organization);
        record.setEmployeeName(employeeName.trim());
        record.setNationalId(normalizedId);
        record.setEmploymentType("OTHER");
        record.setEmploymentCategory(student == null ? "NON_LOAN_EMPLOYEE" : "EMPLOYED");
        record.setEmployerName(organization.getLegalName());
        record.setJobTitle(jobTitle == null || jobTitle.isBlank() ? null : jobTitle.trim());
        record.setStartDate(startDate);
        record.setStatus("VERIFIED");
        EmploymentRecord saved = records.save(record);

        boolean matchedStudent = student != null;
        events.publish(new BusinessEvent(email,
                matchedStudent ? "EMPLOYMENT_STARTED" : "NON_LOAN_EMPLOYEE_ADDED",
                "EMPLOYMENT_RECORD", saved.getId(),
                matchedStudent
                        ? "Employer recorded employment for student " + student.getId()
                        : "Employer added an employee not matched to a student-loan record",
                matchedStudent ? student.getUser().getId() : null,
                matchedStudent ? "Employment recorded" : null,
                matchedStudent ? organization.getLegalName()
                        + " recorded you as an employee starting " + startDate : null));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<EmploymentRecord> own(String email) {
        return records.findAllByStudent_User_EmailIgnoreCaseOrderByStartDateDesc(email);
    }

    @Transactional(readOnly = true)
    public List<EmploymentRecord> all() {
        return records.findAllForLoanBeneficiaries();
    }

    @Transactional(readOnly = true)
    public EmployeeRoster employerEmployees(String email) {
        Long organizationId = users.findByEmailIgnoreCase(email)
                .map(user -> user.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No organization is assigned");
        }
        List<EmploymentRecord> active = records
                .findAllByOrganization_IdAndEndDateIsNullOrderByStartDateDesc(organizationId);
        long matched = active.stream().filter(record -> record.getStudent() != null).count();
        return new EmployeeRoster(active, matched, active.size() - matched);
    }

    public record EmployeeRoster(List<EmploymentRecord> employees,
            long loanBeneficiaryCount, long otherEmployeeCount) { }

    @Transactional(readOnly = true)
    public EmploymentRecord get(Long id, String email, boolean admin) {
        EmploymentRecord record = find(id);
        if (admin && record.getStudent() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Loan beneficiary employment record not found");
        }
        if (!admin && (record.getOrganization() == null
                || !isEmployerForOrganization(email, record.getOrganization().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Record belongs to another organization");
        }
        return record;
    }

    public EmploymentRecord end(Long id, String email, LocalDate date) {
        EmploymentRecord record = find(id);
        if (record.getOrganization() == null
                || !isEmployerForOrganization(email, record.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Record belongs to another organization");
        }
        if (record.getEndDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Employment record has already ended");
        }
        if (date == null || date.isBefore(record.getStartDate()) || date.isAfter(LocalDate.now())) {
            throw bad("End date must be on or after the start date and not in the future");
        }
        record.setEndDate(date);
        record.setStatus("ENDED");
        Student student = record.getStudent();
        events.publish(new BusinessEvent(email, "EMPLOYMENT_ENDED", "EMPLOYMENT_RECORD",
                record.getId(), "Employer ended employment record " + record.getId(),
                student == null ? null : student.getUser().getId(),
                student == null ? null : "Employment ended",
                student == null ? null : "Your employment with "
                        + record.getOrganization().getLegalName() + " ended on " + date));
        return record;
    }

    private EmploymentRecord find(Long id) {
        return records.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employment record not found"));
    }

    private boolean isEmployerForOrganization(String email, Long organizationId) {
        return users.findByEmailIgnoreCase(email)
                .map(user -> "EMPLOYER_ADMIN".equals(user.getRole())
                        && organizationId.equals(user.getOrganizationId()))
                .orElse(false);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
