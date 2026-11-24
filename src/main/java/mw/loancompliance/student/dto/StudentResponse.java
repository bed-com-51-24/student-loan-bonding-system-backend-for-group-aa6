package mw.loancompliance.student.dto;

import mw.loancompliance.student.Student;

import java.time.Instant;
import java.time.LocalDate;
import mw.loancompliance.student.StudyStatus;

public record StudentResponse(
        Long id,
        Long userId,
        String nationalId,
        String repaymentIdentifier,
        String university,
        String program,
        LocalDate graduationDate,
        StudyStatus studyStatus,
        LocalDate studyStatusDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getUser().getId(),
                student.getNationalId(),
                student.getRepaymentIdentifier(),
                student.getUniversity(),
                student.getProgram(),
                student.getGraduationDate(),
                student.getStudyStatus(),
                student.getStudyStatusDate(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
