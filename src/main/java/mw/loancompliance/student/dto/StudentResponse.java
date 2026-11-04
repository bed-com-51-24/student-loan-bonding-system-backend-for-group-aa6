package mw.loancompliance.student.dto;

import mw.loancompliance.student.Student;

import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String studentNumber,
        String university,
        String program,
        LocalDate graduationDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getUser().getId(),
                student.getUser().getFullName(),
                student.getUser().getEmail(),
                student.getStudentNumber(),
                student.getUniversity(),
                student.getProgram(),
                student.getGraduationDate(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
