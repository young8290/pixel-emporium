package com.example.user;

import com.example.user.controller.HelloController;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class HelloControllerDependencyTest {
    @Test
    void helloControllerDoesNotRequireStaticMailSender() {
        assertFalse(Arrays.stream(HelloController.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(JavaMailSender.class::isAssignableFrom));
    }
}
