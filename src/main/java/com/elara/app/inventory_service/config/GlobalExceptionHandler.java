package com.elara.app.inventory_service.config;

import com.elara.app.inventory_service.exceptions.BaseException;
import com.elara.app.inventory_service.utils.ErrorCode;
import com.elara.app.inventory_service.utils.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageService messageService;

    // ========================================
    // CUSTOM EXCEPTIONS
    // ========================================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception, HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler-BaseException] Code: {} | Message: {} | Path: {}", 
            exception.getCode(), exception.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = buildErrorResponse(
            exception.getCode(),
            exception.getValue(),
            exception.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ErrorCode.fromCode(exception.getCode()).getHttpStatus());
    }

    // ========================================
    // VALIDATION & REQUEST ERRORS (4xx)
    // ========================================

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception exception, HttpServletRequest request) {
        String message = extractValidationMessage(exception);
        log.warn("[GlobalExceptionHandler-ValidationError] Type: {} | Message: {} | Path: {}", 
            exception.getClass().getSimpleName(), message, request.getRequestURI());
        ErrorResponse errorResponse = buildErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ErrorCode.INVALID_DATA.getHttpStatus());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        String message = messageService.getMessage("media.type.not.supported", exception.getContentType(), MediaType.APPLICATION_JSON_VALUE);
        log.warn("[GlobalExceptionHandler-UnsupportedMediaType] ContentType: {} | Message: {} | Path: {}", 
            exception.getContentType(), message, request.getRequestURI());
        ErrorResponse errorResponse = buildErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        String message = messageService.getMessage("method.not.supported", request.getMethod());
        log.warn("[GlobalExceptionHandler-MethodNotAllowed] Method: {} | Message: {} | Path: {}", 
            request.getMethod(), message, request.getRequestURI());
        ErrorResponse errorResponse = buildErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // ========================================
    // DATABASE ERRORS (5xx)
    // ========================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception, HttpServletRequest request) {
        String detail = extractDataIntegrityDetail(exception.getMessage());
        String message = "Integrity violation: " + messageService.getMessage("global.error.database", detail);
        log.error("[GlobalExceptionHandler-DataIntegrityViolation] Message: {} | Path: {}", message, request.getRequestURI(), exception);
        ErrorResponse errorResponse = buildErrorResponse(
            ErrorCode.DATABASE_ERROR.getCode(),
            ErrorCode.DATABASE_ERROR.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // ========================================
    // GENERIC FALLBACK (5xx)
    // ========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        String message = messageService.getMessage("global.error.unexpected", exception.getMessage());
        log.error("[GlobalExceptionHandler-UnexpectedException] Message: {} | Path: {} | Exception: {}", 
            message, request.getRequestURI(), exception.getClass().getSimpleName(), exception);
        ErrorResponse errorResponse = buildErrorResponse(
            ErrorCode.UNEXPECTED_ERROR.getCode(),
            ErrorCode.UNEXPECTED_ERROR.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, ErrorCode.UNEXPECTED_ERROR.getHttpStatus());
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    /**
     * Build the error response with structured logging.
     */
    private ErrorResponse buildErrorResponse(int code, String value, String message, String path) {
        return ErrorResponse.builder()
            .code(code)
            .value(value)
            .message(message)
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();
    }

    /**
     * Extract the appropriate message based on the type of validation exception.
     */
    private String extractValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgException) {
            Object[] args = methodArgException.getDetailMessageArguments();
            return args.length > 1 ? args[1].toString() : "Invalid request data";
        } else if (exception instanceof MethodArgumentTypeMismatchException typeMismatchException) {
            String typeName = typeMismatchException.getRequiredType() != null 
                ? typeMismatchException.getRequiredType().getSimpleName() 
                : "Unknown";
            return messageService.getMessage("type.mismatch", 
                typeMismatchException.getName(), 
                typeName, 
                typeMismatchException.getValue());
        } else if (exception instanceof ConstraintViolationException constraintException) {
            return constraintException.getMessage();
        } else if (exception instanceof MissingServletRequestParameterException missingParamException) {
            return messageService.getMessage("parameter.missing", missingParamException.getParameterName());
        }
        return "Invalid request";
    }

    /**
     * Extract the details of the data integrity exception.
     */
    private static String extractDataIntegrityDetail(String exceptionMessage) {
        String notAvailable = "<>";
        if (exceptionMessage == null) {
            return notAvailable;
        }
        Matcher detailMatcher = Pattern.compile("Detail:\\s*([^.]+)").matcher(exceptionMessage);
        return detailMatcher.find() ? detailMatcher.group(1).trim() : notAvailable;
    }

}
