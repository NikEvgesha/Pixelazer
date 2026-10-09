package com.pixelazer.core;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/** Lightweight content classifier used by AUTO mode. */
public final class ImageTypeDetector {
    public ProcessingMode detect(BufferedImage image) {
        int step = Math.max(1, (int) Math.sqrt(
                image.getWidth() * (double) image.getHeight() / 25_000.0));
        int comparisons = 0, flat = 0, textured = 0;
        Set<Integer> colourBuckets = new HashSet<>();

        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int colour = image.getRGB(x, y);
                if (((colour >>> 24) & 0xff) < 16) continue;
                colourBuckets.add(bucket(colour));
                if (x + step < image.getWidth()) {
                    double distance = ColorMath.rgbDistance(colour, image.getRGB(x + step, y));
                    comparisons++;
                    if (distance < 0.035) flat++;
                    else if (distance > 0.12) textured++;
                }
                if (y + step < image.getHeight()) {
                    double distance = ColorMath.rgbDistance(colour, image.getRGB(x, y + step));
                    comparisons++;
                    if (distance < 0.035) flat++;
                    else if (distance > 0.12) textured++;
                }
            }
        }
        if (comparisons == 0) return ProcessingMode.ILLUSTRATION;
        double flatRatio = flat / (double) comparisons;
        double strongChangeRatio = textured / (double) comparisons;
        // Flat interiors separated by occasional strong transitions are typical of
        // logos, cartoons and line drawings. Photographs contain many weak gradients.
        return flatRatio >= 0.48
                && (colourBuckets.size() <= 384 || strongChangeRatio >= 0.08)
                ? ProcessingMode.ILLUSTRATION : ProcessingMode.PHOTO;
    }

    private static int bucket(int argb) {
        return (((argb >>> 19) & 0x1f) << 10)
                | (((argb >>> 11) & 0x1f) << 5)
                | ((argb >>> 3) & 0x1f);
    }
}
