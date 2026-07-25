package com.example.user.util;

import cn.hutool.crypto.asymmetric.RSA;

public class KeyGenerator {
    public static void main(String[] args) {
        // 1. 创建 RSA 对象（会自动生成一对新的密钥）
        RSA rsa = new RSA();

        // 2. 获取私钥（Base64格式）- 给后端使用，存储在配置文件中
        String privateKey = rsa.getPrivateKeyBase64();
        System.out.println("----- 后端私钥 (Private Key) -----");
        System.out.println(privateKey);

        // 3. 获取公钥（Base64格式）- 给前端加密使用
        String publicKey = rsa.getPublicKeyBase64();
        System.out.println("\n");
        System.out.println("----- 前端公钥 (Public Key) -----");
        System.out.println(publicKey);
    }
}
