package com.example.common.exception;

public class CustomBusinessException extends CustomException {
    public CustomBusinessException(String message, int code) {
        super(message, code);
    }

    public CustomBusinessException(String message) {
        super(message, 400);
    }
}
