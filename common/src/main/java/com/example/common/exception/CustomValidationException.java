package com.example.common.exception;

public class CustomValidationException extends CustomException {
    public CustomValidationException(String message, int code) {
        super(message, code);
    }

    public CustomValidationException(String message) {
        super(message, 400);
    }
}
