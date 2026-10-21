package mw.loancompliance.student;

import jakarta.validation.Valid;
import mw.loancompliance.student.dto.CreateStudentRequest;
import mw.loancompliance.student.dto.StudentResponse;
import mw.loancompliance.student.dto.UpdateStudentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
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
}
