package mw.loancompliance.student.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import mw.loancompliance.student.StudyStatus;

public record UpdateStudentRequest(

        @Size(max = 50)
        String nationalId,

        @Size(max = 200)
        String university,

        @Size(max = 200)
        String program,

        LocalDate graduationDate,

        StudyStatus studyStatus,

        LocalDate studyStatusDate
) {
}
