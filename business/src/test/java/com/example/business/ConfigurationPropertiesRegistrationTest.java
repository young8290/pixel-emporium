package com.example.business;

import com.example.business.config.MailNotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationPropertiesRegistrationTest {
    @Test
    void mailNotificationRecordIsRegisteredThroughApplicationConfiguration() {
        assertFalse(MailNotificationProperties.class.isAnnotationPresent(Component.class));
        EnableConfigurationProperties annotation = BusinessApplication.class
                .getAnnotation(EnableConfigurationProperties.class);
        assertNotNull(annotation);
        assertTrue(Arrays.asList(annotation.value()).contains(MailNotificationProperties.class));
    }
}
