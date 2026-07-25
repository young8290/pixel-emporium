package com.example.user.controller.admin;

import com.example.common.annotation.CustomRequireRole;
import com.example.common.exception.CustomBusinessException;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import com.example.user.annotation.OperationLog;
import com.example.user.enums.MailTemplateKey;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.request.MailConnectionTestRequest;
import com.example.user.request.MailDeliveryListRequest;
import com.example.user.request.MailTestSendRequest;
import com.example.user.response.MailConfigResponse;
import com.example.user.response.MailDeliveryResponse;
import com.example.user.service.MailConfigService;
import com.example.user.service.MailDeliveryService;
import com.example.user.service.MailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/mail")
@CustomRequireRole("SUPER_ADMIN")
public class MailAdminController {
    private final MailConfigService config;
    private final MailDeliveryService delivery;
    private final MailTemplateService templates;
    public MailAdminController(MailConfigService c, MailDeliveryService d, MailTemplateService t) { config = c; delivery = d; templates = t; }
    @GetMapping("config") public R<MailConfigResponse> get() { return R.ok(config.get()); }
    @PutMapping("config")
    @OperationLog(action = "MAIL_CONFIG_UPDATE", targetType = "MAIL_CONFIG")
    public R<MailConfigResponse> put(@RequestBody @Valid MailConfigUpdateRequest request) { config.update(request); return R.ok(config.get()); }
    @PostMapping("test-connection")
    @OperationLog(action = "MAIL_CONNECTION_TEST", targetType = "MAIL_CONFIG")
    public R<Boolean> connection(@RequestBody(required = false) MailConnectionTestRequest request) {
        try { config.sender().testConnection(); return R.ok(true); } catch (MessagingException e) { throw new CustomBusinessException("SMTP 连接失败", 503); }
    }
    @PostMapping("test-send")
    @OperationLog(action = "MAIL_TEST_SEND", targetType = "MAIL_DELIVERY")
    public R<Boolean> testSend(@RequestBody @Valid MailTestSendRequest request) { delivery.queueTest(request.getRecipient(), request.getTemplateKey()); return R.ok(true); }
    @GetMapping(value = "preview", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8") public String preview(@RequestParam MailTemplateKey templateKey) { return templates.preview(templateKey); }
    @GetMapping("delivery/list") public R<PageResponse<MailDeliveryResponse>> list(@Valid MailDeliveryListRequest request) { return R.ok(delivery.list(request)); }
    @PostMapping("delivery/{id}/retry")
    @OperationLog(action = "MAIL_DELIVERY_RETRY", targetType = "MAIL_DELIVERY", targetIdSpEL = "#id")
    public R<Boolean> retry(@PathVariable Long id) { delivery.retry(id); return R.ok(true); }
}
