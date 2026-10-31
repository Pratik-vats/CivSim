package civsim.web;

import civsim.dto.ApiErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeoutException;

/**
 * Central REST exception handler returning clean structured error DTOs.
 * Ensures internal implementation details and stack traces are not leaked to HTTP clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(NoSuchFileException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDto> handleConflict(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiErrorDto> handleTimeout(TimeoutException ex) {
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "Simulation query execution timed out");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneralError(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal simulation error occurred: " + ex.getMessage());
    }

    private ResponseEntity<ApiErrorDto> buildResponse(HttpStatus status, String message) {
        ApiErrorDto error = new ApiErrorDto(
                System.currentTimeMillis(),
                status.value(),
                status.getReasonPhrase(),
                message != null ? message : "No details provided",
                ""
        );
        return ResponseEntity.status(status).body(error);
    }
}

