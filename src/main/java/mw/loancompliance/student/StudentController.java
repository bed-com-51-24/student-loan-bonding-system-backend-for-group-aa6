package mw.loancompliance.student;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mw.loancompliance.student.dto.CreateStudentRequest;
import mw.loancompliance.student.dto.StudentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    public record UpdateStudentProfileRequest(
            @Size(max = 50) String nationalId,
            @Size(max = 200) String university,
            @Size(max = 200) String program,
            LocalDate graduationDate,
            StudyStatus studyStatus,
            LocalDate studyStatusDate) {
    }

    @PostMapping
    @PreAuthorize("hasRole('LOAN_BOARD_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        Student student = studentService.createStudent(
                request.fullName(), request.email(), request.nationalId(),
                request.university(), request.program(), request.graduationDate(),
                request.studyStatus(), request.studyStatusDate());
        return StudentResponse.from(student);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public List<StudentResponse> getStudents(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long studentId,
            Authentication authentication) {
        if (hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN")) {
            if (scope != null && !"ALL".equalsIgnoreCase(scope)) {
                throw badRequest("Loan Board administrators may use scope=ALL or studentId");
            }
            List<Student> students = studentId == null
                    ? studentService.getAllStudents()
                    : List.of(studentService.getStudentById(studentId));
            return students.stream().map(StudentResponse::from).toList();
        }
        if (studentId != null) {
            throw badRequest("Students cannot select a studentId");
        }
        if (!"SELF".equalsIgnoreCase(scope)) {
            throw badRequest("Students must request scope=SELF");
        }
        return List.of(StudentResponse.from(
                studentService.getStudentByUserEmail(authentication.getName())));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('LOAN_BOARD_ADMIN', 'STUDENT')")
    public StudentResponse updateStudent(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long studentId,
            Authentication authentication,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        if (hasRole(authentication, "ROLE_LOAN_BOARD_ADMIN")) {
            if (scope != null) {
                throw badRequest("Loan Board administrators must identify the student with studentId");
            }
            if (studentId == null || studentId <= 0) {
                throw badRequest("A positive studentId is required for administrator updates");
            }
            Student student = studentService.updateStudent(
                    studentId, request.nationalId(), request.university(), request.program(),
                    request.graduationDate(), request.studyStatus(), request.studyStatusDate());
            return StudentResponse.from(student);
        }
        if (studentId != null) {
            throw badRequest("Students cannot select a studentId");
        }
        if (!"SELF".equalsIgnoreCase(scope)) {
            throw badRequest("Students must update with scope=SELF");
        }
        if (request.nationalId() != null || request.graduationDate() != null
                || request.studyStatus() != null || request.studyStatusDate() != null) {
            throw badRequest("Students may update only their university and program");
        }
        Student student = studentService.updateOwnStudent(
                authentication.getName(), request.university(), request.program());
        return StudentResponse.from(student);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, String> handleNotFound(java.util.NoSuchElementException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }
}
