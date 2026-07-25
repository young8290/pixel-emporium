package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import com.example.user.config.MailCenterProperties;
import com.example.user.entity.User;
import com.example.user.response.EmailCodeResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate redis; private final UserService users; private final MailConfigService config; private final MailTemplateService templates; private final MailCenterProperties properties;
    public EmailVerificationService(StringRedisTemplate redis, UserService users, MailConfigService config, MailTemplateService templates, MailCenterProperties properties) { this.redis = redis; this.users = users; this.config = config; this.templates = templates; this.properties = properties; }
    public EmailCodeResponse sendRegistrationCode(String email, String ip) {
        assertConfigured(); String normalized = normalizeEmail(email); if (users.getOne(new QueryWrapper<User>().eq("email", normalized)) != null) return response();
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000)); String digest = hash(normalized + "\nREGISTER\n" + code + "\n" + properties.verification().pepper());
        String emailKey = hash(normalized), ipKey = hash(ip == null ? "unknown" : ip), hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        List<String> keys = List.of(key("cooldown", emailKey), key("email-hour", emailKey) + ":" + hour, key("ip-hour", ipKey) + ":" + hour);
        String script = "if redis.call('EXISTS',KEYS[1]) == 1 then return 0 end; if tonumber(redis.call('GET',KEYS[2]) or '0') >= tonumber(ARGV[1]) or tonumber(redis.call('GET',KEYS[3]) or '0') >= tonumber(ARGV[2]) then return -1 end; local a=redis.call('INCR',KEYS[2]); if a == 1 then redis.call('EXPIRE',KEYS[2],3600) end; local b=redis.call('INCR',KEYS[3]); if b == 1 then redis.call('EXPIRE',KEYS[3],3600) end; redis.call('SET',KEYS[1],'1','EX',ARGV[3]); return 1";
        Long allowed = redis.execute(new DefaultRedisScript<>(script, Long.class), keys, Integer.toString(properties.verification().emailHourlyLimit()), Integer.toString(properties.verification().ipHourlyLimit()), Long.toString(properties.verification().resendCooldownSeconds()));
        if (allowed == null || allowed == 0) throw new CustomValidationException("验证码发送过于频繁"); if (allowed < 0) throw new CustomValidationException("验证码发送次数已达上限");
        try { redis.opsForValue().set(key("code", emailKey), digest, java.time.Duration.ofSeconds(properties.verification().ttlSeconds())); redis.delete(key("attempts", emailKey)); MailTemplateService.RenderedMail mail = templates.verification(code); var message = config.sender().createMimeMessage(); MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name()); var smtp = config.current(); helper.setTo(normalized); helper.setFrom(smtp.getFromAddress(), smtp.getFromName()); if (smtp.getReplyTo() != null && !smtp.getReplyTo().isBlank()) helper.setReplyTo(smtp.getReplyTo()); helper.setSubject(mail.subject()); helper.setText(mail.text(), mail.html()); config.sender().send(message); return response();
        } catch (Exception e) { redis.delete(List.of(key("code", emailKey), key("cooldown", emailKey))); if (e instanceof CustomBusinessException business) throw business; throw new CustomBusinessException("验证码发送失败", 503); }
    }
    public void assertValid(String normalizedEmail, String code) { assertConfigured(); String digest = redis.opsForValue().get(key("code", hash(normalizedEmail))); if (digest == null) throw new CustomValidationException("验证码已过期或不存在"); Long attempts = redis.opsForValue().increment(key("attempts", hash(normalizedEmail))); if (attempts != null && attempts == 1) redis.expire(key("attempts", hash(normalizedEmail)), java.time.Duration.ofSeconds(properties.verification().ttlSeconds())); if (attempts != null && attempts > properties.verification().maxAttempts()) { consume(normalizedEmail); throw new CustomValidationException("验证码错误次数过多"); } String expected = hash(normalizedEmail + "\nREGISTER\n" + code + "\n" + properties.verification().pepper()); if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) throw new CustomValidationException("邮箱验证码不正确"); }
    public void consume(String normalizedEmail) { String digest = hash(normalizedEmail); redis.delete(List.of(key("code", digest), key("attempts", digest))); }
    public String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
    private EmailCodeResponse response() { return new EmailCodeResponse(properties.verification().resendCooldownSeconds(), properties.verification().ttlSeconds()); }
    private void assertConfigured() { String pepper = properties.verification() == null ? null : properties.verification().pepper(); if (pepper == null || pepper.trim().getBytes(StandardCharsets.UTF_8).length < 32) throw new CustomBusinessException("邮件验证码服务未配置", 503); }
    private String key(String name, String hash) { return "mail:verify:register:" + name + ":{" + hash + "}"; }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
