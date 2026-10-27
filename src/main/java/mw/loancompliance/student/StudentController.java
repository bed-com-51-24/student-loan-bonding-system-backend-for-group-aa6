package mw.loancompliance.student;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.NoSuchElementException;
import mw.loancompliance.student.dto.CreateStudentRequest;
import mw.loancompliance.student.dto.StudentResponse;
import mw.loancompliance.student.dto.UpdateStudentRequest;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(
            @Valid @RequestBody CreateStudentRequest request) {

        Student student = studentService.createStudent(
                request.userId(),
                request.studentNumber(),
                request.university(),
                request.program(),
                request.graduationDate()
        );

        return StudentResponse.from(student);
    }

    @GetMapping
    public List<StudentResponse> getAllStudents() {
        return studentService.getAllStudents()
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public StudentResponse getStudentById(@PathVariable Long id) {
        return StudentResponse.from(studentService.getStudentById(id));
    }

    @GetMapping("/number/{studentNumber}")
    public StudentResponse getStudentByNumber(
            @PathVariable String studentNumber) {

        return StudentResponse.from(
                studentService.getStudentByNumber(studentNumber));
    }

    @PutMapping("/{id}")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {

        Student student = studentService.updateStudent(
                id,
                request.studentNumber(),
                request.university(),
                request.program(),
                request.graduationDate()
        );

        return StudentResponse.from(student);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> handleIllegalArgument(
            IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, String> handleNotFound(
            NoSuchElementException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }
}
