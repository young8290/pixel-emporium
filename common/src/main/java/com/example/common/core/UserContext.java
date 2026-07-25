package com.example.common.core;

// 用户上下文，记录当前登录用户的 id
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long id) {
        USER_ID.set(id);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static void remove() {
        USER_ID.remove();
    }
}
