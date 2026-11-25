package mw.loancompliance.student.dto;

import jakarta.validation.constraints.Size;

public record UpdateOwnStudentRequest(
        @Size(max = 200)
        String university,

        @Size(max = 200)
        String program
) {
}
