package mw.loancompliance.organization;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = OrganizationController.class)
public class OrganizationExceptionHandler {

    private static final String DUPLICATE_REGISTRATION_MESSAGE =
            "Registration number is already used by another organization";
    private static final String REGISTRATION_NUMBER_CONSTRAINT =
            "organizations_registration_number_key";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Organization not found.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        if (DUPLICATE_REGISTRATION_MESSAGE.equals(exception.getMessage())) {
            return error(HttpStatus.CONFLICT, DUPLICATE_REGISTRATION_MESSAGE);
        }

        throw exception;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> validationErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Invalid value"
                                : fieldError.getDefaultMessage(),
                        (firstMessage, nextMessage) -> firstMessage + "; " + nextMessage,
                        TreeMap::new));

        return ResponseEntity.badRequest()
                .body(new ApiError(400, "Request validation failed.", validationErrors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        if (hasRegistrationNumberConstraint(exception)) {
            return error(HttpStatus.CONFLICT, DUPLICATE_REGISTRATION_MESSAGE);
        }

        return error(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    private boolean hasRegistrationNumberConstraint(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && REGISTRATION_NUMBER_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, Map.of()));
    }

    public record ApiError(int status, String message, Map<String, String> validationErrors) {
    }
}
