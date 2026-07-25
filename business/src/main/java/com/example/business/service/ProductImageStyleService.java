package com.example.business.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.business.client.ImageModelClient;
import com.example.business.client.ImageModelException;
import com.example.business.config.PixelStyleProperties;
import com.example.business.entity.ProductImage;
import com.example.business.mapper.ProductImageMapper;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;

@Service
public class ProductImageStyleService {
    private static final Logger log = LoggerFactory.getLogger(ProductImageStyleService.class);
    private static final long RATE_WINDOW_MILLIS = 60_000L;

    private final ProductImageMapper imageMapper;
    private final RemoteFileFetcher remoteFileFetcher;
    private final ImageModelClient imageModelClient;
    private final ValidatedOssStorage storage;
    private final PixelStyleProperties properties;
    private final Semaphore concurrency;
    private final Deque<Long> requestTimes = new ArrayDeque<>();

    public ProductImageStyleService(ProductImageMapper imageMapper,
                                    RemoteFileFetcher remoteFileFetcher,
                                    ImageModelClient imageModelClient,
                                    ValidatedOssStorage storage,
                                    PixelStyleProperties properties) {
        this.imageMapper = imageMapper;
        this.remoteFileFetcher = remoteFileFetcher;
        this.imageModelClient = imageModelClient;
        this.storage = storage;
        this.properties = properties;
        this.concurrency = new Semaphore(Math.max(1, properties.getMaxConcurrency()), true);
    }

    @Transactional
    public ProductImage prepare(Long imageId, String presetKey) {
        PixelStyleProperties.Preset preset = requireConfigurationAndPreset(presetKey);
        ProductImage image = requireImage(imageId);
        if (image.getStyleStatus() == ProductImage.StyleStatus.PENDING) {
            throw new CustomConflictException("该图片正在转换，请勿重复提交");
        }
        int updated = imageMapper.update(null, new LambdaUpdateWrapper<ProductImage>()
                .set(ProductImage::getStyleStatus, ProductImage.StyleStatus.PENDING)
                .set(ProductImage::getStylePreset, preset.getKey())
                .eq(ProductImage::getId, imageId)
                .ne(ProductImage::getStyleStatus, ProductImage.StyleStatus.PENDING));
        if (updated != 1) {
            throw new CustomConflictException("该图片状态已变化，请稍后重试");
        }
        image.setStyleStatus(ProductImage.StyleStatus.PENDING);
        image.setStylePreset(preset.getKey());
        return image;
    }

    public void stylize(Long imageId, String presetKey) {
        boolean acquired = concurrency.tryAcquire();
        if (!acquired) {
            markFailed(imageId, presetKey, "并发配额已满");
            return;
        }
        try {
            if (!acquireRatePermit()) {
                markFailed(imageId, presetKey, "调用频率已达上限");
                return;
            }
            PixelStyleProperties.Preset preset = requireConfigurationAndPreset(presetKey);
            ProductImage image = requireImage(imageId);
            if (image.getStyleStatus() != ProductImage.StyleStatus.PENDING
                    || !presetKey.equals(image.getStylePreset())) {
                return;
            }

            RemoteFileFetcher.RemoteFilePayload original = remoteFileFetcher.fetch(parseUri(image.getOriginalUrl()));
            ImageModelClient.ImageModelResult result = imageModelClient.imageToImage(
                    new ImageModelClient.ImageModelRequest(original.bytes(), original.contentType(),
                            preset.getPrompt(), preset.getNegative(), preset.getStrength(), preset.getSize()));
            ResolvedImage resolved = resolveResult(result);
            Long ownerId = image.getCreatedBy() == null ? image.getProductId() : image.getCreatedBy();
            String styledUrl = storage.storeStyled(ownerId, resolved.filename(),
                    resolved.contentType(), resolved.bytes());
            image.setStyledUrl(styledUrl);
            image.setStyleStatus(ProductImage.StyleStatus.DONE);
            image.setStylePreset(preset.getKey());
            imageMapper.updateById(image);
        } catch (Exception exception) {
            markFailed(imageId, presetKey, exception.getClass().getSimpleName());
        } finally {
            concurrency.release();
        }
    }

    public void markFailed(Long imageId, String presetKey, String reason) {
        ProductImage image = imageMapper.selectById(imageId);
        if (image == null || image.getStyleStatus() != ProductImage.StyleStatus.PENDING
                || !presetKey.equals(image.getStylePreset())) {
            return;
        }
        image.setStyleStatus(ProductImage.StyleStatus.FAILED);
        imageMapper.updateById(image);
        log.warn("商品图风格转换失败，图片ID={}，原因类型={}", imageId, reason);
    }

    private PixelStyleProperties.Preset requireConfigurationAndPreset(String presetKey) {
        if (!properties.isEnabled()) {
            throw new CustomBusinessException("商品图风格转换未启用");
        }
        if (!StringUtils.hasText(properties.getApi().getKey())) {
            throw new CustomBusinessException("商品图风格转换未启用：未配置 API Key");
        }
        if (!StringUtils.hasText(properties.getApi().getBaseUrl())) {
            throw new CustomBusinessException("商品图风格转换未启用：未配置模型地址");
        }
        if (!StringUtils.hasText(presetKey)) {
            throw new CustomValidationException("风格预设不能为空");
        }
        PixelStyleProperties.Preset preset = properties.findPreset(presetKey);
        if (preset == null) {
            throw new CustomValidationException("风格预设不存在");
        }
        return preset;
    }

    private ProductImage requireImage(Long imageId) {
        ProductImage image = imageId == null ? null : imageMapper.selectById(imageId);
        if (image == null) {
            throw new NoSuchElementException("商品图片不存在");
        }
        return image;
    }

    private ResolvedImage resolveResult(ImageModelClient.ImageModelResult result) {
        if (result == null) {
            throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型未返回图片");
        }
        if (result.hasBytes()) {
            String contentType = StringUtils.hasText(result.contentType()) ? result.contentType() : "image/png";
            String filename = StringUtils.hasText(result.filename())
                    ? result.filename() : "styled" + suffixFor(contentType);
            return new ResolvedImage(result.bytes(), contentType, filename);
        }
        if (result.hasUrl()) {
            RemoteFileFetcher.RemoteFilePayload payload = remoteFileFetcher.fetch(result.url());
            String filename = StringUtils.hasText(payload.filename())
                    ? payload.filename() : "styled" + suffixFor(payload.contentType());
            return new ResolvedImage(payload.bytes(), payload.contentType(), filename);
        }
        throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型未返回图片");
    }

    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (RuntimeException exception) {
            throw new CustomValidationException("原图地址无效");
        }
    }

    private String suffixFor(String contentType) {
        if (contentType == null) {
            return ".png";
        }
        return switch (contentType.split(";", 2)[0].trim().toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".png";
        };
    }

    private synchronized boolean acquireRatePermit() {
        int limit = properties.getRate();
        if (limit <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        while (!requestTimes.isEmpty() && now - requestTimes.getFirst() >= RATE_WINDOW_MILLIS) {
            requestTimes.removeFirst();
        }
        if (requestTimes.size() >= limit) {
            return false;
        }
        requestTimes.addLast(now);
        return true;
    }

    private record ResolvedImage(byte[] bytes, String contentType, String filename) {
    }
}
