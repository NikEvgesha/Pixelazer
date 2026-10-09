package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IllustrationDownsamplerTest {
    @Test
    void preservesAContinuousOnePixelOutline() {
        BufferedImage source = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 80, 80);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(15, 15, 48, 48);
        graphics.dispose();

        BufferedImage result = new IllustrationDownsampler().resize(source, 10, 10, 4)
                .downsampled().image();

        int darkPixels = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if ((result.getRGB(x, y) & 0xff) < 40) darkPixels++;
            }
        }
        assertTrue(darkPixels >= 16, "the rectangular outline should survive downsampling");
        assertTrue(darkPixels < 50, "the outline must not turn into a filled region");
    }

    @Test
    void autoDetectorRecognizesFlatArtwork() {
        BufferedImage source = new BufferedImage(60, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(0, 0, 60, 40);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(10, 10, 40, 20);
        graphics.dispose();

        assertEquals(ProcessingMode.ILLUSTRATION, new ImageTypeDetector().detect(source));
    }
}
