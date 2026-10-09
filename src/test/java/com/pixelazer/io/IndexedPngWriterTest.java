package com.pixelazer.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexedPngWriterTest {
    @TempDir Path temporaryDirectory;

    @Test
    void writesPalettePngWithOnlyUsedColoursAndTransparency() throws Exception {
        BufferedImage source = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xffff0000);
        source.setRGB(1, 0, 0xff0000ff);
        source.setRGB(2, 0, 0x00000000);
        File output = temporaryDirectory.resolve("indexed.png").toFile();

        IndexedPngWriter.write(source,
                List.of(0xff00ff00, 0xff0000ff, 0xffff0000), output);

        byte[] png = Files.readAllBytes(output.toPath());
        assertEquals(3, png[25] & 0xff, "IHDR colour type must be indexed palette");
        assertEquals(9, chunkLength(png, "PLTE"), "PLTE must contain exactly three RGB entries");
        BufferedImage restored = ImageIO.read(output);
        assertInstanceOf(java.awt.image.IndexColorModel.class, restored.getColorModel());
        assertEquals(0xffff0000, restored.getRGB(0, 0));
        assertEquals(0xff0000ff, restored.getRGB(1, 0));
        assertEquals(0x00000000, restored.getRGB(2, 0));
    }

    @Test
    void rejectsMoreThan256ExactColours() {
        BufferedImage source = new BufferedImage(257, 1, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 257; x++) source.setRGB(x, 0, 0xff000000 | x);

        assertThrows(java.io.IOException.class, () -> IndexedPngWriter.write(
                source, List.of(), temporaryDirectory.resolve("too-many.png").toFile()));
    }

    private static int chunkLength(byte[] png, String requestedType) {
        int offset = 8;
        while (offset + 12 <= png.length) {
            int length = ByteBuffer.wrap(png, offset, 4).getInt();
            String type = new String(png, offset + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (type.equals(requestedType)) return length;
            offset += 12 + length;
        }
        fail("Missing PNG chunk " + requestedType);
        return -1;
    }
}
