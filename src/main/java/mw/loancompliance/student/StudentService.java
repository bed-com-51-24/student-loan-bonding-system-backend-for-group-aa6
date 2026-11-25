package mw.loancompliance.student;

import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.entity.UserRole;
import mw.loancompliance.user.dto.CreatePendingUserCommand;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.user.service.UserService;
import mw.loancompliance.loan.LoanAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.security.SecureRandom;

@Service
public class StudentService {

    private static final String REPAYMENT_IDENTIFIER_CHARACTERS = "0123456789ABCDEF";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final LoanAccountRepository loanAccountRepository;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            UserService userService,
            LoanAccountRepository loanAccountRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.loanAccountRepository = loanAccountRepository;
    }

    @Transactional
    public Student createStudent(
            String fullName,
            String email,
            String nationalId,
            String university,
            String program,
            java.time.LocalDate graduationDate,
            StudyStatus studyStatus,
            java.time.LocalDate studyStatusDate) {

        String normalizedNationalId = normalizeNationalId(nationalId);
        if (studentRepository.existsByNationalIdIgnoreCase(normalizedNationalId)) {
            throw new IllegalArgumentException(
                    "National ID already exists: " + nationalId);
        }

        var userResponse = userService.createPendingUser(
                new CreatePendingUserCommand(fullName, email),
                UserRole.STUDENT,
                null);
        Long userId = userResponse.id();

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("New student account was not found"));

        if (studentRepository.existsByUser_Id(userId)) {
            throw new IllegalArgumentException(
                    "This user already has a student profile.");
        }

        Student student = new Student();
        student.setUser(user);
        student.setNationalId(normalizedNationalId);
        student.setUniversity(university);
        student.setProgram(program);
        student.setGraduationDate(graduationDate);
        student.setRepaymentIdentifier(generateUniqueRepaymentIdentifier());
        applyStudyStatus(student, studyStatus, studyStatusDate);

        return studentRepository.save(student);
    }

    private String generateUniqueRepaymentIdentifier() {
        String identifier;
        do {
            StringBuilder value = new StringBuilder(5);
            for (int index = 0; index < 5; index++) {
                value.append(REPAYMENT_IDENTIFIER_CHARACTERS.charAt(
                        SECURE_RANDOM.nextInt(REPAYMENT_IDENTIFIER_CHARACTERS.length())));
            }
            identifier = value.toString();
        } while (studentRepository.existsByRepaymentIdentifier(identifier));
        return identifier;
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Student not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Student getStudentByNationalId(String nationalId) {
        return studentRepository.findByNationalId(nationalId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Student not found with National ID"));
    }

    @Transactional(readOnly = true)
    public Student getStudentByUserEmail(String email) {
        return studentRepository.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() -> new NoSuchElementException(
                        "Student profile not found for the authenticated user"));
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional
    public Student updateStudent(
            Long id,
            String nationalId,
            String university,
            String program,
            java.time.LocalDate graduationDate,
            StudyStatus studyStatus,
            java.time.LocalDate studyStatusDate) {

        Student student = getStudentById(id);

        if (nationalId != null) {
            if (nationalId.isBlank()) {
                throw new IllegalArgumentException("National ID must not be blank");
            }
            String normalizedNationalId = normalizeNationalId(nationalId);
            if (!student.getNationalId().equalsIgnoreCase(normalizedNationalId)
                    && studentRepository.existsByNationalIdIgnoreCase(normalizedNationalId)) {
                throw new IllegalArgumentException(
                        "National ID already exists");
            }
            student.setNationalId(normalizedNationalId);
        }
        if (university != null) {
            if (university.isBlank()) {
                throw new IllegalArgumentException("University must not be blank");
            }
            student.setUniversity(university);
        }
        if (program != null) {
            if (program.isBlank()) {
                throw new IllegalArgumentException("Program must not be blank");
            }
            student.setProgram(program);
        }
        if (graduationDate != null) {
            student.setGraduationDate(graduationDate);
        }
        if (studyStatus != null || studyStatusDate != null) {
            applyStudyStatus(student, studyStatus, studyStatusDate);
            syncLoanRepaymentStartDates(student);
        } else if (graduationDate != null) {
            syncLoanRepaymentStartDates(student);
        }
        if (nationalId == null && university == null && program == null
                && graduationDate == null && studyStatus == null && studyStatusDate == null) {
            throw new IllegalArgumentException("At least one profile field is required");
        }

        return studentRepository.save(student);
    }

    @Transactional
    public Student updateOwnStudent(
            String email,
            String university,
            String program) {
        Student student = getStudentByUserEmail(email);
        if (university != null) {
            if (university.isBlank()) {
                throw new IllegalArgumentException("University must not be blank");
            }
            student.setUniversity(university);
        }
        if (program != null) {
            if (program.isBlank()) {
                throw new IllegalArgumentException("Program must not be blank");
            }
            student.setProgram(program);
        }
        if (university == null && program == null) {
            throw new IllegalArgumentException("At least one profile field is required");
        }
        return studentRepository.save(student);
    }

    private void applyStudyStatus(
            Student student, StudyStatus requestedStatus, java.time.LocalDate requestedDate) {
        StudyStatus status = requestedStatus == null ? student.getStudyStatus() : requestedStatus;
        if (requestedStatus != null) {
            student.setStudyStatus(status);
        }

        if (status == StudyStatus.ENROLLED) {
            if (requestedDate != null) {
                throw new IllegalArgumentException(
                        "Study status date is only used for graduated, withdrawn, or expelled students");
            }
            student.setStudyStatusDate(null);
            return;
        }

        java.time.LocalDate effectiveDate = requestedDate;
        if (effectiveDate == null && requestedStatus == null) {
            effectiveDate = student.getStudyStatusDate();
        }
        if (effectiveDate == null && status == StudyStatus.GRADUATED) {
            effectiveDate = student.getGraduationDate();
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException(
                    "Study status date is required for graduated, withdrawn, or expelled students");
        }
        student.setStudyStatusDate(effectiveDate);
        if (status == StudyStatus.GRADUATED) {
            student.setGraduationDate(effectiveDate);
        }
    }

    private String normalizeNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            throw new IllegalArgumentException("National ID must not be blank");
        }
        return nationalId.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void syncLoanRepaymentStartDates(Student student) {
        java.time.LocalDate startDate = student.calculateRepaymentStartDate();
        loanAccountRepository.findAllByStudent_IdOrderByAcademicYearDesc(student.getId())
                .forEach(account -> account.setRepaymentStartDate(startDate));
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }
}
