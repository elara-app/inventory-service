package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class ServiceUnavailableException extends BaseException {
    public ServiceUnavailableException(Object... args) {
        super(ErrorCode.SERVICE_UNAVAILABLE, args);
    }

    public ServiceUnavailableException(String customMessage, Object... args) {
        super(ErrorCode.SERVICE_UNAVAILABLE, customMessage, args);
    }
}
