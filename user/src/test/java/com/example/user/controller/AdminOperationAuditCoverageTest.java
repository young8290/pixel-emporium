package com.example.user.controller;

import com.example.user.annotation.OperationLog;
import com.example.user.controller.admin.MailAdminController;
import com.example.user.controller.admin.UserAdminController;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.request.MailConnectionTestRequest;
import com.example.user.request.MailTestSendRequest;
import com.example.user.request.UserEnabledRequest;
import com.example.user.request.UserRolesRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminOperationAuditCoverageTest {

    @Test
    void userAdministrationWritesHaveContentFreeAuditMetadata() throws Exception {
        assertAudit(UserAdminController.class.getMethod("updateEnabled", Long.class, UserEnabledRequest.class),
                "USER_ENABLED_UPDATE", "USER", "#id");
        assertAudit(UserAdminController.class.getMethod("replaceRoles", Long.class, UserRolesRequest.class),
                "USER_ROLES_UPDATE", "USER", "#id");
    }

    @Test
    void mailAdministrationWritesHaveContentFreeAuditMetadata() throws Exception {
        assertAudit(MailAdminController.class.getMethod("put", MailConfigUpdateRequest.class),
                "MAIL_CONFIG_UPDATE", "MAIL_CONFIG", "");
        assertAudit(MailAdminController.class.getMethod("connection", MailConnectionTestRequest.class),
                "MAIL_CONNECTION_TEST", "MAIL_CONFIG", "");
        assertAudit(MailAdminController.class.getMethod("testSend", MailTestSendRequest.class),
                "MAIL_TEST_SEND", "MAIL_DELIVERY", "");
        assertAudit(MailAdminController.class.getMethod("retry", Long.class),
                "MAIL_DELIVERY_RETRY", "MAIL_DELIVERY", "#id");
    }

    private void assertAudit(Method method, String action, String targetType, String targetIdSpEL) {
        OperationLog audit = method.getAnnotation(OperationLog.class);
        assertNotNull(audit, method.getName() + " must be audited");
        assertEquals(action, audit.action());
        assertEquals(targetType, audit.targetType());
        assertEquals(targetIdSpEL, audit.targetIdSpEL());
        assertEquals("", audit.detail(), "credentials and personal data must not enter audit logs");
    }
}
