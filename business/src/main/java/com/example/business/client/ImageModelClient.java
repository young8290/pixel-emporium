package com.example.business.client;

import java.net.URI;

public interface ImageModelClient {
    ImageModelResult imageToImage(ImageModelRequest request);

    record ImageModelRequest(byte[] imageBytes, String contentType, String prompt,
                             String negativePrompt, double strength, int size) {
    }

    record ImageModelResult(byte[] bytes, String contentType, URI url, String filename) {
        public boolean hasBytes() {
            return bytes != null && bytes.length > 0;
        }

        public boolean hasUrl() {
            return url != null;
        }
    }
}
