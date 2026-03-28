package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class ServiceUnavailableException extends BaseException {
    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
    }
}
