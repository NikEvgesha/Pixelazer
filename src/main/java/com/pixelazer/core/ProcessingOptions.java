package com.pixelazer.core;

import java.util.List;

/** Immutable conversion settings. */
public record ProcessingOptions(
        int width,
        int height,
        int paletteSize,
        double edgeStrength,
        boolean cleanupIsolatedPixels,
        ProcessingMode mode,
        DetailLevel detailLevel,
        OutlineSelection outlineSelection,
        int manualOutlineArgb,
        OutlineThickness outlineThickness,
        boolean closeOutlineGaps,
        DitheringMode ditheringMode,
        double ditheringStrength,
        List<Integer> lockedPaletteColors,
        GridAlignmentMode gridAlignmentMode,
        double gridOffsetX,
        double gridOffsetY) {

    public ProcessingOptions {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Output dimensions must be positive");
        }
        if (paletteSize < 2 || paletteSize > 32) {
            throw new IllegalArgumentException("Palette size must be between 2 and 32");
        }
        if (!Double.isFinite(edgeStrength) || edgeStrength < 0 || edgeStrength > 16) {
            throw new IllegalArgumentException("Edge strength must be between 0 and 16");
        }
        if (mode == null) throw new IllegalArgumentException("Processing mode is required");
        if (detailLevel == null || outlineSelection == null || outlineThickness == null) {
            throw new IllegalArgumentException("Advanced processing settings are required");
        }
        if (ditheringMode == null || !Double.isFinite(ditheringStrength)
                || ditheringStrength < 0 || ditheringStrength > 1) {
            throw new IllegalArgumentException("Invalid dithering settings");
        }
        if (lockedPaletteColors == null || lockedPaletteColors.size() > paletteSize) {
            throw new IllegalArgumentException("Too many locked palette colours");
        }
        if (gridAlignmentMode == null || !Double.isFinite(gridOffsetX) || !Double.isFinite(gridOffsetY)
                || gridOffsetX < -0.5 || gridOffsetX > 0.5
                || gridOffsetY < -0.5 || gridOffsetY > 0.5) {
            throw new IllegalArgumentException("Invalid pixel grid alignment");
        }
        lockedPaletteColors = List.copyOf(lockedPaletteColors.stream().distinct().toList());
    }

    public ProcessingOptions(int width, int height, int paletteSize, double edgeStrength,
                             boolean cleanupIsolatedPixels) {
        this(width, height, paletteSize, edgeStrength, cleanupIsolatedPixels, ProcessingMode.AUTO,
                DetailLevel.BALANCED, OutlineSelection.AUTO, 0xff000000,
                OutlineThickness.NORMAL, true, DitheringMode.OFF, 0.65, List.of(),
                GridAlignmentMode.AUTO, 0, 0);
    }

    public ProcessingOptions(int width, int height, int paletteSize, double edgeStrength,
                             boolean cleanupIsolatedPixels, ProcessingMode mode) {
        this(width, height, paletteSize, edgeStrength, cleanupIsolatedPixels, mode,
                DetailLevel.BALANCED, OutlineSelection.AUTO, 0xff000000,
                OutlineThickness.NORMAL, true, DitheringMode.OFF, 0.65, List.of(),
                GridAlignmentMode.AUTO, 0, 0);
    }

    public ProcessingOptions(int width, int height, int paletteSize, double edgeStrength,
                             boolean cleanupIsolatedPixels, ProcessingMode mode,
                             DetailLevel detailLevel, OutlineSelection outlineSelection,
                             int manualOutlineArgb, OutlineThickness outlineThickness,
                             boolean closeOutlineGaps) {
        this(width, height, paletteSize, edgeStrength, cleanupIsolatedPixels, mode,
                detailLevel, outlineSelection, manualOutlineArgb, outlineThickness,
                closeOutlineGaps, DitheringMode.OFF, 0.65, List.of(),
                GridAlignmentMode.AUTO, 0, 0);
    }

    public ProcessingOptions(int width, int height, int paletteSize, double edgeStrength,
                             boolean cleanupIsolatedPixels, ProcessingMode mode,
                             DetailLevel detailLevel, OutlineSelection outlineSelection,
                             int manualOutlineArgb, OutlineThickness outlineThickness,
                             boolean closeOutlineGaps, DitheringMode ditheringMode,
                             double ditheringStrength, List<Integer> lockedPaletteColors) {
        this(width, height, paletteSize, edgeStrength, cleanupIsolatedPixels, mode,
                detailLevel, outlineSelection, manualOutlineArgb, outlineThickness,
                closeOutlineGaps, ditheringMode, ditheringStrength, lockedPaletteColors,
                GridAlignmentMode.AUTO, 0, 0);
    }

    public static ProcessingOptions defaults(int width, int height) {
        return new ProcessingOptions(width, height, 16, 8.0, true, ProcessingMode.AUTO,
                DetailLevel.BALANCED, OutlineSelection.AUTO, 0xff000000,
                OutlineThickness.NORMAL, true, DitheringMode.OFF, 0.65, List.of(),
                GridAlignmentMode.AUTO, 0, 0);
    }

    public ProcessingOptions withMode(ProcessingMode newMode) {
        return new ProcessingOptions(width, height, paletteSize, edgeStrength,
                cleanupIsolatedPixels, newMode, detailLevel, outlineSelection,
                manualOutlineArgb, outlineThickness, closeOutlineGaps,
                ditheringMode, ditheringStrength, lockedPaletteColors,
                gridAlignmentMode, gridOffsetX, gridOffsetY);
    }
}
