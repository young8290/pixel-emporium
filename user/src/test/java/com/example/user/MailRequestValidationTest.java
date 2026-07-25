package com.example.user;

import com.example.user.enums.MailSecurityMode;
import com.example.user.enums.MailTemplateKey;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.request.MailTestSendRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsMailConfigurationBeforeBeanValidation() {
        MailConfigUpdateRequest request = new MailConfigUpdateRequest();
        request.setVersion(1);
        request.setEnabled(true);
        request.setHost(" smtp.example.com ");
        request.setPort(587);
        request.setSecurityMode(MailSecurityMode.STARTTLS);
        request.setUsername(" account@example.com ");
        request.setPassword(" smtp-secret ");
        request.setFromAddress(" shop@example.com ");
        request.setFromName(" Pixel Shop ");
        request.setReplyTo(" support@example.com ");
        request.setConnectionTimeoutMs(5_000);
        request.setReadTimeoutMs(10_000);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("smtp.example.com", request.getHost());
        assertEquals("shop@example.com", request.getFromAddress());
        assertEquals("Pixel Shop", request.getFromName());
    }

    @Test
    void trimsTestRecipientAndRejectsHeaderInjection() {
        MailTestSendRequest request = new MailTestSendRequest();
        request.setRecipient(" buyer@example.com ");
        request.setTemplateKey(MailTemplateKey.ORDER_CREATED);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("buyer@example.com", request.getRecipient());

        request.setRecipient("buyer@example.com\r\nBcc: attacker@example.com");
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("recipient")));
    }
}
