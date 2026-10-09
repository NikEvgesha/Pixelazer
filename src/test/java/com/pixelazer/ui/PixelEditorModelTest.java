package com.pixelazer.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PixelEditorModelTest {
    @Test
    void editsItsOwnCopyWithoutChangingSource() {
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(1, 1, 0xff112233);
        PixelEditorModel model = new PixelEditorModel(source);

        assertTrue(model.paint(1, 1, 0xffabcdef));

        assertEquals(0xff112233, source.getRGB(1, 1));
        assertEquals(0xffabcdef, model.copyImage().getRGB(1, 1));
    }

    @Test
    void rejectsCoordinatesOutsideCanvas() {
        PixelEditorModel model = new PixelEditorModel(
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));

        assertFalse(model.paint(-1, 0, 0xffffffff));
        assertFalse(model.paint(2, 1, 0xffffffff));
    }
}
