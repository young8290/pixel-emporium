package com.example.business;

import com.example.business.client.ImageModelClient;
import com.example.business.config.PixelStyleProperties;
import com.example.business.entity.ProductImage;
import com.example.business.mapper.ProductImageMapper;
import com.example.business.service.ProductImageStyleService;
import com.example.business.service.ValidatedOssStorage;
import com.example.business.service.RemoteFileFetcher;
import com.example.common.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageStyleServiceTest {
    @Mock
    private ProductImageMapper imageMapper;
    @Mock
    private RemoteFileFetcher remoteFileFetcher;
    @Mock
    private ImageModelClient imageModelClient;
    @Mock
    private ValidatedOssStorage storage;

    private PixelStyleProperties properties;
    private ProductImageStyleService service;
    private ProductImage image;

    @BeforeEach
    void setUp() {
        properties = new PixelStyleProperties();
        properties.setEnabled(true);
        properties.getApi().setBaseUrl("https://model.example.test/v1");
        properties.getApi().setKey("test-key");
        image = new ProductImage();
        image.setId(9L);
        image.setProductId(7L);
        image.setOriginalUrl("https://bucket.oss.example.com/7/image/original.png");
        image.setCreatedBy(88L);
        image.setStyleStatus(ProductImage.StyleStatus.PENDING);
        image.setStylePreset("stardew-soft");
        lenient().when(imageMapper.selectById(9L)).thenReturn(image);
        service = new ProductImageStyleService(imageMapper, remoteFileFetcher, imageModelClient,
                storage, properties);
    }

    @Test
    void successfulStylizeStoresValidatedResultAndKeepsOriginal() {
        byte[] original = pngBytes();
        byte[] styled = pngBytes();
        when(remoteFileFetcher.fetch(URI.create(image.getOriginalUrl())))
                .thenReturn(new RemoteFileFetcher.RemoteFilePayload(
                        URI.create(image.getOriginalUrl()), "image/png", "original.png", original));
        when(imageModelClient.imageToImage(any())).thenReturn(
                new ImageModelClient.ImageModelResult(styled, "image/png", null, "styled.png"));
        when(storage.storeStyled(anyLong(), anyString(), anyString(), any(byte[].class)))
                .thenReturn("https://bucket.oss.example.com/88/styled/styled.png");

        service.stylize(9L, "stardew-soft");

        ArgumentCaptor<ProductImage> saved = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageMapper).updateById(saved.capture());
        assertEquals(ProductImage.StyleStatus.DONE, saved.getValue().getStyleStatus());
        assertEquals("stardew-soft", saved.getValue().getStylePreset());
        assertEquals("https://bucket.oss.example.com/88/styled/styled.png", saved.getValue().getStyledUrl());
        assertEquals(image.getOriginalUrl(), saved.getValue().getOriginalUrl());
        verify(storage).storeStyled(88L, "styled.png", "image/png", styled);
    }

    @Test
    void failedStylizeMarksFailedAndDoesNotEraseOriginal() {
        when(remoteFileFetcher.fetch(any(URI.class))).thenThrow(new RuntimeException("timeout"));

        service.stylize(9L, "stardew-soft");

        ArgumentCaptor<ProductImage> saved = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageMapper).updateById(saved.capture());
        assertEquals(ProductImage.StyleStatus.FAILED, saved.getValue().getStyleStatus());
        assertEquals(image.getOriginalUrl(), saved.getValue().getOriginalUrl());
        assertTrue(saved.getValue().getStyledUrl() == null);
        verify(imageModelClient, never()).imageToImage(any());
    }

    @Test
    void disabledConfigurationRejectsBeforeCallingModel() {
        properties.setEnabled(false);

        assertThrows(CustomBusinessException.class, () -> service.prepare(9L, "stardew-soft"));

        verify(imageModelClient, never()).imageToImage(any());
    }

    @Test
    void unknownPresetIsRejected() {
        assertThrows(com.example.common.exception.CustomValidationException.class,
                () -> service.prepare(9L, "unknown"));
        verify(imageMapper, never()).update(any());
    }

    @Test
    void defaultPresetKeepsOriginalCategoryAndAvoidsProduceProps() {
        PixelStyleProperties.Preset preset = new PixelStyleProperties().findPreset("stardew-soft");

        assertTrue(preset.getPrompt().contains("warm cozy palette"));
        assertTrue(preset.getNegative().contains("no added food or produce props"));
        assertTrue(preset.getNegative().contains("keep the original product category"));
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0};
    }
}
