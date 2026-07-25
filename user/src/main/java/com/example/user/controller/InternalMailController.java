package com.example.user.controller;

import com.example.common.exception.CustomValidationException;
import com.example.common.response.R;
import com.example.user.model.OrderCreatedMailEvent;
import com.example.user.security.InternalMailRequestVerifier;
import com.example.user.service.MailDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("internal/mail")
public class InternalMailController {
    private final InternalMailRequestVerifier verifier;
    private final ObjectMapper json;
    private final MailDeliveryService delivery;

    public InternalMailController(InternalMailRequestVerifier verifier, ObjectMapper json,
                                  MailDeliveryService delivery) {
        this.verifier = verifier;
        this.json = json;
        this.delivery = delivery;
    }

    @PostMapping("order-created")
    public ResponseEntity<R<Boolean>> receive(@RequestHeader("X-Internal-Event-Id") String eventId,
                                               @RequestHeader("X-Internal-Timestamp") long timestamp,
                                               @RequestHeader("X-Internal-Signature") String signature,
                                               @RequestBody byte[] body) throws Exception {
        verifier.verify(timestamp, eventId, signature, body);
        OrderCreatedMailEvent event = json.readValue(body, OrderCreatedMailEvent.class);
        if (!eventId.equals(event.getEventId())) {
            throw new CustomValidationException("事件 ID 不匹配");
        }
        boolean created = delivery.accept(event);
        return created ? ResponseEntity.accepted().body(R.ok(true)) : ResponseEntity.ok(R.ok(true));
    }
}
