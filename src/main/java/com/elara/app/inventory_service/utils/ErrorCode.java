package com.elara.app.inventory_service.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DATABASE_ERROR(1001, "DATABASE_ERROR", "global.error.database", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_DATA(1002, "INVALID_DATA", "inventoryItem.invalid.data", HttpStatus.BAD_REQUEST),
    RESOURCE_CONFLICT(1003, "RESOURCE_CONFLICT", "global.error.conflict", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND(1004, "RESOURCE_NOT_FOUND", "global.error.not.found", HttpStatus.NOT_FOUND),
    SERVICE_UNAVAILABLE(1005, "SERVICE_UNAVAILABLE", "global.error.service.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    UNEXPECTED_ERROR(1006, "UNEXPECTED_ERROR", "global.error.unexpected", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String value;
    private final String key;
    private final HttpStatus httpStatus;

    /**
     * Gets the ErrorCode corresponding to a numeric code.
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNEXPECTED_ERROR; // Fallback to an unexpected error
    }

}
