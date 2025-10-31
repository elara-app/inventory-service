package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class ResourceConflictException extends BaseException {
    public ResourceConflictException(Object... args) {
        super(ErrorCode.RESOURCE_CONFLICT, args);
    }
}
