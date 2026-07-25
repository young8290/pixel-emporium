package com.example.business.controller.admin;

import com.aliyun.oss.OSS;
import com.example.business.annotation.OperationLog;
import com.example.business.config.UploadSecurityProperties;
import com.example.business.service.FileUploadPolicy;
import com.example.business.service.RemoteFileFetcher;
import com.example.business.service.ValidatedOssStorage;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomValidationException;
import com.example.common.exception.ErrorCode;
import com.example.common.response.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("admin/file")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class FileAdminController {
    private static final Logger log = LoggerFactory.getLogger(FileAdminController.class);

    private final ValidatedOssStorage validatedStorage;
    private final RemoteFileFetcher remoteFileFetcher;
    private final UploadSecurityProperties uploadSecurityProperties;
    private final String bucket;
    private final String endpoint;

    @Autowired
    public FileAdminController(ValidatedOssStorage validatedStorage,
                               RemoteFileFetcher remoteFileFetcher,
                               UploadSecurityProperties uploadSecurityProperties,
                               @Value("${alibaba.cloud.oss.bucket}") String bucket,
                               @Value("${alibaba.cloud.oss.endpoint}") String endpoint) {
        this.validatedStorage = validatedStorage;
        this.remoteFileFetcher = remoteFileFetcher;
        this.uploadSecurityProperties = uploadSecurityProperties;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    public FileAdminController(OSS ossClient, FileUploadPolicy uploadPolicy,
                               RemoteFileFetcher remoteFileFetcher,
                               UploadSecurityProperties uploadSecurityProperties,
                               String bucket, String endpoint) {
        this(new ValidatedOssStorage(ossClient, uploadPolicy, bucket, endpoint),
                remoteFileFetcher, uploadSecurityProperties, bucket, endpoint);
    }

    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(action = "FILE_UPLOAD", targetType = "FILE")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = readMultipart(file);
            return R.ok(putValidated(file.getOriginalFilename(), file.getContentType(), bytes));
        } catch (CustomValidationException exception) {
            return validationFailure(exception);
        } catch (Exception exception) {
            log.warn("文件上传失败，类型={}", exception.getClass().getSimpleName());
            return R.fail("上传失败");
        }
    }

    @PostMapping("uploadByUrl")
    @OperationLog(action = "FILE_REMOTE_UPLOAD", targetType = "REMOTE_FILE")
    public R<Map<String, String>> uploadByUrl(@RequestParam("url") String url) {
        try {
            URI source = parseUri(url);
            RemoteFileFetcher.RemoteFilePayload payload = remoteFileFetcher.fetch(source);
            String ossUrl = putValidated(remoteFilename(payload), payload.contentType(), payload.bytes());
            Map<String, String> data = new HashMap<>();
            data.put("originalURL", url);
            data.put("url", ossUrl);
            return R.ok(data);
        } catch (CustomValidationException exception) {
            return validationFailure(exception);
        } catch (Exception exception) {
            log.warn("外链图片转存失败，类型={}", exception.getClass().getSimpleName());
            return R.fail("外链图片转存失败");
        }
    }

    @PostMapping("uploadFromVditor")
    @OperationLog(action = "EDITOR_FILE_UPLOAD", targetType = "FILE")
    public Map<String, Object> uploadFromVditor(@RequestParam("file[]") MultipartFile[] files) {
        List<String> errorFiles = new ArrayList<>();
        Map<String, String> imageMap = new HashMap<>();

        if (files == null) {
            files = new MultipartFile[0];
        }
        for (MultipartFile file : files) {
            String originalFilename = file == null ? null : file.getOriginalFilename();
            String filePath = null;
            if (file != null && !file.isEmpty()) {
                try {
                    byte[] bytes = readMultipart(file);
                    filePath = putValidated(originalFilename, file.getContentType(), bytes);
                } catch (CustomValidationException exception) {
                    log.info("Vditor 文件校验失败，类型={}", exception.getClass().getSimpleName());
                    errorFiles.add(originalFilename);
                } catch (Exception exception) {
                    log.warn("Vditor 文件上传失败，类型={}", exception.getClass().getSimpleName());
                    errorFiles.add(originalFilename);
                }
            } else if (originalFilename != null) {
                errorFiles.add(originalFilename);
            }
            imageMap.put(originalFilename, filePath);
        }

        Data data = new Data();
        data.setErrFiles(errorFiles);
        data.setSuccMap(imageMap);

        Map<String, Object> result = new HashMap<>();
        if (errorFiles.isEmpty()) {
            result.put("msg", "success");
            result.put("code", 0);
        } else {
            result.put("msg", "部分文件上传失败，请检查文件类型和大小");
            result.put("code", -1);
        }
        result.put("data", data);
        return result;
    }

    @PostMapping("uploadOutsideLinkFromVditor")
    @OperationLog(action = "EDITOR_REMOTE_FILE_UPLOAD", targetType = "REMOTE_FILE")
    public Map<String, Object> uploadOutsideLinkFromVditor(@RequestBody Map<String, String> body,
                                                            HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (body == null || body.get("url") == null || body.get("url").isBlank()) {
            result.put("msg", "无效的 url");
            result.put("code", -1);
            result.put("data", null);
            return result;
        }

        String url = body.get("url");
        Map<String, String> data = new HashMap<>();
        try {
            URI source = parseUri(url);
            validateVditorUri(source);
            if (!isOutsideLink(request, source)) {
                result.put("msg", "无需额外处理，直接返回原图地址");
                result.put("code", 0);
                data.put("originalURL", url);
                data.put("url", url);
                result.put("data", data);
                return result;
            }

            RemoteFileFetcher.RemoteFilePayload payload = remoteFileFetcher.fetch(source);
            String ossUrl = putValidated(remoteFilename(payload), payload.contentType(), payload.bytes());
            result.put("msg", "外部图片链接已成功上传到服务器");
            result.put("code", 0);
            data.put("originalURL", url);
            data.put("url", ossUrl);
            result.put("data", data);
        } catch (CustomValidationException exception) {
            result.put("code", -1);
            result.put("msg", "远程地址或文件不符合上传安全策略");
            data.put("originalURL", url);
            data.put("url", url);
            result.put("data", data);
        } catch (Exception exception) {
            result.put("code", -1);
            result.put("msg", "图片处理异常");
            data.put("originalURL", url);
            data.put("url", url);
            result.put("data", data);
            log.warn("Vditor 外链转存失败，类型={}", exception.getClass().getSimpleName());
        }
        return result;
    }

    private byte[] readMultipart(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CustomValidationException("文件不能为空");
        }
        if (file.getSize() > maxMultipartBytes()) {
            throw new CustomValidationException("文件大小超过限制");
        }
        return file.getBytes();
    }

    private long maxMultipartBytes() {
        return Math.max(uploadSecurityProperties.getImageMaxBytes(),
                Math.max(uploadSecurityProperties.getVideoMaxBytes(), uploadSecurityProperties.getDocumentMaxBytes()));
    }

    private String putValidated(String filename, String contentType, byte[] bytes) throws Exception {
        return validatedStorage.storeDefault(filename, contentType, bytes);
    }

    private String remoteFilename(RemoteFileFetcher.RemoteFilePayload payload) {
        String filename = payload.filename();
        if (filename != null && hasExtension(filename)) {
            return filename;
        }
        String suffix = suffixForContentType(payload.contentType());
        return suffix == null ? filename : (filename == null || filename.isBlank() ? "download" : filename) + suffix;
    }

    private boolean hasExtension(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        int dot = filename.lastIndexOf('.');
        return dot > slash && dot < filename.length() - 1;
    }

    private String suffixForContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "video/mp4" -> ".mp4";
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> null;
        };
    }

    private URI parseUri(String url) {
        if (url == null || url.isBlank()) {
            throw new CustomValidationException("远程地址无效");
        }
        try {
            return URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new CustomValidationException("远程地址无效");
        }
    }

    private boolean isOutsideLink(HttpServletRequest request, URI source) {
        if (request != null && sameOrigin(request, source)) {
            return false;
        }
        return !sameOssOrigin(source);
    }

    private boolean sameOrigin(HttpServletRequest request, URI source) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        int sourcePort = source.getPort() >= 0 ? source.getPort() : defaultPort(source.getScheme());
        int requestPort = port >= 0 ? port : defaultPort(scheme);
        return scheme != null && host != null
                && scheme.equalsIgnoreCase(source.getScheme())
                && host.equalsIgnoreCase(source.getHost())
                && requestPort == sourcePort;
    }

    private boolean sameOssOrigin(URI source) {
        String ossHost = bucket + "." + endpoint;
        return source.getUserInfo() == null
                && "https".equalsIgnoreCase(source.getScheme())
                && ossHost.equalsIgnoreCase(source.getHost())
                && (source.getPort() < 0 || source.getPort() == 443);
    }

    private void validateVditorUri(URI source) {
        if (source == null || !source.isAbsolute() || source.getUserInfo() != null
                || source.getHost() == null
                || !("http".equalsIgnoreCase(source.getScheme()) || "https".equalsIgnoreCase(source.getScheme()))) {
            throw new CustomValidationException("远程地址无效");
        }
    }

    private int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private <T> R<T> validationFailure(CustomValidationException exception) {
        return R.fail(ErrorCode.VALIDATION_FAILED, exception.getMessage());
    }

    @Setter
    @Getter
    private static class Data {
        private List<String> errFiles;
        private Map<String, String> succMap;
    }
}
