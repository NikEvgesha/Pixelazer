package com.pixelazer.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Downsamples a labelled colour-region map and protects a coherent outline layer. */
public final class IllustrationDownsampler {
    private final WeightedMedianCut quantizer = new WeightedMedianCut();

    public Result resize(BufferedImage source, int targetWidth, int targetHeight, int paletteSize) {
        return resize(source, targetWidth, targetHeight, paletteSize, DetailLevel.BALANCED,
                OutlineSelection.AUTO, 0xff000000, OutlineThickness.NORMAL, true, false,
                List.of(), 0, 0);
    }

    public Result resize(BufferedImage source, int targetWidth, int targetHeight, int paletteSize,
                         DetailLevel detailLevel, OutlineSelection outlineSelection,
                         int manualOutlineArgb, OutlineThickness outlineThickness,
                         boolean closeOutlineGaps, boolean lineArtMode,
                         List<Integer> lockedPaletteColours, double offsetX, double offsetY) {
        List<Integer> palette = quantizer.createPalette(source, null, paletteSize, lockedPaletteColours);
        double[][] paletteLab = palette.stream().map(ColorMath::lab).toArray(double[][]::new);
        int width = source.getWidth(), height = source.getHeight();
        int[] labels = new int[width * height];
        int[] usage = new int[palette.size()];
        Arrays.fill(labels, -1);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                if (((argb >>> 24) & 0xff) < 24) continue;
                int label = nearest(ColorMath.lab(argb), paletteLab);
                labels[y * width + x] = label;
                usage[label]++;
            }
        }

        int outline = selectOutlineLabel(labels, width, height, palette, usage,
                outlineSelection, manualOutlineArgb);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        double[] importance = new double[targetWidth * targetHeight];

        for (int ty = 0; ty < targetHeight; ty++) {
            int y0 = cellStart(ty, height, targetHeight, offsetY);
            int y1 = cellEnd(ty, height, targetHeight, offsetY);
            for (int tx = 0; tx < targetWidth; tx++) {
                int x0 = cellStart(tx, width, targetWidth, offsetX);
                int x1 = cellEnd(tx, width, targetWidth, offsetX);
                int[] counts = new int[palette.size()];
                int transparent = 0, coherentOutline = 0, boundaryPixels = 0;

                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int label = labels[y * width + x];
                        if (label < 0) transparent++;
                        else counts[label]++;
                        if (label == outline && isCoherent(labels, width, height, x, y, outline)) {
                            coherentOutline++;
                        }
                        if (label >= 0 && isBoundary(labels, width, height, x, y, label)) boundaryPixels++;
                    }
                }

                int chosen = largest(counts);
                int cellWidth = x1 - x0, cellHeight = y1 - y0;
                double thresholdFactor = detailLevel.lineThresholdFactor() * (lineArtMode ? 0.72 : 1.0);
                int lineThreshold = Math.max(1, (int) Math.ceil(
                        Math.min(cellWidth, cellHeight) * thresholdFactor));
                boolean protectedLine = outline >= 0 && coherentOutline >= lineThreshold;
                if (protectedLine) chosen = outline;
                else if (transparent > counts[chosen]) chosen = -1;

                output.setRGB(tx, ty, chosen < 0 ? 0 : palette.get(chosen));
                int area = cellWidth * cellHeight;
                importance[ty * targetWidth + tx] = protectedLine ? 1.0
                        : Math.min(0.34, boundaryPixels / (double) Math.max(1, area));
            }
        }
        if (outline >= 0) {
            int outlineColour = palette.get(outline);
            output = postProcessOutline(output, outlineColour, outlineThickness,
                    closeOutlineGaps || lineArtMode);
            for (int y = 0; y < targetHeight; y++) {
                for (int x = 0; x < targetWidth; x++) {
                    if (output.getRGB(x, y) == outlineColour) {
                        importance[y * targetWidth + x] = 1.0;
                    }
                }
            }
        }
        return new Result(new DownsampleResult(output, importance), usedPalette(output, palette));
    }

    private static int cellStart(int index, int sourceSize, int targetSize, double offset) {
        return Math.max(0, Math.min(sourceSize - 1,
                (int) Math.floor((index + offset) * sourceSize / targetSize)));
    }

    private static int cellEnd(int index, int sourceSize, int targetSize, double offset) {
        int start = cellStart(index, sourceSize, targetSize, offset);
        int end = (int) Math.ceil((index + 1 + offset) * sourceSize / targetSize);
        return Math.max(1, Math.min(sourceSize, Math.max(start + 1, end)));
    }

    private static int selectOutlineLabel(int[] labels, int width, int height,
                                          List<Integer> palette, int[] usage,
                                          OutlineSelection selection, int manualArgb) {
        if (selection == OutlineSelection.DARKEST) {
            int darkest = 0;
            for (int i = 1; i < palette.size(); i++) {
                if (ColorMath.luminance(palette.get(i)) < ColorMath.luminance(palette.get(darkest))) {
                    darkest = i;
                }
            }
            return darkest;
        }
        if (selection == OutlineSelection.MANUAL) {
            double[] selectedLab = ColorMath.lab(manualArgb);
            int nearest = 0;
            double distance = Double.MAX_VALUE;
            for (int i = 0; i < palette.size(); i++) {
                double candidate = ColorMath.labDistance(selectedLab, ColorMath.lab(palette.get(i)));
                if (candidate < distance) {
                    distance = candidate;
                    nearest = i;
                }
            }
            return nearest;
        }
        int[] boundaries = new int[palette.size()];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = labels[y * width + x];
                if (label >= 0 && isBoundary(labels, width, height, x, y, label)) boundaries[label]++;
            }
        }
        int opaquePixels = Arrays.stream(usage).sum();
        int best = -1;
        double bestScore = 0.42;
        for (int i = 0; i < palette.size(); i++) {
            if (usage[i] == 0) continue;
            double darkness = 1.0 - ColorMath.luminance(palette.get(i));
            double boundaryRatio = boundaries[i] / (double) usage[i];
            double coverage = usage[i] / (double) Math.max(1, opaquePixels);
            double largeRegionPenalty = Math.max(0, coverage - 0.35) * 0.8;
            double score = darkness * 0.68 + boundaryRatio * 0.32 - largeRegionPenalty;
            if (darkness > 0.38 && score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static BufferedImage postProcessOutline(BufferedImage source, int outline,
                                                     OutlineThickness thickness, boolean closeGaps) {
        BufferedImage current = copy(source);
        int width = source.getWidth(), height = source.getHeight();
        if (closeGaps) {
            BufferedImage closed = copy(current);
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (current.getRGB(x, y) == outline) continue;
                    boolean horizontal = current.getRGB(x - 1, y) == outline
                            && current.getRGB(x + 1, y) == outline;
                    boolean vertical = current.getRGB(x, y - 1) == outline
                            && current.getRGB(x, y + 1) == outline;
                    boolean diagonal = current.getRGB(x - 1, y - 1) == outline
                            && current.getRGB(x + 1, y + 1) == outline
                            || current.getRGB(x + 1, y - 1) == outline
                            && current.getRGB(x - 1, y + 1) == outline;
                    if (horizontal || vertical || diagonal) closed.setRGB(x, y, outline);
                }
            }
            current = closed;
        }

        if (thickness == OutlineThickness.THICK) {
            BufferedImage thickened = copy(current);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (current.getRGB(x, y) != outline) continue;
                    if (x > 0) thickened.setRGB(x - 1, y, outline);
                    if (x + 1 < width) thickened.setRGB(x + 1, y, outline);
                    if (y > 0) thickened.setRGB(x, y - 1, outline);
                    if (y + 1 < height) thickened.setRGB(x, y + 1, outline);
                }
            }
            current = thickened;
        } else if (thickness == OutlineThickness.THIN) {
            BufferedImage thinned = copy(current);
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (current.getRGB(x, y) != outline) continue;
                    boolean horizontal = current.getRGB(x - 1, y) == outline
                            && current.getRGB(x + 1, y) == outline;
                    boolean vertical = current.getRGB(x, y - 1) == outline
                            && current.getRGB(x, y + 1) == outline;
                    if (horizontal && vertical) {
                        int replacement = nearestNonOutline(current, x, y, outline);
                        if (replacement != outline) thinned.setRGB(x, y, replacement);
                    }
                }
            }
            current = thinned;
        }
        return current;
    }

    private static int nearestNonOutline(BufferedImage image, int x, int y, int outline) {
        for (int radius = 1; radius <= 2; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || nx >= image.getWidth() || ny < 0 || ny >= image.getHeight()) continue;
                    int colour = image.getRGB(nx, ny);
                    if (colour != outline) return colour;
                }
            }
        }
        return outline;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, source.getWidth(), source.getHeight(),
                source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()),
                0, source.getWidth());
        return copy;
    }

    private static boolean isCoherent(int[] labels, int width, int height,
                                      int x, int y, int label) {
        return same(labels, width, height, x - 1, y, label)
                || same(labels, width, height, x + 1, y, label)
                || same(labels, width, height, x, y - 1, label)
                || same(labels, width, height, x, y + 1, label)
                || same(labels, width, height, x - 1, y - 1, label)
                || same(labels, width, height, x + 1, y + 1, label)
                || same(labels, width, height, x + 1, y - 1, label)
                || same(labels, width, height, x - 1, y + 1, label);
    }

    private static boolean isBoundary(int[] labels, int width, int height,
                                      int x, int y, int label) {
        return !same(labels, width, height, x - 1, y, label)
                || !same(labels, width, height, x + 1, y, label)
                || !same(labels, width, height, x, y - 1, label)
                || !same(labels, width, height, x, y + 1, label);
    }

    private static boolean same(int[] labels, int width, int height, int x, int y, int label) {
        return x >= 0 && x < width && y >= 0 && y < height && labels[y * width + x] == label;
    }

    private static int nearest(double[] colour, double[][] palette) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            double distance = ColorMath.labDistance(colour, palette[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static int largest(int[] counts) {
        int best = 0;
        for (int i = 1; i < counts.length; i++) if (counts[i] > counts[best]) best = i;
        return best;
    }

    private static List<Integer> usedPalette(BufferedImage image, List<Integer> palette) {
        List<Integer> used = new ArrayList<>();
        for (int colour : palette) {
            boolean found = false;
            for (int y = 0; y < image.getHeight() && !found; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (image.getRGB(x, y) == colour) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) used.add(colour);
        }
        return used;
    }

    public record Result(DownsampleResult downsampled, List<Integer> palette) {}
}
