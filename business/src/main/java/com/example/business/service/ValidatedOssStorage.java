package com.example.business.service;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

@Service
public class ValidatedOssStorage {
    private static final Logger log = LoggerFactory.getLogger(ValidatedOssStorage.class);

    private final OSS ossClient;
    private final FileUploadPolicy uploadPolicy;
    private final String bucket;
    private final String endpoint;

    @Autowired
    public ValidatedOssStorage(ObjectProvider<OSS> ossClientProvider, FileUploadPolicy uploadPolicy,
                               @Value("${alibaba.cloud.oss.bucket}") String bucket,
                               @Value("${alibaba.cloud.oss.endpoint}") String endpoint) {
        this(ossClientProvider.getIfAvailable(), uploadPolicy, bucket, endpoint);
    }

    public ValidatedOssStorage(OSS ossClient, FileUploadPolicy uploadPolicy,
                               @Value("${alibaba.cloud.oss.bucket}") String bucket,
                               @Value("${alibaba.cloud.oss.endpoint}") String endpoint) {
        this.ossClient = ossClient;
        this.uploadPolicy = uploadPolicy;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    public String storeDefault(String filename, String contentType, byte[] bytes) {
        return store(UserContext.get(), null, null, filename, contentType, bytes);
    }

    public String storeProductImage(Long ownerId, String filename, String contentType, byte[] bytes) {
        return store(ownerId, "image", "image", filename, contentType, bytes);
    }

    public String storeStyled(Long ownerId, String filename, String contentType, byte[] bytes) {
        return store(ownerId, "styled", "image", filename, contentType, bytes);
    }

    public void deleteOwnObject(String publicUrl) {
        String objectName = objectName(publicUrl);
        if (objectName == null) {
            return;
        }
        if (ossClient == null) {
            log.warn("OSS 未配置，跳过对象清理");
            return;
        }
        try {
            ossClient.deleteObject(bucket, objectName);
        } catch (RuntimeException exception) {
            log.warn("OSS 对象清理失败，类型={}", exception.getClass().getSimpleName());
        }
    }

    private String store(Long ownerId, String directory, String expectedCategory,
                         String filename, String contentType, byte[] bytes) {
        OSS client = requireOssClient();
        FileUploadPolicy.FileUploadDescriptor descriptor = uploadPolicy.validate(filename, contentType, bytes);
        if (expectedCategory != null && !expectedCategory.equals(descriptor.category())) {
            throw new CustomValidationException("商品图片仅支持图片类型");
        }
        String resolvedDirectory = directory == null ? objectDirectory(descriptor) : directory;
        String owner = ownerId == null ? "system" : ownerId.toString();
        String objectName = owner + "/" + resolvedDirectory + "/"
                + IdUtil.getSnowflakeNextIdStr() + descriptor.suffix();
        try (InputStream inputStream = new ByteArrayInputStream(descriptor.bytes())) {
            client.putObject(bucket, objectName, inputStream);
            return String.format("https://%s.%s/%s", bucket, endpoint, objectName);
        } catch (Exception exception) {
            deleteQuietly(objectName);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("OSS 上传失败", exception);
        }
    }

    private void deleteQuietly(String objectName) {
        if (ossClient == null) {
            return;
        }
        try {
            ossClient.deleteObject(bucket, objectName);
        } catch (RuntimeException cleanupException) {
            log.warn("OSS 对象清理失败，类型={}", cleanupException.getClass().getSimpleName());
        }
    }

    private String objectName(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(publicUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || !(bucket + "." + endpoint).equalsIgnoreCase(uri.getHost())
                    || (uri.getPort() >= 0 && uri.getPort() != 443)
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return null;
            }
            String path = uri.getRawPath();
            return path == null || path.length() <= 1 ? null : path.substring(1);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String objectDirectory(FileUploadPolicy.FileUploadDescriptor descriptor) {
        return switch (descriptor.suffix()) {
            case ".pdf" -> "pdf";
            case ".doc", ".docx" -> "word";
            default -> descriptor.category();
        };
    }

    private OSS requireOssClient() {
        if (ossClient == null) {
            throw new CustomBusinessException("文件存储未配置，请先启用 OSS");
        }
        return ossClient;
    }
}
