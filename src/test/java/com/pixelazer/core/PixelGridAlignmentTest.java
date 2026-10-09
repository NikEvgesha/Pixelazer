package com.pixelazer.core;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixelGridAlignmentTest {
    @Test
    void manualGridOffsetIsReportedByConversion() {
        BufferedImage source = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        var options = new ProcessingOptions(8, 8, 4, 8, true,
                ProcessingMode.PHOTO, DetailLevel.BALANCED, OutlineSelection.AUTO,
                0xff000000, OutlineThickness.NORMAL, true,
                DitheringMode.OFF, 0.5, List.of(),
                GridAlignmentMode.MANUAL, 0.3, -0.2);

        var result = new PixelArtConverter().convert(source, options);

        assertEquals(0.3, result.gridOffsetX(), 0.0001);
        assertEquals(-0.2, result.gridOffsetY(), 0.0001);
    }

    @Test
    void autoAlignmentKeepsNeutralOffsetForFlatImage() {
        BufferedImage source = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        PixelGridAligner.Offset offset = new PixelGridAligner().findBestOffset(source, 8, 8);
        assertEquals(0, offset.x(), 0.0001);
        assertEquals(0, offset.y(), 0.0001);
    }
}
