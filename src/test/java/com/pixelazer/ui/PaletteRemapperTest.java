package com.pixelazer.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaletteRemapperTest {
    @Test
    void replacesEveryMatchingPixelAndLeavesTransparencyUntouched() {
        BufferedImage source = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xffff0000);
        source.setRGB(1, 0, 0xff00ff00);
        source.setRGB(2, 0, 0x00000000);

        BufferedImage result = PaletteRemapper.remap(source,
                List.of(0xffff0000, 0xff00ff00),
                List.of(0xff0000ff, 0xffffff00));

        assertEquals(0xff0000ff, result.getRGB(0, 0));
        assertEquals(0xffffff00, result.getRGB(1, 0));
        assertEquals(0x00000000, result.getRGB(2, 0));
    }
}
