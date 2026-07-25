package com.example.user.request;

import com.example.user.enums.MailTemplateKey;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MailTestSendRequest {
    @NotBlank
    @Email
    @Pattern(regexp = "^[^\\r\\n\\p{Cc}]+$", message = "邮件地址不能包含控制字符")
    private String recipient;

    @NotNull
    @Setter
    private MailTemplateKey templateKey;

    public void setRecipient(String recipient) {
        this.recipient = recipient == null ? null : recipient.trim();
    }
}
