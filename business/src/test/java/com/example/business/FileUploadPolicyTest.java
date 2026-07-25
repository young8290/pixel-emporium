package com.example.business;

import com.example.business.config.UploadSecurityProperties;
import com.example.business.service.FileUploadPolicy;
import com.example.business.service.impl.DefaultFileUploadPolicy;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUploadPolicyTest {

    @ParameterizedTest
    @MethodSource("allowedFiles")
    void acceptsAllowedTypeAndMatchingSignature(String filename, String contentType, byte[] bytes, String category) {
        FileUploadPolicy.FileUploadDescriptor descriptor = policy().validate(filename, contentType, bytes);

        assertEquals(category, descriptor.category());
        assertEquals(filename.substring(filename.lastIndexOf('.')), descriptor.suffix());
    }

    @Test
    void rejectsUnknownMimeInsteadOfUsingDefaultDirectory() {
        CustomValidationException exception = assertThrows(CustomValidationException.class,
                () -> policy().validate("payload.bin", "application/octet-stream", bytes("MZ")));

        assertEquals("不支持的文件类型", exception.getMessage());
    }

    @Test
    void rejectsMismatchedExtensionAndMime() {
        assertThrows(CustomValidationException.class,
                () -> policy().validate("picture.jpg", "application/pdf", pdfBytes()));
    }

    @Test
    void rejectsExecutableBytesWithImageExtension() {
        assertThrows(CustomValidationException.class,
                () -> policy().validate("picture.jpg", "image/jpeg", bytes("MZ executable")));
    }

    @Test
    void rejectsEmptyAndPathTraversalFilenames() {
        assertThrows(CustomValidationException.class,
                () -> policy().validate("", "image/png", pngBytes()));
        assertThrows(CustomValidationException.class,
                () -> policy().validate("..\\secret.png", "image/png", pngBytes()));
    }

    @Test
    void enforcesImageLimit() {
        UploadSecurityProperties properties = properties();
        properties.setImageMaxBytes(4);
        DefaultFileUploadPolicy limited = new DefaultFileUploadPolicy(properties);

        assertThrows(CustomValidationException.class,
                () -> limited.validate("picture.png", "image/png", withSize(pngBytes(), 5)));
    }

    @Test
    void acceptsImageAtExactLimitAndRejectsOneByteMore() {
        UploadSecurityProperties properties = properties();
        properties.setImageMaxBytes(9);
        DefaultFileUploadPolicy limited = new DefaultFileUploadPolicy(properties);

        limited.validate("picture.png", "image/png", withSize(pngBytes(), 9));
        assertThrows(CustomValidationException.class,
                () -> limited.validate("picture.png", "image/png", withSize(pngBytes(), 10)));
    }

    @Test
    void enforcesVideoLimit() {
        UploadSecurityProperties properties = properties();
        properties.setVideoMaxBytes(8);
        DefaultFileUploadPolicy limited = new DefaultFileUploadPolicy(properties);

        assertThrows(CustomValidationException.class,
                () -> limited.validate("movie.mp4", "video/mp4", withSize(mp4Bytes(), 9)));
    }

    @Test
    void enforcesDocumentLimit() {
        UploadSecurityProperties properties = properties();
        properties.setDocumentMaxBytes(8);
        DefaultFileUploadPolicy limited = new DefaultFileUploadPolicy(properties);

        assertThrows(CustomValidationException.class,
                () -> limited.validate("readme.pdf", "application/pdf", withSize(pdfBytes(), 9)));
    }

    @Test
    void usesConfiguredAllowedTypePairsInsteadOfAnUnchangeableWhitelist() {
        UploadSecurityProperties properties = properties();
        properties.setAllowedTypes(Map.of(".png", "image/png"));
        DefaultFileUploadPolicy configured = new DefaultFileUploadPolicy(properties);

        configured.validate("picture.png", "image/png", pngBytes());
        assertThrows(CustomValidationException.class,
                () -> configured.validate("picture.jpg", "image/jpeg", jpegBytes()));
    }

    private static Stream<Arguments> allowedFiles() {
        return Stream.of(
                Arguments.of("photo.jpg", "image/jpeg", jpegBytes(), "image"),
                Arguments.of("photo.png", "image/png", pngBytes(), "image"),
                Arguments.of("photo.webp", "image/webp", webpBytes(), "image"),
                Arguments.of("photo.gif", "image/gif", gifBytes(), "image"),
                Arguments.of("movie.mp4", "video/mp4", mp4Bytes(), "video"),
                Arguments.of("readme.pdf", "application/pdf", pdfBytes(), "document"),
                Arguments.of("legacy.doc", "application/msword", docBytes(), "document"),
                Arguments.of("modern.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes(), "document")
        );
    }

    private static DefaultFileUploadPolicy policy() {
        return new DefaultFileUploadPolicy(properties());
    }

    private static UploadSecurityProperties properties() {
        return new UploadSecurityProperties();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] jpegBytes() {
        return new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 'J', 'P', 'E', 'G'};
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0};
    }

    private static byte[] webpBytes() {
        return bytes("RIFF0000WEBP");
    }

    private static byte[] gifBytes() {
        return bytes("GIF89a");
    }

    private static byte[] mp4Bytes() {
        return bytes("0000ftypisom");
    }

    private static byte[] pdfBytes() {
        return bytes("%PDF-1.7");
    }

    private static byte[] docBytes() {
        return new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1};
    }

    private static byte[] docxBytes() {
        return new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0};
    }

    private static byte[] withSize(byte[] prefix, int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(prefix, 0, bytes, 0, Math.min(prefix.length, bytes.length));
        return bytes;
    }
}
