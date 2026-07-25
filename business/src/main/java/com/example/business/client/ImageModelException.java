package com.example.business.client;

public class ImageModelException extends RuntimeException {
    public enum Reason { AUTHENTICATION, QUOTA, TIMEOUT, PROVIDER, INVALID_RESPONSE }

    private final Reason reason;

    public ImageModelException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ImageModelException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
