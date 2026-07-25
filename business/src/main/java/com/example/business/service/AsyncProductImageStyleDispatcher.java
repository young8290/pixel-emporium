package com.example.business.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncProductImageStyleDispatcher implements ProductImageStyleDispatcher {
    private final ProductImageStyleService styleService;

    public AsyncProductImageStyleDispatcher(ProductImageStyleService styleService) {
        this.styleService = styleService;
    }

    @Async("pixelStyleExecutor")
    @Override
    public void dispatch(Long imageId, String presetKey) {
        styleService.stylize(imageId, presetKey);
    }
}
