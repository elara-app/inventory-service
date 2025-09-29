package com.elara.app.inventory_service.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    DATABASE_ERROR(1001, "DATABASE_ERROR", "global.error.database"),
    INVALID_DATA(1002, "INVALID_DATA", "inventoryItem.invalid.data"),
    RESOURCE_CONFLICT(1003, "RESOURCE_CONFLICT", "global.error.conflict"),
    RESOURCE_NOT_FOUND(1004, "RESOURCE_NOT_FOUND", "global.error.not.found"),
    SERVICE_UNAVAILABLE(1005, "SERVICE_UNAVAILABLE", "global.error.service.unavailable"),
    UNEXPECTED_ERROR(1006, "UNEXPECTED_ERROR", "global.error.unexpected");

    private final int code;
    private final String value;
    private final String key;

}
