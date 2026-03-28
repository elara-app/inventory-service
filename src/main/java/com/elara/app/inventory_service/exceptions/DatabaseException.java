package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class DatabaseException extends BaseException {
    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR, message);
    }
}
