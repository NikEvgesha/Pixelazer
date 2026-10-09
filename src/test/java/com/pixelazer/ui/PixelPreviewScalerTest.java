package com.pixelazer.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixelPreviewScalerTest {
    @Test
    void enlargesByWholeNumberAndReplicatesPixelsExactly() {
        BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xff112233);
        source.setRGB(1, 0, 0xffddeeff);

        BufferedImage preview = PixelPreviewScaler.scale(source, 9, 7);

        assertEquals(8, preview.getWidth());
        assertEquals(4, preview.getHeight());
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) assertEquals(0xff112233, preview.getRGB(x, y));
            for (int x = 4; x < 8; x++) assertEquals(0xffddeeff, preview.getRGB(x, y));
        }
    }

    @Test
    void reducesOversizedPreviewByWholeNumber() {
        BufferedImage source = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        BufferedImage preview = PixelPreviewScaler.scale(source, 480, 600);
        assertEquals(334, preview.getWidth());
        assertEquals(234, preview.getHeight());
    }
}
