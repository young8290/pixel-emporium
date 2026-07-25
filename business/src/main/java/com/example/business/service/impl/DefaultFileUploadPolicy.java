package com.example.business.service.impl;

import com.example.business.config.UploadSecurityProperties;
import com.example.business.service.FileUploadPolicy;
import com.example.common.exception.CustomValidationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class DefaultFileUploadPolicy implements FileUploadPolicy {
    private final UploadSecurityProperties properties;
    private final Map<String, AllowedType> allowedTypes;

    public DefaultFileUploadPolicy(UploadSecurityProperties properties) {
        this.properties = properties;
        this.allowedTypes = configuredAllowedTypes(properties.getAllowedTypes());
    }

    @Override
    public FileUploadDescriptor validate(String filename, String contentType, byte[] bytes) {
        if (filename == null || filename.isBlank() || filename.indexOf('\u0000') >= 0
                || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0
                || filename.contains("..")) {
            throw new CustomValidationException("文件名无效");
        }
        if (bytes == null) {
            throw new CustomValidationException("文件内容不能为空");
        }

        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            throw new CustomValidationException("文件扩展名无效");
        }
        String suffix = filename.substring(dot).toLowerCase(Locale.ROOT);
        String normalizedContentType = normalizeContentType(contentType);
        AllowedType allowedType = allowedTypes.get(suffix);
        if (allowedType == null || !allowedType.contentType().equals(normalizedContentType)) {
            throw new CustomValidationException("不支持的文件类型");
        }

        long maxBytes = maxBytesFor(allowedType.category());
        if (bytes.length > maxBytes) {
            throw new CustomValidationException("文件大小超过限制");
        }
        if (!hasExpectedSignature(suffix, bytes)) {
            throw new CustomValidationException("文件内容与扩展名不匹配");
        }

        return new FileUploadDescriptor(suffix, allowedType.category(), normalizedContentType, bytes);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new CustomValidationException("文件类型不能为空");
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private long maxBytesFor(String category) {
        return switch (category) {
            case "image" -> properties.getImageMaxBytes();
            case "video" -> properties.getVideoMaxBytes();
            case "document" -> properties.getDocumentMaxBytes();
            default -> throw new CustomValidationException("不支持的文件类型");
        };
    }

    private boolean hasExpectedSignature(String suffix, byte[] bytes) {
        return switch (suffix) {
            case ".jpg", ".jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
            case ".png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case ".webp" -> startsWithAscii(bytes, "RIFF") && bytes.length >= 12 && asciiAt(bytes, 8, "WEBP");
            case ".gif" -> startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a");
            case ".mp4" -> bytes.length >= 8 && asciiAt(bytes, 4, "ftyp");
            case ".pdf" -> startsWithAscii(bytes, "%PDF");
            case ".doc" -> startsWith(bytes, 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1);
            case ".docx" -> startsWith(bytes, 0x50, 0x4b, 0x03, 0x04);
            default -> false;
        };
    }

    private Map<String, AllowedType> configuredAllowedTypes(Map<String, String> configured) {
        Map<String, AllowedType> result = new LinkedHashMap<>();
        configured.forEach((extension, contentType) -> {
            String normalizedExtension = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
            String normalizedContentType = normalizeConfiguredContentType(contentType);
            String category = categoryFor(normalizedContentType);
            if (normalizedExtension.startsWith(".") && normalizedExtension.length() > 1 && category != null) {
                result.put(normalizedExtension, new AllowedType(normalizedContentType, category));
            }
        });
        return Map.copyOf(result);
    }

    private String normalizeConfiguredContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String categoryFor(String contentType) {
        if (contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType.startsWith("video/")) {
            return "video";
        }
        return switch (contentType) {
            case "application/pdf", "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "document";
            default -> null;
        };
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xff) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String expected) {
        return bytes.length >= expected.length() && asciiAt(bytes, 0, expected);
    }

    private boolean asciiAt(byte[] bytes, int offset, String expected) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expectedBytes.length) {
            return false;
        }
        for (int i = 0; i < expectedBytes.length; i++) {
            if (bytes[offset + i] != expectedBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private record AllowedType(String contentType, String category) {
    }
}
