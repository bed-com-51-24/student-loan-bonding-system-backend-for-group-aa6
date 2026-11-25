package mw.loancompliance.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        ProblemDetail detail = ProblemDetail.forStatus(status == null
                ? exception.getStatusCode()
                : status);
        String reason = exception.getReason();
        if (reason != null && !reason.isBlank()) {
            detail.setDetail(reason);
        }
        return detail;
    }
}
