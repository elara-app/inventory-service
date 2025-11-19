package com.elara.app.inventory_service.config;

import com.elara.app.inventory_service.exceptions.BaseException;
import com.elara.app.inventory_service.utils.ErrorCode;
import com.elara.app.inventory_service.utils.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            exception.getCode(),
            exception.getValue(),
            exception.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, determineHttpStatus(exception.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            Arrays.stream(exception.getDetailMessageArguments()).toList().get(1).toString(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            messageService.getMessage("parameter.missing", exception.getParameterName()),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            messageService.getMessage("method.not.supported", request.getMethod()),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception, HttpServletRequest request) {
        String detail = getDataFromDataIntegrityExceptionMessage(exception.getMessage());
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.DATABASE_ERROR.getCode(),
            ErrorCode.DATABASE_ERROR.getValue(),
            "Integrity violation: " + messageService.getMessage("global.error.database", detail),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.UNEXPECTED_ERROR.getCode(),
            ErrorCode.UNEXPECTED_ERROR.getValue(),
            messageService.getMessage("global.error.unexpected", exception.getMessage()),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String getDataFromDataIntegrityExceptionMessage(String exceptionMessage) {
        String notAvailable = "<>";
        Matcher detailMatcher = Pattern.compile("Detail:\\s*([^.]+)").matcher(exceptionMessage);
        return detailMatcher.find() ? detailMatcher.group(1).trim() : notAvailable;
    }

    private ErrorResponse createErrorResponse(int code, String value, String message, String path) {
        return ErrorResponse.builder()
            .code(code)
            .value(value)
            .message(message)
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();
    }

    private HttpStatus determineHttpStatus(int errorCode) {
        return switch (errorCode) {
            case 1002 -> HttpStatus.BAD_REQUEST;
            case 1003 -> HttpStatus.CONFLICT;
            case 1004 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

}
