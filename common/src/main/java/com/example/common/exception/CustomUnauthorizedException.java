package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class CustomUnauthorizedException extends CustomException {
    public CustomUnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED.value());
    }
}