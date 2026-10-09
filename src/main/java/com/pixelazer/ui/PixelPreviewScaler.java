package com.pixelazer.ui;

import java.awt.image.BufferedImage;

/** Builds a sharp preview using only integer zoom or integer decimation. */
final class PixelPreviewScaler {
    private PixelPreviewScaler() {}

    static BufferedImage scale(BufferedImage source, int maximumWidth, int maximumHeight) {
        if (source == null) throw new IllegalArgumentException("Source image is required");
        if (maximumWidth < 1 || maximumHeight < 1) {
            throw new IllegalArgumentException("Preview dimensions must be positive");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maximumWidth && height <= maximumHeight) {
            int zoom = Math.max(1, Math.min(maximumWidth / width, maximumHeight / height));
            return replicate(source, zoom);
        }

        int divisor = Math.max(ceilDiv(width, maximumWidth), ceilDiv(height, maximumHeight));
        int targetWidth = ceilDiv(width, divisor);
        int targetHeight = ceilDiv(height, divisor);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                result.setRGB(x, y, source.getRGB(
                        Math.min(width - 1, x * divisor),
                        Math.min(height - 1, y * divisor)));
            }
        }
        return result;
    }

    private static BufferedImage replicate(BufferedImage source, int zoom) {
        BufferedImage result = new BufferedImage(
                source.getWidth() * zoom, source.getHeight() * zoom, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                result.setRGB(x, y, source.getRGB(x / zoom, y / zoom));
            }
        }
        return result;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
