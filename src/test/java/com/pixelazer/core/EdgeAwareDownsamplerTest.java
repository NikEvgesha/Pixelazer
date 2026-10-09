package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class EdgeAwareDownsamplerTest {
    @Test
    void keepsThinHighContrastVerticalLine() {
        BufferedImage source = solid(80, 80, Color.WHITE);
        for (int y = 0; y < 80; y++) source.setRGB(39, y, Color.BLACK.getRGB());

        BufferedImage result = new EdgeAwareDownsampler().resize(source, 10, 10, 12).image();

        int darkPixels = 0;
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                if ((result.getRGB(x, y) & 0xff) < 80) darkPixels++;
            }
        }
        assertTrue(darkPixels >= 8, "the vertical line should remain continuous");
    }

    @Test
    void producesExactRequestedDimensions() {
        DownsampleResult result = new EdgeAwareDownsampler().resize(solid(37, 21, Color.RED), 9, 5, 8);
        assertEquals(9, result.image().getWidth());
        assertEquals(5, result.image().getHeight());
        assertEquals(45, result.importance().length);
    }

    private static BufferedImage solid(int width, int height, Color colour) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(colour);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }
}
