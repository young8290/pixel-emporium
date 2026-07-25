package com.example.user.util;

import cn.hutool.crypto.digest.BCrypt;

public class HashPw {
    public static void main(String[] args) {
        String plaintext = "xxx";
        String hashed = BCrypt.hashpw(plaintext);
        System.out.println(hashed);
        System.out.println(BCrypt.checkpw(plaintext, hashed));
    }
}
