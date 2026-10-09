package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DitheringAndPaletteTest {
    @Test
    void lockedColourIsAlwaysPresentInGeneratedPalette() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) for (int x = 0; x < 20; x++) {
            image.setRGB(x, y, 0xff000000 | (x * 12 << 16) | (y * 12 << 8));
        }
        int lockedMagenta = 0xffff00ff;
        var options = new ProcessingOptions(10, 10, 4, 8, false,
                ProcessingMode.PHOTO, DetailLevel.BALANCED, OutlineSelection.AUTO,
                0xff000000, OutlineThickness.NORMAL, true,
                DitheringMode.OFF, 0.65, List.of(lockedMagenta));

        var result = new PixelArtConverter().convert(image, options);

        assertTrue(result.palette().contains(lockedMagenta));
        assertTrue(result.palette().size() <= 4);
    }

    @Test
    void floydSteinbergUsesBothLockedTonesForMidGrey() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            image.setRGB(x, y, 0xff646464);
        }
        var options = new ProcessingOptions(16, 16, 2, 0, false,
                ProcessingMode.PHOTO, DetailLevel.BALANCED, OutlineSelection.AUTO,
                0xff000000, OutlineThickness.NORMAL, false,
                DitheringMode.FLOYD_STEINBERG, 1, List.of(0xff000000, 0xffffffff));

        BufferedImage result = new PixelArtConverter().convert(image, options).image();
        Set<Integer> used = new HashSet<>();
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            used.add(result.getRGB(x, y));
        }

        assertEquals(Set.of(0xff000000, 0xffffffff), used);
    }
}
