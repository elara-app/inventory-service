package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class UnexpectedErrorException extends BaseException {
    public UnexpectedErrorException(Object... args) {
        super(ErrorCode.UNEXPECTED_ERROR, args);
    }
}
