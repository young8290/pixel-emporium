package com.example.user.service;

import com.example.user.entity.User;

public interface AuthenticationService {
    User authenticate(String email, String password);

    boolean register(String email, String emailCode, String rawPassword, String name, String mobile);

    void logout(String token);

    String refreshToken(String token);

    String generateToken(User user);

    boolean validateToken(String token);
}
