package com.example.user.config;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CustomRsaComponent {
    @Value("${custom.private-key}")
    private String PRIVATE_KEY;

    private RSA rsa;

    @PostConstruct
    public void init() {
        this.rsa = new RSA(PRIVATE_KEY, null);
    }

    public String decrypt(String encryptText) {
        return rsa.decryptStr(encryptText, KeyType.PrivateKey);
    }
}
