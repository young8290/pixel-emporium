package com.example.business.controller;

import com.example.business.annotation.OperationLog;
import com.example.business.controller.admin.ProductAdminController;
import com.example.business.controller.admin.FileAdminController;
import com.example.business.controller.admin.ProductImageAdminController;
import com.example.business.controller.admin.ProductSkuAdminController;
import com.example.business.request.ProductRequest;
import com.example.business.request.ProductSkuGenerateRequest;
import com.example.business.request.ProductSkuRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminWriteAuditCoverageTest {

    @Test
    void productAndSkuWritesHaveContentFreeAuditMetadata() throws Exception {
        assertAudit(ProductAdminController.class.getMethod("save", ProductRequest.class),
                "PRODUCT_SAVE", "PRODUCT", "#request.id");
        assertAudit(ProductSkuAdminController.class.getMethod("save", ProductSkuRequest.class),
                "SKU_SAVE", "SKU", "#request.id");
        assertAudit(ProductSkuAdminController.class.getMethod("generate", ProductSkuGenerateRequest.class),
                "SKU_GENERATE", "PRODUCT", "#request.productId");
        assertAudit(ProductSkuAdminController.class.getMethod("deleteBatch", Long.class, java.util.List.class),
                "SKU_BATCH_DELETE", "PRODUCT", "#productId");
    }

    @Test
    void productImageWritesHaveContentFreeAuditMetadata() throws Exception {
        assertAudit(ProductImageAdminController.class.getMethod("upload", Long.class, Long.class,
                        Integer.class, MultipartFile.class),
                "PRODUCT_IMAGE_UPLOAD", "PRODUCT", "#productId");
        assertAudit(ProductImageAdminController.class.getMethod("delete", Long.class),
                "PRODUCT_IMAGE_DELETE", "PRODUCT_IMAGE", "#id");
        assertAudit(ProductImageAdminController.class.getMethod("setMain", Long.class),
                "PRODUCT_IMAGE_SET_MAIN", "PRODUCT_IMAGE", "#id");
        assertAudit(ProductImageAdminController.class.getMethod("stylize", Long.class,
                        ProductImageAdminController.StylizeRequest.class),
                "PRODUCT_IMAGE_STYLIZE", "PRODUCT_IMAGE", "#id");
    }

    @Test
    void fileAndRemoteTransferWritesHaveContentFreeAuditMetadata() throws Exception {
        assertAudit(FileAdminController.class.getMethod("upload", MultipartFile.class),
                "FILE_UPLOAD", "FILE", "");
        assertAudit(FileAdminController.class.getMethod("uploadByUrl", String.class),
                "FILE_REMOTE_UPLOAD", "REMOTE_FILE", "");
        assertAudit(FileAdminController.class.getMethod("uploadFromVditor", MultipartFile[].class),
                "EDITOR_FILE_UPLOAD", "FILE", "");
        assertAudit(FileAdminController.class.getMethod("uploadOutsideLinkFromVditor", java.util.Map.class,
                        jakarta.servlet.http.HttpServletRequest.class),
                "EDITOR_REMOTE_FILE_UPLOAD", "REMOTE_FILE", "");
    }

    private void assertAudit(Method method, String action, String targetType, String targetIdSpEL) {
        OperationLog audit = method.getAnnotation(OperationLog.class);
        assertNotNull(audit, method.getName() + " must be audited");
        assertEquals(action, audit.action());
        assertEquals(targetType, audit.targetType());
        assertEquals(targetIdSpEL, audit.targetIdSpEL());
        assertEquals("", audit.detail(), "request data must not be copied into the audit detail");
    }
}
