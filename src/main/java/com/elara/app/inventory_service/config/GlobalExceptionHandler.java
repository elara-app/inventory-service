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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        assert exception.getRequiredType() != null;
        String message = messageService.getMessage("type.mismatch", exception.getName(), exception.getRequiredType().getSimpleName(), exception.getValue());
        log.info("GlobalExceptionHandler: {}", message);
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            message,
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
                ErrorCode.INVALID_DATA.getCode(),
                ErrorCode.INVALID_DATA.getValue(),
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        ErrorResponse errorResponse = createErrorResponse(
            ErrorCode.INVALID_DATA.getCode(),
            ErrorCode.INVALID_DATA.getValue(),
            messageService.getMessage("media.type.not.supported", exception.getContentType(), MediaType.APPLICATION_JSON_VALUE),
            request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
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
