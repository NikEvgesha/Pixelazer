package com.pixelazer.core;

import java.awt.image.BufferedImage;

/** Image plus structural importance retained for palette generation. */
public record DownsampleResult(BufferedImage image, double[] importance) {
    public DownsampleResult {
        if (image == null || importance == null
                || importance.length != image.getWidth() * image.getHeight()) {
            throw new IllegalArgumentException("Importance map must match the image");
        }
    }
}
