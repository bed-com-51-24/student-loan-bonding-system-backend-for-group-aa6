package mw.loancompliance.student;

import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Student createStudent(
            Long userId,
            String studentNumber,
            String university,
            String program,
            java.time.LocalDate graduationDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found with ID: " + userId));

        if (studentRepository.existsByUser_Id(userId)) {
            throw new IllegalArgumentException(
                    "This user already has a student profile.");
        }

        if (studentRepository.existsByStudentNumber(studentNumber)) {
            throw new IllegalArgumentException(
                    "Student number already exists: " + studentNumber);
        }

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
                        new NoSuchElementException("Student not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Student getStudentByNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Student not found with number: " + studentNumber));
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
            java.time.LocalDate graduationDate) {

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
