package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineArtModeTest {
    @Test
    void manualColouredOutlineIsProtected() {
        BufferedImage source = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 80, 80);
        graphics.setColor(Color.RED);
        graphics.drawLine(39, 0, 39, 79);
        graphics.dispose();

        var options = new ProcessingOptions(10, 10, 4, 8, true,
                ProcessingMode.LINE_ART, DetailLevel.HIGH, OutlineSelection.MANUAL,
                Color.RED.getRGB(), OutlineThickness.NORMAL, true);
        var converted = new PixelArtConverter().convert(source, options);

        int redPixels = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                int colour = converted.image().getRGB(x, y);
                if (((colour >>> 16) & 0xff) > 220 && (colour & 0xff) < 40) redPixels++;
            }
        }
        assertEquals(ProcessingMode.LINE_ART, converted.resolvedMode());
        assertTrue(redPixels >= 8, "manual red outline should survive the reduction");
    }

    @Test
    void illustrationModeIgnoresLineArtOutlineSettings() {
        BufferedImage source = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 40, 40);
        graphics.setColor(Color.BLACK);
        graphics.drawOval(5, 5, 30, 30);
        graphics.dispose();

        var first = new ProcessingOptions(12, 12, 4, 8, true,
                ProcessingMode.ILLUSTRATION, DetailLevel.LOW, OutlineSelection.MANUAL,
                Color.RED.getRGB(), OutlineThickness.THICK, false,
                DitheringMode.OFF, 0.5, List.of());
        var second = new ProcessingOptions(12, 12, 4, 8, true,
                ProcessingMode.ILLUSTRATION, DetailLevel.HIGH, OutlineSelection.DARKEST,
                Color.BLUE.getRGB(), OutlineThickness.THIN, true,
                DitheringMode.OFF, 0.5, List.of());

        BufferedImage a = new PixelArtConverter().convert(source, first).image();
        BufferedImage b = new PixelArtConverter().convert(source, second).image();
        assertArrayEquals(a.getRGB(0, 0, 12, 12, null, 0, 12),
                b.getRGB(0, 0, 12, 12, null, 0, 12));
    }
}
