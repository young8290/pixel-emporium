package com.example.business.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pixel-style")
public class PixelStyleProperties {
    private boolean enabled;
    private Api api = new Api();
    private int timeoutMs = 30_000;
    private int maxConcurrency = 2;
    private int rate = 10;
    private int maxRetries = 2;
    private boolean keepOriginal = true;
    private List<Preset> presets = defaultPresets();

    public Preset findPreset(String key) {
        if (key == null) {
            return null;
        }
        return presets.stream().filter(preset -> key.equals(preset.getKey())).findFirst().orElse(null);
    }

    @Getter
    @Setter
    public static class Api {
        private String baseUrl;
        private String key;
        private String model;
        private String authHeader = "Authorization";
        private String authScheme = "Bearer";
        private String imageField = "image";
        private String promptField = "prompt";
        private String negativeField = "negative_prompt";
        private String strengthField = "strength";
        private String sizeField = "size";
        private String modelField = "model";
        private String responseUrlField = "data.0.url";
        private String responseBase64Field = "data.0.b64_json";
        private String responseContentTypeField = "data.0.content_type";
    }

    @Getter
    @Setter
    public static class Preset {
        private String key;
        private String name;
        private String prompt;
        private String negative;
        private double strength;
        private int size;

        public Preset() {
        }

        public Preset(String key, String name, String prompt, String negative, double strength, int size) {
            this.key = key;
            this.name = name;
            this.prompt = prompt;
            this.negative = negative;
            this.strength = strength;
            this.size = size;
        }
    }

    private static List<Preset> defaultPresets() {
        String prompt = "Repaint this product photo into cozy Stardew-Valley-inspired pixel art, keeping the product clearly recognizable. "
                + "Stardew-Valley-inspired cozy pixel art, warm cozy palette (parchment cream #F3E7C9, grass-green #5F9E43, "
                + "orange-wood #A2652F, harvest-gold #E6A62B), crisp 1px pixel edges, consistent pixel density, flat 2D, "
                + "soft top-left light, gentle dithering, limited palette. Preserve the product's real shape, proportions and main colors; "
                + "simplify surfaces into clean pixel clusters; place on a soft cream or transparent background, centered, single subject. "
                + "In the cozy pixel style of Stardew Valley.";
        String negative = "no photorealism, no 3D render, no blur, no anti-aliased vector look, no heavy gradients, no neon, "
                + "no extra objects, no busy background, no text or logo, no watermark, no distorted or unrecognizable product, "
                + "no changed product identity, no added food or produce props, do not turn the product into groceries, "
                + "keep the original product category.";
        List<Preset> result = new ArrayList<>();
        result.add(new Preset("stardew-soft", "星露谷·柔和", prompt, negative, 0.42, 512));
        result.add(new Preset("stardew-strong", "星露谷·浓郁", prompt
                + " chunkier pixel blocks, stronger stylization, bolder outline, more pronounced dithering.",
                negative + " not too abstract, keep the product silhouette readable.", 0.58, 512));
        result.add(new Preset("stardew-lineart", "星露谷·描边", prompt
                + " add a subtle dark-wood 1px outline around the subject, slightly flatter shading, like a Stardew inventory item icon.",
                negative, 0.50, 512));
        return result;
    }
}
