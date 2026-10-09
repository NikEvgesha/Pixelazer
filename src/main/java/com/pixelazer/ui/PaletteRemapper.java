package com.pixelazer.ui;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

final class PaletteRemapper {
    private PaletteRemapper() {}

    static BufferedImage remap(BufferedImage source, List<Integer> original, List<Integer> replacement) {
        if (original.size() != replacement.size()) {
            throw new IllegalArgumentException("Palette sizes must match");
        }
        Map<Integer, Integer> mapping = IntStream.range(0, original.size()).boxed()
                .collect(java.util.stream.Collectors.toMap(original::get, replacement::get,
                        (first, second) -> second, java.util.LinkedHashMap::new));
        BufferedImage remapped = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int colour = source.getRGB(x, y);
                remapped.setRGB(x, y, mapping.getOrDefault(colour, colour));
            }
        }
        return remapped;
    }
}
