package com.pixelazer.core;

import java.awt.image.BufferedImage;

/** Chooses a grid phase that places strong narrow details near destination pixel centres. */
public final class PixelGridAligner {
    private static final double[] CANDIDATES = {0.0, -0.2, 0.2, -0.4, 0.4};

    public Offset findBestOffset(BufferedImage image, int targetWidth, int targetHeight) {
        int sampleStep = Math.max(1, (int) Math.sqrt(
                image.getWidth() * (double) image.getHeight() / 200_000.0));
        double bestX = 0, bestY = 0;
        double bestXScore = -1, bestYScore = -1;
        double scaleX = image.getWidth() / (double) targetWidth;
        double scaleY = image.getHeight() / (double) targetHeight;

        for (double candidate : CANDIDATES) {
            double scoreX = 0, scoreY = 0;
            for (int y = sampleStep; y < image.getHeight() - sampleStep; y += sampleStep) {
                for (int x = sampleStep; x < image.getWidth() - sampleStep; x += sampleStep) {
                    int centre = image.getRGB(x, y);
                    double horizontalContrast = Math.max(
                            ColorMath.rgbDistance(centre, image.getRGB(x - sampleStep, y)),
                            ColorMath.rgbDistance(centre, image.getRGB(x + sampleStep, y)));
                    double verticalContrast = Math.max(
                            ColorMath.rgbDistance(centre, image.getRGB(x, y - sampleStep)),
                            ColorMath.rgbDistance(centre, image.getRGB(x, y + sampleStep)));
                    scoreX += horizontalContrast * centreCloseness(x / scaleX - candidate);
                    scoreY += verticalContrast * centreCloseness(y / scaleY - candidate);
                }
            }
            if (scoreX > bestXScore) { bestXScore = scoreX; bestX = candidate; }
            if (scoreY > bestYScore) { bestYScore = scoreY; bestY = candidate; }
        }
        return new Offset(bestX, bestY);
    }

    private static double centreCloseness(double coordinate) {
        double fraction = coordinate - Math.floor(coordinate);
        return 1.0 - 2.0 * Math.abs(fraction - 0.5);
    }

    public record Offset(double x, double y) {}
}
