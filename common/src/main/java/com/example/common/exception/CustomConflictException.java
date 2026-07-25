package com.example.common.exception;

public class CustomConflictException extends CustomException {
    public CustomConflictException(String message) {
        super(message, ErrorCode.CONFLICT.getCode());
    }
}
