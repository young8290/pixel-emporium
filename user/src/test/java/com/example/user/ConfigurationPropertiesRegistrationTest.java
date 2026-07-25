package com.example.user;

import com.example.user.config.MailCenterProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationPropertiesRegistrationTest {
    @Test
    void mailCenterRecordIsRegisteredThroughApplicationConfiguration() {
        assertFalse(MailCenterProperties.class.isAnnotationPresent(Component.class));
        EnableConfigurationProperties annotation = UserApplication.class
                .getAnnotation(EnableConfigurationProperties.class);
        assertNotNull(annotation);
        assertTrue(Arrays.asList(annotation.value()).contains(MailCenterProperties.class));
    }
}
