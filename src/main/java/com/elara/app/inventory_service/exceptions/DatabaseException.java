package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;

public class DatabaseException extends BaseException {
    public DatabaseException(Object... args) {
        super(ErrorCode.DATABASE_ERROR, args);
    }

    public DatabaseException(String customMessage, Object... args) {
        super(ErrorCode.DATABASE_ERROR, customMessage, args);
    }
}
