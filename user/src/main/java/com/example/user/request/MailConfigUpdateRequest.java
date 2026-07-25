package com.example.user.request;

import com.example.user.enums.MailSecurityMode;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MailConfigUpdateRequest {
    @NotNull @Setter private Integer version;
    @NotNull @Setter private Boolean enabled;
    @Size(max = 255) @Pattern(regexp = "^$|^[^\\r\\n\\p{Cc}]+$", message = "SMTP 文本不能包含控制字符") private String host;
    @Min(1) @Max(65535) @Setter private Integer port;
    @NotNull @Setter private MailSecurityMode securityMode;
    @Size(max = 255) @Pattern(regexp = "^$|^[^\\r\\n\\p{Cc}]+$", message = "SMTP 文本不能包含控制字符") private String username;
    private String password;
    @Email @Size(max = 255) @Pattern(regexp = "^$|^[^\\r\\n\\p{Cc}]+$", message = "邮件地址不能包含控制字符") private String fromAddress;
    @Size(max = 100) @Pattern(regexp = "^$|^[^\\r\\n\\p{Cc}]+$", message = "邮件头不能包含控制字符") private String fromName;
    @Email @Size(max = 255) @Pattern(regexp = "^$|^[^\\r\\n\\p{Cc}]+$", message = "邮件地址不能包含控制字符") private String replyTo;
    @NotNull @Min(1000) @Max(60000) @Setter private Integer connectionTimeoutMs;
    @NotNull @Min(1000) @Max(60000) @Setter private Integer readTimeoutMs;

    public void setHost(String host) { this.host = trim(host); }
    public void setUsername(String username) { this.username = trim(username); }
    public void setPassword(String password) { this.password = trim(password); }
    public void setFromAddress(String fromAddress) { this.fromAddress = trim(fromAddress); }
    public void setFromName(String fromName) { this.fromName = trim(fromName); }
    public void setReplyTo(String replyTo) { this.replyTo = trim(replyTo); }

    public void normalize() { host = trim(host); username = trim(username); password = trim(password); fromAddress = trim(fromAddress); fromName = trim(fromName); replyTo = trim(replyTo); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
