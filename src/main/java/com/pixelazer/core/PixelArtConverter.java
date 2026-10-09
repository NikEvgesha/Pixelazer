package com.pixelazer.core;

import java.awt.image.BufferedImage;
import java.util.List;

public final class PixelArtConverter {
    private final EdgeAwareDownsampler downsampler = new EdgeAwareDownsampler();
    private final IllustrationDownsampler illustrationDownsampler = new IllustrationDownsampler();
    private final WeightedMedianCut quantizer = new WeightedMedianCut();
    private final ImageTypeDetector typeDetector = new ImageTypeDetector();
    private final PixelGridAligner gridAligner = new PixelGridAligner();

    public ConversionResult convert(BufferedImage source, ProcessingOptions options) {
        ProcessingMode resolvedMode = options.mode() == ProcessingMode.AUTO
                ? typeDetector.detect(source) : options.mode();
        PixelGridAligner.Offset gridOffset = switch (options.gridAlignmentMode()) {
            case AUTO -> gridAligner.findBestOffset(source, options.width(), options.height());
            case CENTERED -> new PixelGridAligner.Offset(0, 0);
            case MANUAL -> new PixelGridAligner.Offset(options.gridOffsetX(), options.gridOffsetY());
        };
        DownsampleResult reduced;
        List<Integer> palette;
        BufferedImage mapped;
        if (resolvedMode == ProcessingMode.ILLUSTRATION || resolvedMode == ProcessingMode.LINE_ART) {
            boolean lineArt = resolvedMode == ProcessingMode.LINE_ART;
            IllustrationDownsampler.Result illustration = illustrationDownsampler.resize(
                    source, options.width(), options.height(), options.paletteSize(),
                    lineArt ? options.detailLevel() : DetailLevel.BALANCED,
                    lineArt ? options.outlineSelection() : OutlineSelection.AUTO,
                    lineArt ? options.manualOutlineArgb() : 0xff000000,
                    lineArt ? options.outlineThickness() : OutlineThickness.NORMAL,
                    lineArt ? options.closeOutlineGaps() : true,
                    lineArt, options.lockedPaletteColors(), gridOffset.x(), gridOffset.y());
            reduced = illustration.downsampled();
            palette = illustration.palette();
            mapped = reduced.image();
        } else {
            reduced = downsampler.resize(source, options.width(), options.height(),
                    options.edgeStrength(), gridOffset.x(), gridOffset.y());
            palette = quantizer.createPalette(reduced.image(), reduced.importance(),
                    options.paletteSize(), options.lockedPaletteColors());
            mapped = mapToPalette(reduced.image(), reduced.importance(), palette,
                    options.ditheringMode(), options.ditheringStrength());
        }
        boolean ditheringNeedsItsPixels = resolvedMode == ProcessingMode.PHOTO
                && options.ditheringMode() != DitheringMode.OFF;
        if (options.cleanupIsolatedPixels() && !ditheringNeedsItsPixels) {
            mapped = cleanup(mapped, reduced.importance());
        }
        return new ConversionResult(mapped, palette, resolvedMode, gridOffset.x(), gridOffset.y());
    }

    private static final int[][] BAYER_4 = {
            {0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}
    };

    private BufferedImage mapToPalette(BufferedImage input, double[] importance, List<Integer> palette,
                                       DitheringMode dithering, double strength) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        double[][] labs = palette.stream().map(ColorMath::lab).toArray(double[][]::new);
        int width = input.getWidth(), height = input.getHeight();
        double[][] errorR = new double[height + 1][width + 2];
        double[][] errorG = new double[height + 1][width + 2];
        double[][] errorB = new double[height + 1][width + 2];
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int colour = input.getRGB(x, y);
                if (((colour >>> 24) & 0xff) < 16) {
                    output.setRGB(x, y, 0);
                    continue;
                }
                boolean protectedEdge = dithering == DitheringMode.EDGE_LIMITED
                        && importance[y * width + x] >= 0.22;
                double r = (colour >>> 16) & 0xff;
                double g = (colour >>> 8) & 0xff;
                double b = colour & 0xff;
                if ((dithering == DitheringMode.FLOYD_STEINBERG
                        || dithering == DitheringMode.EDGE_LIMITED) && !protectedEdge) {
                    r = clamp(r + errorR[y][x + 1] * strength);
                    g = clamp(g + errorG[y][x + 1] * strength);
                    b = clamp(b + errorB[y][x + 1] * strength);
                } else if (dithering == DitheringMode.ORDERED) {
                    double adjustment = (BAYER_4[y & 3][x & 3] - 7.5) * 3.2 * strength;
                    r = clamp(r + adjustment);
                    g = clamp(g + adjustment);
                    b = clamp(b + adjustment);
                }
                int adjusted = 0xff000000 | ((int) Math.round(r) << 16)
                        | ((int) Math.round(g) << 8) | (int) Math.round(b);
                int best = nearestPalette(ColorMath.lab(adjusted), labs);
                int selected = palette.get(best);
                output.setRGB(x, y, selected);

                if ((dithering == DitheringMode.FLOYD_STEINBERG
                        || dithering == DitheringMode.EDGE_LIMITED) && !protectedEdge) {
                    double dr = r - ((selected >>> 16) & 0xff);
                    double dg = g - ((selected >>> 8) & 0xff);
                    double db = b - (selected & 0xff);
                    diffuse(errorR, x, y, dr, width, height);
                    diffuse(errorG, x, y, dg, width, height);
                    diffuse(errorB, x, y, db, width, height);
                }
            }
        }
        return output;
    }

    private static int nearestPalette(double[] lab, double[][] paletteLabs) {
        int best = 0;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < paletteLabs.length; i++) {
            double candidate = ColorMath.labDistance(lab, paletteLabs[i]);
            if (candidate < distance) {
                distance = candidate;
                best = i;
            }
        }
        return best;
    }

    private static void diffuse(double[][] errors, int x, int y, double error,
                                int width, int height) {
        if (x + 1 < width) errors[y][x + 2] += error * 7 / 16.0;
        if (y + 1 >= height) return;
        if (x > 0) errors[y + 1][x] += error * 3 / 16.0;
        errors[y + 1][x + 1] += error * 5 / 16.0;
        if (x + 1 < width) errors[y + 1][x + 2] += error / 16.0;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(255, value));
    }

    private BufferedImage cleanup(BufferedImage input, double[] importance) {
        int width = input.getWidth(), height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int centre = input.getRGB(x, y);
                if (importance[y * width + x] >= 0.35) {
                    output.setRGB(x, y, centre);
                    continue;
                }
                MapCounter neighbours = new MapCounter();
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            neighbours.add(input.getRGB(nx, ny));
                        }
                    }
                }
                int dominant = neighbours.dominant();
                output.setRGB(x, y, neighbours.count(centre) <= 1 && neighbours.count(dominant) >= 5
                        ? dominant : centre);
            }
        }
        return output;
    }

    public record ConversionResult(BufferedImage image, List<Integer> palette,
                                   ProcessingMode resolvedMode,
                                   double gridOffsetX, double gridOffsetY) {}

    private static final class MapCounter {
        private final java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        void add(int colour) { counts.merge(colour, 1, Integer::sum); }
        int count(int colour) { return counts.getOrDefault(colour, 0); }
        int dominant() { return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey).orElse(0); }
    }
}
