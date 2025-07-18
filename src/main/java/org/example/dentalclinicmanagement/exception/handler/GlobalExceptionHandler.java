package org.example.dentalclinicmanagement.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.example.dentalclinicmanagement.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.response.ValidationErrorResponse;
import org.example.dentalclinicmanagement.exception.EmailAlreadyExistsException;
import org.example.dentalclinicmanagement.exception.PasswordUpdateException;
import org.example.dentalclinicmanagement.exception.ResetPasswordException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResetPasswordException.class)
    public ResponseEntity<ErrorResponse> handleResetPasswordException(
            ResetPasswordException ex) {

        logBusinessError("Password reset failed: " + ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Password reset failed. Please check your credentials and try again.")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(PasswordUpdateException.class)
    public ResponseEntity<ErrorResponse> handlePasswordUpdateException(
            PasswordUpdateException ex) {

        logBusinessError("Password update failed: " + ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Password update failed. Please verify your current password and try again.")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        logValidationError("Request validation failed", ex);

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String localizedMessage = error.getDefaultMessage();
            fieldErrors.put(error.getField(), localizedMessage);
        });

        List<String> globalErrors = ex.getBindingResult().getGlobalErrors()
                .stream()
                .map(error -> getLocalizedGlobalError(Objects.requireNonNull(error.getCode())))
                .collect(Collectors.toList());

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed. Please check the provided data.")
                .fieldErrors(fieldErrors)
                .globalErrors(globalErrors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        logValidationError("Parameter constraint violation", ex);

        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            if (fieldName.contains(".")) {
                fieldName = fieldName.substring(fieldName.lastIndexOf('.') + 1);
            }

            String message = violation.getMessage();
            fieldErrors.put(fieldName, message);
        }

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Request parameters are invalid.")
                .fieldErrors(fieldErrors)
                .globalErrors(Collections.emptyList())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        logRequestError("Malformed JSON request", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Malformed JSON request. Please check your request format.")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        logRequestError("Type mismatch for parameter '" + ex.getName() + "'", ex);

        assert ex.getRequiredType() != null;
        String message = String.format("Invalid value for parameter '%s'. Expected type: %s",
                ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex) {

        logRequestError("Missing required parameter: " + ex.getParameterName(), ex);

        String message = String.format("Missing required parameter: %s", ex.getParameterName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {

        logSecurityEvent("Authentication failed", ex);

        String message = getAuthenticationMessage(ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    private static String getAuthenticationMessage(AuthenticationException ex) {
        String message = "Authentication failed. Please check your credentials.";

        if (ex.getMessage() != null) {
            String exceptionMessage = ex.getMessage().toLowerCase();

            if (exceptionMessage.contains("locked")) {
                message = "Your account has been locked. Please contact support.";
            } else if (exceptionMessage.contains("expired")) {
                message = "Your authentication token has expired. Please log in again.";
            } else if (exceptionMessage.contains("credentials expired")) {
                message = "Your credentials have expired. Please reset your password.";
            } else {
                message = "Invalid credentials. Please check your username and password.";
            }
        }
        return message;
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex) {

        logSecurityEvent("Login attempt with disabled account", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Your account is disabled. Please contact support.")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {

        logSecurityEvent("Invalid login credentials", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid credentials. Please check your username and password.")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(Exception ex) {

        logSecurityEvent("Access denied", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access denied. You don't have permission to access this resource.")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {

        logBusinessError("Registration failed - email already exists", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("An account with this email address already exists.")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex) {

        logBusinessError(ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        logSystemError("Data integrity violation", ex);

        String message = "A data constraint violation occurred.";
        String exceptionMessage = ex.getMessage().toLowerCase();

        if (exceptionMessage.contains("unique") || exceptionMessage.contains("duplicate")) {
            message = "A record with this data already exists.";
        } else if (exceptionMessage.contains("foreign key")) {
            message = "Cannot perform this operation due to related data constraints.";
        } else if (exceptionMessage.contains("not null")) {
            message = "Required field cannot be empty.";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        logRequestError("Invalid argument", ex);

        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Invalid argument provided.";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex) {

        logBusinessError("Illegal state", ex);

        String message = getIllegalStateMessage(ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    private String getIllegalStateMessage(String exceptionMessage) {
        String message = "Invalid operation requested.";

        if (exceptionMessage != null) {
            if (exceptionMessage.toLowerCase().contains("not available")) {
                message = "Tour is not available for booking.";
            } else if (exceptionMessage.toLowerCase().contains("insufficient balance")) {
                message = "Insufficient balance to complete this operation.";
            } else if (exceptionMessage.toLowerCase().contains("not active")) {
                message = "Account is not active.";
            } else if (!exceptionMessage.trim().isEmpty()) {
                message = exceptionMessage;
            }
        }
        return message;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {

        logSystemError("Unexpected runtime exception", ex);

        String message = getIllegalStateMessage(ex.getMessage());
        if (message.equals("Invalid operation requested.")) {
            message = "An unexpected error occurred. Please try again later.";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {

        logSystemError("Unexpected system error", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An internal server error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getLocalizedGlobalError(String errorCode) {
        return switch (errorCode.toLowerCase()) {
            case "notblank" -> "This field cannot be blank.";
            case "notempty" -> "This field cannot be empty.";
            case "size" -> "This field size is invalid.";
            case "min" -> "Value is too small.";
            case "max" -> "Value is too large.";
            case "email" -> "Invalid email format.";
            case "pattern" -> "Invalid format.";
            default -> "This field is required.";
        };
    }

    private void logSecurityEvent(String message, Exception ex) {
        log.warn("SECURITY EVENT: {} | Exception: {}", message, ex.getMessage());
        log.debug("Security event details:", ex);
    }

    private void logBusinessError(String message, Exception ex) {
        log.error("BUSINESS ERROR: {} | Exception: {}", message, ex.getMessage());
        log.debug("Business error details:", ex);
    }

    private void logSystemError(String message, Exception ex) {
        log.error("SYSTEM ERROR: {} | Exception: {}", message, ex.getMessage(), ex);
    }

    private void logValidationError(String message, Exception ex) {
        log.warn("VALIDATION ERROR: {} | Exception: {}", message, ex.getMessage());
    }

    private void logRequestError(String message, Exception ex) {
        log.warn("REQUEST ERROR: {} | Exception: {}", message, ex.getMessage());
    }
}