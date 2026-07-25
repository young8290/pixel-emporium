package com.example.business;

import com.aliyun.oss.OSS;
import com.example.business.config.UploadSecurityProperties;
import com.example.business.controller.admin.FileAdminController;
import com.example.business.service.FileUploadPolicy;
import com.example.business.service.RemoteFileFetcher;
import com.example.business.service.impl.DefaultFileUploadPolicy;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileUploadSecurityTest {
    private final OSS oss = Mockito.mock(OSS.class);
    private final RemoteFileFetcher remoteFetcher = Mockito.mock(RemoteFileFetcher.class);
    private final UploadSecurityProperties properties = new UploadSecurityProperties();
    private final FileUploadPolicy policy = new DefaultFileUploadPolicy(properties);
    private final FileAdminController controller = new FileAdminController(
            oss, policy, remoteFetcher, properties, "bucket", "oss.example.com");

    @AfterEach
    void clearUser() {
        UserContext.remove();
    }

    @Test
    void rejectsUnknownMultipartTypeWithoutCallingOss() {
        UserContext.set(7L);

        R<String> result = controller.upload(new MockMultipartFile(
                "file", "payload.bin", "application/octet-stream", bytes("MZ")));

        assertEquals(422, result.getCode());
        verifyNoInteractions(oss);
    }

    @Test
    void rejectsOversizedImageWithoutCallingOss() {
        UserContext.set(7L);
        properties.setImageMaxBytes(4);

        R<String> result = controller.upload(new MockMultipartFile(
                "file", "picture.png", "image/png", withSize(pngBytes(), 5)));

        assertEquals(422, result.getCode());
        verifyNoInteractions(oss);
    }

    @Test
    void rejectsExecutableBytesWithImageExtensionWithoutCallingOss() {
        UserContext.set(7L);

        R<String> result = controller.upload(new MockMultipartFile(
                "file", "picture.jpg", "image/jpeg", bytes("MZ executable")));

        assertEquals(422, result.getCode());
        verifyNoInteractions(oss);
    }

    @Test
    void acceptsValidMultipartImageWithExistingResponseShape() {
        UserContext.set(7L);

        R<String> result = controller.upload(new MockMultipartFile(
                "file", "picture.png", "image/png", pngBytes()));

        assertEquals(200, result.getCode());
        assertTrue(result.getData().startsWith("https://bucket.oss.example.com/7/image/"));
        verify(oss).putObject(eq("bucket"), anyString(), any(java.io.InputStream.class));
    }

    @Test
    void returnsValidationFailureForDangerousUploadByUrl() {
        UserContext.set(7L);
        when(remoteFetcher.fetch(URI.create("http://127.0.0.1/image.png")))
                .thenThrow(new CustomValidationException("远程地址不允许访问内网或本地资源"));

        R<Map<String, String>> result = controller.uploadByUrl("http://127.0.0.1/image.png");

        assertEquals(422, result.getCode());
        verifyNoInteractions(oss);
    }

    @Test
    void transfersSafeUploadByUrlAndPreservesMapFields() {
        UserContext.set(7L);
        URI source = URI.create("https://cdn.example.com/image.png");
        when(remoteFetcher.fetch(source)).thenReturn(new RemoteFileFetcher.RemoteFilePayload(
                source, "image/png", "image.png", pngBytes()));

        R<Map<String, String>> result = controller.uploadByUrl(source.toString());

        assertEquals(200, result.getCode());
        assertEquals(source.toString(), result.getData().get("originalURL"));
        assertTrue(result.getData().get("url").startsWith("https://bucket.oss.example.com/7/image/"));
        verify(oss).putObject(eq("bucket"), anyString(), any(java.io.InputStream.class));
    }

    @Test
    void addsAnImageSuffixWhenRemoteUrlHasNoExtension() {
        UserContext.set(7L);
        URI source = URI.create("https://cdn.example.com/pixel");
        when(remoteFetcher.fetch(source)).thenReturn(new RemoteFileFetcher.RemoteFilePayload(
                source, "image/png", "pixel", pngBytes()));

        R<Map<String, String>> result = controller.uploadByUrl(source.toString());

        assertEquals(200, result.getCode());
        verify(oss).putObject(eq("bucket"), argThat(name -> name.endsWith(".png")),
                any(java.io.InputStream.class));
    }

    @Test
    void preservesLegacyPdfAndWordObjectDirectories() {
        UserContext.set(7L);

        assertEquals(200, controller.upload(new MockMultipartFile(
                "file", "guide.pdf", "application/pdf", bytes("%PDF-1.7"))).getCode());
        verify(oss).putObject(eq("bucket"), argThat(name -> name.startsWith("7/pdf/")),
                any(java.io.InputStream.class));

        assertEquals(200, controller.upload(new MockMultipartFile(
                "file", "guide.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{'P', 'K', 3, 4, 0})).getCode());
        verify(oss).putObject(eq("bucket"), argThat(name -> name.startsWith("7/word/")),
                any(java.io.InputStream.class));
    }

    @Test
    void deletesObjectWhenOssWriteFails() {
        UserContext.set(7L);
        doThrow(new RuntimeException("provider failure")).when(oss)
                .putObject(eq("bucket"), anyString(), any(java.io.InputStream.class));

        R<String> result = controller.upload(new MockMultipartFile(
                "file", "picture.png", "image/png", pngBytes()));

        assertEquals(400, result.getCode());
        verify(oss).deleteObject(eq("bucket"), anyString());
    }

    @Test
    void keepsVditorResponseFieldsWhenOneFileFails() throws Exception {
        UserContext.set(7L);
        MockMultipartFile valid = new MockMultipartFile("file[]", "picture.png", "image/png", pngBytes());
        MockMultipartFile invalid = new MockMultipartFile("file[]", "payload.bin", "application/octet-stream", bytes("MZ"));

        Map<String, Object> result = controller.uploadFromVditor(new org.springframework.web.multipart.MultipartFile[]{valid, invalid});

        assertEquals(-1, result.get("code"));
        assertEquals("部分文件上传失败，请检查文件类型和大小", result.get("msg"));
        String dataJson = new ObjectMapper().writeValueAsString(result.get("data"));
        assertTrue(dataJson.contains("errFiles"));
        assertTrue(dataJson.contains("succMap"));
    }

    @Test
    void securesVditorOutsideLinkTransferWithoutChangingResponseShape() {
        UserContext.set(7L);
        URI source = URI.create("https://cdn.example.com/image.png");
        when(remoteFetcher.fetch(source)).thenReturn(new RemoteFileFetcher.RemoteFilePayload(
                source, "image/png", "image.png", pngBytes()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("shop.example.com");
        request.setServerPort(80);

        Map<String, Object> result = controller.uploadOutsideLinkFromVditor(Map.of("url", source.toString()), request);

        assertEquals(0, result.get("code"));
        assertEquals("外部图片链接已成功上传到服务器", result.get("msg"));
        verify(oss).putObject(eq("bucket"), anyString(), any(java.io.InputStream.class));
    }

    @Test
    void rejectsNonHttpUrlEvenWhenItsHostLooksLikeTheOssHost() {
        UserContext.set(7L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("shop.example.com");
        request.setServerPort(80);

        Map<String, Object> result = controller.uploadOutsideLinkFromVditor(
                Map.of("url", "ftp://bucket.oss.example.com/image.png"), request);

        assertEquals(-1, result.get("code"));
        result = controller.uploadOutsideLinkFromVditor(
                Map.of("url", "https://user:pass@bucket.oss.example.com/image.png"), request);
        assertEquals(-1, result.get("code"));
        verifyNoInteractions(oss);
    }

    @Test
    void rejectsBlankVditorUrlWithFailureCode() {
        Map<String, Object> result = controller.uploadOutsideLinkFromVditor(Map.of("url", "  "), new MockHttpServletRequest());

        assertEquals(-1, result.get("code"));
        verifyNoInteractions(oss);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0};
    }

    private static byte[] withSize(byte[] prefix, int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(prefix, 0, bytes, 0, Math.min(prefix.length, bytes.length));
        return bytes;
    }
}
