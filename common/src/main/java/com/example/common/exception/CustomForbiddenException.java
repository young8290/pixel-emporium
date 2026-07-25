package com.example.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 403 无权限异常
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CustomForbiddenException extends CustomException {
    public CustomForbiddenException() {
        super("权限不足", 403);
    }

    public CustomForbiddenException(String message) {
        super(message, 403);
    }
}
