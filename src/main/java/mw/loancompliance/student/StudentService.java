package mw.loancompliance.student;

import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            UserService userService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public Student createStudent(
            String fullName,
            String email,
            String studentNumber,
            String university,
            String program,
            LocalDate graduationDate) {

        if (studentRepository.existsByStudentNumber(studentNumber)) {
            throw new IllegalArgumentException(
                    "Student number already exists: " + studentNumber);
        }

        // Create a pending user and obtain the generated user ID.
        UserResponse pendingUser =
                userService.createPendingUser(fullName, email);

        User user = userRepository.findById(pendingUser.id())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Newly created user could not be found."));

        // Create the student profile using the generated user.
        Student student = new Student();
        student.setUser(user);
        student.setStudentNumber(studentNumber);
        student.setUniversity(university);
        student.setProgram(program);
        student.setGraduationDate(graduationDate);

        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Student not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Student getStudentByNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Student not found with number: " + studentNumber));
    }

    @Transactional(readOnly = true)
    public Student getStudentByEmail(String email) {
        return studentRepository.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Student profile not found for: " + email));
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional
    public Student updateStudent(
            Long id,
            String studentNumber,
            String university,
            String program,
            LocalDate graduationDate) {

        Student student = getStudentById(id);

        if (!student.getStudentNumber().equals(studentNumber)
                && studentRepository.existsByStudentNumber(studentNumber)) {
            throw new IllegalArgumentException(
                    "Student number already exists: " + studentNumber);
        }

        student.setStudentNumber(studentNumber);
        student.setUniversity(university);
        student.setProgram(program);
        student.setGraduationDate(graduationDate);

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }
}
