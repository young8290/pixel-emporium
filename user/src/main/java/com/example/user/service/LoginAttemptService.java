package com.example.user.service;

public interface LoginAttemptService {
    void onFailure(String email);

    void onSuccess(String email);

    boolean isBlocked(String email);

    void assertAllowed(String email);
}
