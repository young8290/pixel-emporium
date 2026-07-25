package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomValidationException;
import com.example.user.entity.MailConfig;
import com.example.user.mapper.MailConfigMapper;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.response.MailConfigResponse;
import com.example.user.security.MailSecretCipher;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailConfigService {
    private final MailConfigMapper mapper; private final MailSecretCipher cipher; private final DynamicMailSenderFactory senderFactory;
    public MailConfigService(MailConfigMapper mapper, MailSecretCipher cipher, DynamicMailSenderFactory senderFactory) { this.mapper = mapper; this.cipher = cipher; this.senderFactory = senderFactory; }
    public MailConfigResponse get() { return response(require()); }
    public MailConfig update(MailConfigUpdateRequest request) {
        request.normalize(); MailConfig current = require(); if (!current.getVersion().equals(request.getVersion())) throw new CustomConflictException("邮件配置已被其他管理员更新，请重新加载");
        boolean needsPassword = Boolean.TRUE.equals(request.getEnabled()) && !StringUtils.hasText(current.getPasswordCiphertext()) && !StringUtils.hasText(request.getPassword());
        if (Boolean.TRUE.equals(request.getEnabled()) && (!StringUtils.hasText(request.getHost()) || request.getPort() == null || !StringUtils.hasText(request.getFromName()) || !StringUtils.hasText(request.getFromAddress()) || needsPassword)) throw new CustomValidationException("启用邮件服务必须提供主机、端口、发件名称、发件地址和密码");
        current.setEnabled(request.getEnabled()); current.setHost(request.getHost()); current.setPort(request.getPort()); current.setSecurityMode(request.getSecurityMode()); current.setUsername(request.getUsername()); current.setFromAddress(request.getFromAddress()); current.setFromName(request.getFromName()); current.setReplyTo(request.getReplyTo()); current.setConnectionTimeoutMs(request.getConnectionTimeoutMs()); current.setReadTimeoutMs(request.getReadTimeoutMs());
        if (StringUtils.hasText(request.getPassword())) current.setPasswordCiphertext(cipher.encryptSmtpPassword(request.getPassword()));
        int changed = mapper.update(current, new LambdaUpdateWrapper<MailConfig>().eq(MailConfig::getId, 1L).eq(MailConfig::getVersion, request.getVersion()));
        if (changed != 1) throw new CustomConflictException("邮件配置已被其他管理员更新，请重新加载"); senderFactory.clear(); return require();
    }
    public JavaMailSenderImpl sender() { MailConfig config = require(); if (!Boolean.TRUE.equals(config.getEnabled())) throw new CustomBusinessException("邮件服务未启用", 503); return senderFactory.create(config); }
    public MailConfig current() { return require(); }
    private MailConfig require() { MailConfig config = mapper.selectById(1L); if (config == null) throw new CustomBusinessException("邮件配置未初始化", 503); return config; }
    private MailConfigResponse response(MailConfig c) { MailConfigResponse r = new MailConfigResponse(); r.setEnabled(c.getEnabled()); r.setHost(c.getHost()); r.setPort(c.getPort()); r.setSecurityMode(c.getSecurityMode()); r.setUsername(c.getUsername()); r.setPasswordConfigured(StringUtils.hasText(c.getPasswordCiphertext())); r.setFromAddress(c.getFromAddress()); r.setFromName(c.getFromName()); r.setReplyTo(c.getReplyTo()); r.setConnectionTimeoutMs(c.getConnectionTimeoutMs()); r.setReadTimeoutMs(c.getReadTimeoutMs()); r.setVersion(c.getVersion()); return r; }
}
