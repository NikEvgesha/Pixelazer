package com.pixelazer.io;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/** Writes a real indexed PNG whose PLTE contains only colours used by the image. */
public final class IndexedPngWriter {
    private IndexedPngWriter() {}

    public static void write(BufferedImage source, List<Integer> preferredOrder, File file)
            throws IOException {
        if (source == null || file == null) throw new IllegalArgumentException("Image and file are required");

        Set<Integer> used = new LinkedHashSet<>();
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) used.add(source.getRGB(x, y));
        }
        if (used.size() > 256) {
            throw new IOException("Indexed PNG supports at most 256 colours; found " + used.size());
        }

        List<Integer> entries = new ArrayList<>(used.size());
        if (preferredOrder != null) {
            for (int colour : preferredOrder) {
                if (used.contains(colour) && !entries.contains(colour)) entries.add(colour);
            }
        }
        for (int colour : used) if (!entries.contains(colour)) entries.add(colour);

        int bits = entries.size() <= 2 ? 1 : entries.size() <= 4 ? 2 : entries.size() <= 16 ? 4 : 8;
        Map<Integer, Integer> indices = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            indices.put(entries.get(i), i);
        }

        try (DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            output.write(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream(13);
            try (DataOutputStream header = new DataOutputStream(headerBytes)) {
                header.writeInt(source.getWidth());
                header.writeInt(source.getHeight());
                header.writeByte(bits);
                header.writeByte(3); // indexed colour
                header.writeByte(0); // compression
                header.writeByte(0); // filtering
                header.writeByte(0); // no interlace
            }
            writeChunk(output, "IHDR", headerBytes.toByteArray());

            byte[] palette = new byte[entries.size() * 3];
            int lastTransparent = -1;
            for (int i = 0; i < entries.size(); i++) {
                int argb = entries.get(i);
                palette[i * 3] = (byte) (argb >>> 16);
                palette[i * 3 + 1] = (byte) (argb >>> 8);
                palette[i * 3 + 2] = (byte) argb;
                if ((argb >>> 24) != 0xff) lastTransparent = i;
            }
            writeChunk(output, "PLTE", palette);
            if (lastTransparent >= 0) {
                byte[] transparency = new byte[lastTransparent + 1];
                for (int i = 0; i <= lastTransparent; i++) {
                    transparency[i] = (byte) (entries.get(i) >>> 24);
                }
                writeChunk(output, "tRNS", transparency);
            }

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
                int rowBytes = (source.getWidth() * bits + 7) / 8;
                for (int y = 0; y < source.getHeight(); y++) {
                    byte[] row = new byte[rowBytes + 1]; // filter byte is zero
                    for (int x = 0; x < source.getWidth(); x++) {
                        int index = indices.get(source.getRGB(x, y));
                        int bitPosition = x * bits;
                        int shift = 8 - bits - bitPosition % 8;
                        row[1 + bitPosition / 8] |= (byte) (index << shift);
                    }
                    deflater.write(row);
                }
            }
            writeChunk(output, "IDAT", compressed.toByteArray());
            writeChunk(output, "IEND", new byte[0]);
        }
    }

    private static void writeChunk(DataOutputStream output, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        output.writeInt(data.length);
        output.write(typeBytes);
        output.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        output.writeInt((int) crc.getValue());
    }
}
