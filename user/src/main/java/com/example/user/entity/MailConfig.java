package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.Version;
import com.example.common.orm.entity.BaseEntity;
import com.example.user.enums.MailSecurityMode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MailConfig extends BaseEntity {
    private Boolean enabled; private String host; private Integer port; private MailSecurityMode securityMode;
    private String username; private String passwordCiphertext; private String fromAddress; private String fromName;
    private String replyTo; private Integer connectionTimeoutMs; private Integer readTimeoutMs;
    @Version private Integer version;
}
