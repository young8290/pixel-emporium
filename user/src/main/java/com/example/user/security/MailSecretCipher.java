package com.example.user.security;

import com.example.common.exception.CustomBusinessException;
import com.example.user.config.MailCenterProperties;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Component
public class MailSecretCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final MailCenterProperties properties;
    public MailSecretCipher(MailCenterProperties properties) { this.properties = properties; }
    public String encryptSmtpPassword(String value) { return encrypt(value, "sb-shop:mail-config:v1"); }
    public String decryptSmtpPassword(String value) { return decrypt(value, "sb-shop:mail-config:v1"); }
    public String encryptDeliveryModel(String value) { return encrypt(value, "sb-shop:mail-delivery-model:v1"); }
    public String decryptDeliveryModel(String value) { return decrypt(value, "sb-shop:mail-delivery-model:v1"); }
    private String encrypt(String value, String aad) {
        try { byte[] iv = new byte[12]; RANDOM.nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv)); cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)); byte[] packed = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length); return "v1:" + Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) { throw unavailable(); }
    }
    private String decrypt(String value, String aad) {
        try { if (value == null || !value.startsWith("v1:")) throw unavailable(); byte[] packed = Base64.getDecoder().decode(value.substring(3));
            if (packed.length < 29) throw unavailable(); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOfRange(packed, 0, 12)));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8)); return new String(cipher.doFinal(Arrays.copyOfRange(packed, 12, packed.length)), StandardCharsets.UTF_8);
        } catch (CustomBusinessException e) { throw e; } catch (Exception e) { throw new CustomBusinessException("邮件密文无效"); }
    }
    private SecretKeySpec key() {
        try { String configured = properties.crypto() == null ? null : properties.crypto().masterKeyBase64();
            byte[] bytes = configured == null ? new byte[0] : Base64.getDecoder().decode(configured.trim());
            if (bytes.length != 32) throw unavailable(); return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException e) { throw unavailable(); }
    }
    private CustomBusinessException unavailable() { return new CustomBusinessException("邮件加密密钥不可用", 503); }
}
