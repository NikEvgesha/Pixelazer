package com.pixelazer.ui;

import java.awt.image.BufferedImage;

/** Mutable copy used by the editor so cancelling never changes the conversion result. */
final class PixelEditorModel {
    private final BufferedImage image;

    PixelEditorModel(BufferedImage source) {
        image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, source.getWidth(), source.getHeight(),
                source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()),
                0, source.getWidth());
    }

    int width() { return image.getWidth(); }
    int height() { return image.getHeight(); }
    int colourAt(int x, int y) { return image.getRGB(x, y); }

    boolean paint(int x, int y, int argb) {
        if (x < 0 || x >= width() || y < 0 || y >= height()) return false;
        if (image.getRGB(x, y) == argb) return false;
        image.setRGB(x, y, argb);
        return true;
    }

    BufferedImage copyImage() {
        BufferedImage copy = new BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, width(), height(),
                image.getRGB(0, 0, width(), height(), null, 0, width()), 0, width());
        return copy;
    }
}
