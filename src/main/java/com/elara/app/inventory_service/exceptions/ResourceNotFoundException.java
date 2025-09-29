package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(Object... args) {
        super(ErrorCode.RESOURCE_NOT_FOUND, args);
    }

    public ResourceNotFoundException(String customMessage, Object... args) {
        super(ErrorCode.RESOURCE_NOT_FOUND, customMessage, args);
    }
}
