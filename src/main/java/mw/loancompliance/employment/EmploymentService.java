package mw.loancompliance.employment;

import jakarta.persistence.EntityNotFoundException;
import mw.loancompliance.employment.dto.EmploymentRequest;
import mw.loancompliance.organization.Organization;
import mw.loancompliance.organization.OrganizationRepository;
import mw.loancompliance.student.Student;
import mw.loancompliance.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmploymentService {

    private final EmploymentRepository employmentRepository;
    private final StudentRepository studentRepository;
    private final OrganizationRepository organizationRepository;

    public EmploymentService(EmploymentRepository employmentRepository,
                             StudentRepository studentRepository,
                             OrganizationRepository organizationRepository) {
        this.employmentRepository = employmentRepository;
        this.studentRepository = studentRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public Employment create(EmploymentRequest request) {
        Employment employment = new Employment();
        applyRequest(employment, request);
        return employmentRepository.save(employment);
    }

    public List<Employment> getAll() {
        return employmentRepository.findAll();
    }

    public Employment getById(Long id) {
        return employmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employment record not found."));
    }

    @Transactional
    public Employment update(Long id, EmploymentRequest request) {
        Employment employment = getById(id);
        applyRequest(employment, request);
        return employmentRepository.save(employment);
    }

    @Transactional
    public Employment updateVerificationStatus(Long id, String status) {
        Employment employment = getById(id);
        employment.setVerificationStatus(status);
        return employmentRepository.save(employment);
    }

    @Transactional
    public void delete(Long id) {
        Employment employment = getById(id);
        employmentRepository.delete(employment);
    }

    private void applyRequest(Employment employment, EmploymentRequest request) {
        if (request.employmentEndDate() != null
                && request.employmentEndDate().isBefore(request.employmentStartDate())) {
            throw new IllegalArgumentException("Employment end date cannot be before start date.");
        }
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found."));
        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found."));

        employment.setStudent(student);
        employment.setOrganization(organization);
        employment.setJobTitle(request.jobTitle());
        employment.setMonthlySalary(request.monthlySalary());
        employment.setEmploymentStartDate(request.employmentStartDate());
        employment.setEmploymentEndDate(request.employmentEndDate());
        if (employment.getVerificationStatus() == null) {
            employment.setVerificationStatus("PENDING");
        }
    }
}
