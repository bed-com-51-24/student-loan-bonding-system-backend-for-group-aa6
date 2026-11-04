package mw.loancompliance.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    Optional<Student> findByUser_EmailIgnoreCase(String email);

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByUser_Id(Long userId);
}
