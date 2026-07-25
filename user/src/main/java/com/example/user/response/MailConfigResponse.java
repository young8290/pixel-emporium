package com.example.user.response;
import com.example.user.enums.MailSecurityMode;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class MailConfigResponse {
    private Boolean enabled; private String host; private Integer port; private MailSecurityMode securityMode; private String username;
    private Boolean passwordConfigured; private String fromAddress; private String fromName; private String replyTo;
    private Integer connectionTimeoutMs; private Integer readTimeoutMs; private Integer version;
}
