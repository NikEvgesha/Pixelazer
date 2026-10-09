package com.pixelazer.core;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Reduces an image while favouring locally contrasting, directionally coherent
 * pixels. Colours are grouped inside every destination cell before a winner is
 * selected; this avoids inventing blurred intermediate colours.
 */
public final class EdgeAwareDownsampler {
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    public DownsampleResult resize(BufferedImage source, int targetWidth, int targetHeight,
                                   double edgeStrength) {
        return resize(source, targetWidth, targetHeight, edgeStrength, 0, 0);
    }

    public DownsampleResult resize(BufferedImage source, int targetWidth, int targetHeight,
                                   double edgeStrength, double offsetX, double offsetY) {
        if (source == null) throw new IllegalArgumentException("Source image is required");
        if (targetWidth < 1 || targetHeight < 1) {
            throw new IllegalArgumentException("Target dimensions must be positive");
        }

        int sw = source.getWidth(), sh = source.getHeight();
        double[] structure = structureMap(source);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        double[] retainedImportance = new double[targetWidth * targetHeight];

        for (int ty = 0; ty < targetHeight; ty++) {
            int y0 = cellStart(ty, sh, targetHeight, offsetY);
            int y1 = cellEnd(ty, sh, targetHeight, offsetY);
            for (int tx = 0; tx < targetWidth; tx++) {
                int x0 = cellStart(tx, sw, targetWidth, offsetX);
                int x1 = cellEnd(tx, sw, targetWidth, offsetX);
                CellChoice choice = chooseCellColour(source, structure, x0, y0,
                        Math.min(x1, sw), Math.min(y1, sh), edgeStrength);
                result.setRGB(tx, ty, choice.argb());
                retainedImportance[ty * targetWidth + tx] = choice.importance();
            }
        }
        return new DownsampleResult(result, retainedImportance);
    }

    private static int cellStart(int index, int sourceSize, int targetSize, double offset) {
        int start = (int) Math.floor((index + offset) * sourceSize / targetSize);
        return clamp(start, 0, sourceSize - 1);
    }

    private static int cellEnd(int index, int sourceSize, int targetSize, double offset) {
        int start = cellStart(index, sourceSize, targetSize, offset);
        int end = (int) Math.ceil((index + 1 + offset) * sourceSize / targetSize);
        return clamp(Math.max(start + 1, end), 1, sourceSize);
    }

    private CellChoice chooseCellColour(BufferedImage image, double[] structure,
                                        int x0, int y0, int x1, int y1, double edgeStrength) {
        Map<Integer, Bucket> buckets = new HashMap<>();
        int width = image.getWidth();
        double totalCoverage = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha == 0) continue;
                double coverage = alpha / 255.0;
                double importance = structure[y * width + x];
                int key = quantizedKey(argb);
                buckets.computeIfAbsent(key, ignored -> new Bucket())
                        .add(argb, coverage, importance);
                totalCoverage += coverage;
            }
        }
        if (buckets.isEmpty()) return new CellChoice(0, 0);

        Bucket winner = null;
        double bestScore = -1;
        for (Bucket bucket : buckets.values()) {
            // Coverage preserves large regions; structure lets narrow, coherent marks
            // compete without allowing a single noisy pixel to dominate a whole cell.
            double fraction = bucket.coverage / Math.max(1e-9, totalCoverage);
            double averageStructure = bucket.structure / Math.max(1e-9, bucket.coverage);
            // Squaring the average distinguishes the actual thin mark from the broad
            // background touching that mark: both contain edge pixels, but the mark
            // has a much higher proportion of structurally important samples.
            double score = fraction + edgeStrength * averageStructure * averageStructure
                    * Math.sqrt(fraction);
            if (score > bestScore) {
                bestScore = score;
                winner = bucket;
            }
        }
        return new CellChoice(winner.averageArgb(), Math.min(1.0,
                winner.structure / Math.max(1e-9, winner.coverage)));
    }

    private double[] structureMap(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        double[] lum = new double[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) lum[y * width + x] = ColorMath.luminance(image.getRGB(x, y));
        }

        double[] result = new double[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double gx = sample(lum, width, height, x + 1, y - 1)
                        + 2 * sample(lum, width, height, x + 1, y)
                        + sample(lum, width, height, x + 1, y + 1)
                        - sample(lum, width, height, x - 1, y - 1)
                        - 2 * sample(lum, width, height, x - 1, y)
                        - sample(lum, width, height, x - 1, y + 1);
                double gy = sample(lum, width, height, x - 1, y + 1)
                        + 2 * sample(lum, width, height, x, y + 1)
                        + sample(lum, width, height, x + 1, y + 1)
                        - sample(lum, width, height, x - 1, y - 1)
                        - 2 * sample(lum, width, height, x, y - 1)
                        - sample(lum, width, height, x + 1, y - 1);
                double sobel = Math.min(1.0, Math.hypot(gx, gy) / 2.5);

                int centre = image.getRGB(x, y);
                double contrast = 0;
                int aligned = 0;
                for (int i = 0; i < DX.length; i++) {
                    int nx = clamp(x + DX[i], 0, width - 1);
                    int ny = clamp(y + DY[i], 0, height - 1);
                    double distance = ColorMath.rgbDistance(centre, image.getRGB(nx, ny));
                    contrast = Math.max(contrast, distance);
                    if (distance > 0.12) aligned++;
                }
                // Local contrast also marks a one-pixel line whose symmetric Sobel
                // derivative can be zero. Repetition in several directions rewards
                // continuous marks more than isolated compression noise.
                double coherence = Math.min(1.0, aligned / 3.0);
                result[y * width + x] = Math.min(1.0,
                        Math.max(sobel, contrast * (0.7 + 0.3 * coherence)));
            }
        }
        return result;
    }

    private static double sample(double[] values, int width, int height, int x, int y) {
        return values[clamp(y, 0, height - 1) * width + clamp(x, 0, width - 1)];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int quantizedKey(int argb) {
        int a = ((argb >>> 24) & 0xff) >>> 5;
        int r = ((argb >>> 16) & 0xff) >>> 4;
        int g = ((argb >>> 8) & 0xff) >>> 4;
        int b = (argb & 0xff) >>> 4;
        return (a << 12) | (r << 8) | (g << 4) | b;
    }

    private record CellChoice(int argb, double importance) {}

    private static final class Bucket {
        double coverage, structure, a, r, g, b;

        void add(int argb, double weight, double importance) {
            coverage += weight;
            structure += weight * importance;
            a += ((argb >>> 24) & 0xff) * weight;
            r += ((argb >>> 16) & 0xff) * weight;
            g += ((argb >>> 8) & 0xff) * weight;
            b += (argb & 0xff) * weight;
        }

        int averageArgb() {
            int aa = (int) Math.round(a / coverage);
            int rr = (int) Math.round(r / coverage);
            int gg = (int) Math.round(g / coverage);
            int bb = (int) Math.round(b / coverage);
            return (aa << 24) | (rr << 16) | (gg << 8) | bb;
        }
    }
}
