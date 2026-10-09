package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PixelArtConverterTest {
    @Test
    void respectsPaletteLimitAndDimensions() {
        BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                source.setRGB(x, y, 0xff000000 | (x * 4 << 16) | (y * 4 << 8) | ((x + y) * 2));
            }
        }
        var converted = new PixelArtConverter().convert(source,
                new ProcessingOptions(16, 12, 8, 8, true));
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < 12; y++) for (int x = 0; x < 16; x++) {
            colours.add(converted.image().getRGB(x, y));
        }
        assertEquals(16, converted.image().getWidth());
        assertEquals(12, converted.image().getHeight());
        assertTrue(colours.size() <= 8);
        assertTrue(converted.palette().size() <= 8);
    }

    @Test
    void validatesPaletteRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessingOptions(16, 16, 33, 8, true));
    }
}
