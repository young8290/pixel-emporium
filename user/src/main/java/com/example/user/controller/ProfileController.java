package com.example.user.controller;

import com.example.common.response.R;
import com.example.common.exception.CustomValidationException;
import com.example.user.config.CustomRsaComponent;
import com.example.user.request.PasswordChangeRequest;
import com.example.user.request.ProfileUpdateRequest;
import com.example.user.response.ProfileResponse;
import com.example.user.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("profile")
public class ProfileController {
    private final AccountService accountService;
    private final CustomRsaComponent customRsaComponent;

    public ProfileController(AccountService accountService, CustomRsaComponent customRsaComponent) {
        this.accountService = accountService;
        this.customRsaComponent = customRsaComponent;
    }

    @GetMapping
    public R<ProfileResponse> profile() {
        return R.ok(accountService.profile());
    }

    @PutMapping
    public R<ProfileResponse> update(@RequestBody @Valid ProfileUpdateRequest request) {
        return R.ok(accountService.updateProfile(request));
    }

    @PutMapping("password")
    public R<Boolean> changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        accountService.changePassword(
                decryptPassword(request.getCurrentPassword()),
                decryptPassword(request.getNewPassword()));
        return R.ok(true);
    }

    private String decryptPassword(String encryptedPassword) {
        try {
            return customRsaComponent.decrypt(encryptedPassword);
        } catch (RuntimeException exception) {
            throw new CustomValidationException("密码密文无效");
        }
    }
}
