package com.example.user.service;

import com.example.user.request.ProfileUpdateRequest;
import com.example.user.response.ProfileResponse;

public interface AccountService {
    ProfileResponse profile();

    ProfileResponse updateProfile(ProfileUpdateRequest request);

    void changePassword(String currentPassword, String newPassword);
}
