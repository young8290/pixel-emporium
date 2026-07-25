package com.example.common.exception;

public abstract class CustomException extends RuntimeException {
    private final int code;

    public CustomException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
