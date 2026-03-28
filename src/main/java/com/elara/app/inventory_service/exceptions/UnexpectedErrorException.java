package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class UnexpectedErrorException extends BaseException {
    public UnexpectedErrorException(String message) {
        super(ErrorCode.UNEXPECTED_ERROR, message);
    }
}
