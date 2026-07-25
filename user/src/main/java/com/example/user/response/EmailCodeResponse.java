package com.example.user.response;
public record EmailCodeResponse(long cooldownSeconds, long expiresInSeconds) {}
