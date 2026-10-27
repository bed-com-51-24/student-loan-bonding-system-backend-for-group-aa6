package mw.loancompliance.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateStudentRequest(

        @NotBlank
        @Size(max = 50)
        String studentNumber,

        @NotBlank
        @Size(max = 200)
        String university,

        @NotBlank
        @Size(max = 200)
        String program,

        LocalDate graduationDate
) {
}
