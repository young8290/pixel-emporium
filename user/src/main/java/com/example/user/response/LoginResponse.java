package com.example.user.response;

import lombok.*;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String email;
    private String token;
}
