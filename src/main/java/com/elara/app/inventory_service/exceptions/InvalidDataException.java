package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class InvalidDataException extends BaseException {
    public InvalidDataException(String message) {
        super(ErrorCode.INVALID_DATA, message);
    }
}
