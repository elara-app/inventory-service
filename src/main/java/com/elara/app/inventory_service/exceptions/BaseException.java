package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ApplicationContextHolder;
import com.elara.app.inventory_service.utils.ErrorCode;
import com.elara.app.inventory_service.utils.MessageService;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final int code;
    private final String value;
    private final String message;

    public BaseException(ErrorCode errorCode, Object... args) {
        this.code = errorCode.getCode();
        this.value = errorCode.getValue();
        this.message = getLocalizeMessage(errorCode, args);
    }

    private String getLocalizeMessage(ErrorCode errorCode, Object... args) {
        try {
            MessageService messageService = ApplicationContextHolder.getBean(MessageService.class);
            return messageService.getMessage(errorCode, args);
        } catch (Exception e) {
            return errorCode.getValue();
        }
    }

}
