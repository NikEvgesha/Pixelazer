package com.pixelazer.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Weighted median-cut palette builder; structural pixels count more. */
public final class WeightedMedianCut {
    public List<Integer> createPalette(BufferedImage image, int maximumColours) {
        return createPalette(image, null, maximumColours);
    }

    public List<Integer> createPalette(BufferedImage image, double[] importance, int maximumColours) {
        if (maximumColours < 1 || maximumColours > 32) {
            throw new IllegalArgumentException("Palette size must be between 1 and 32");
        }
        Map<Integer, Sample> merged = new HashMap<>();
        int width = image.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha == 0) continue;
                double structuralWeight = importance == null ? 0 : importance[y * width + x];
                double weight = alpha / 255.0 * (1.0 + 3.0 * structuralWeight);
                merged.compute(argb, (key, old) -> old == null
                        ? new Sample(argb, weight) : new Sample(argb, old.weight + weight));
            }
        }
        if (merged.isEmpty()) return List.of(0);

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(new ArrayList<>(merged.values())));
        while (boxes.size() < maximumColours) {
            Box split = boxes.stream().filter(box -> box.samples.size() > 1)
                    .max(Comparator.comparingDouble(Box::priority)).orElse(null);
            if (split == null) break;
            boxes.remove(split);
            boxes.addAll(split.split());
        }
        return boxes.stream().map(Box::average).toList();
    }

    public List<Integer> createPalette(BufferedImage image, double[] importance,
                                       int maximumColours, List<Integer> lockedColours) {
        List<Integer> locked = lockedColours.stream().distinct().limit(maximumColours).toList();
        int generatedSlots = maximumColours - locked.size();
        List<Integer> result = new ArrayList<>(locked);
        if (generatedSlots > 0) {
            for (int colour : createPalette(image, importance, generatedSlots)) {
                if (!result.contains(colour)) result.add(colour);
                if (result.size() == maximumColours) break;
            }
        }
        return List.copyOf(result);
    }

    private record Sample(int argb, double weight) {
        int channel(int channel) {
            return switch (channel) {
                case 0 -> (argb >>> 16) & 0xff;
                case 1 -> (argb >>> 8) & 0xff;
                default -> argb & 0xff;
            };
        }
    }

    private static final class Box {
        final List<Sample> samples;
        final int channel;
        final int range;
        final double weight;

        Box(List<Sample> samples) {
            this.samples = samples;
            int bestChannel = 0, bestRange = -1;
            for (int c = 0; c < 3; c++) {
                int min = 255, max = 0;
                for (Sample sample : samples) {
                    min = Math.min(min, sample.channel(c));
                    max = Math.max(max, sample.channel(c));
                }
                if (max - min > bestRange) {
                    bestRange = max - min;
                    bestChannel = c;
                }
            }
            channel = bestChannel;
            range = bestRange;
            weight = samples.stream().mapToDouble(Sample::weight).sum();
        }

        double priority() { return range * weight; }

        List<Box> split() {
            samples.sort(Comparator.comparingInt(sample -> sample.channel(channel)));
            double halfway = weight / 2, accumulated = 0;
            int index = 1;
            for (; index < samples.size(); index++) {
                accumulated += samples.get(index - 1).weight;
                if (accumulated >= halfway) break;
            }
            index = Math.max(1, Math.min(samples.size() - 1, index));
            return List.of(new Box(new ArrayList<>(samples.subList(0, index))),
                    new Box(new ArrayList<>(samples.subList(index, samples.size()))));
        }

        int average() {
            double a = 0, r = 0, g = 0, b = 0, total = 0;
            for (Sample sample : samples) {
                double w = sample.weight;
                a += ((sample.argb >>> 24) & 0xff) * w;
                r += ((sample.argb >>> 16) & 0xff) * w;
                g += ((sample.argb >>> 8) & 0xff) * w;
                b += (sample.argb & 0xff) * w;
                total += w;
            }
            return ((int) Math.round(a / total) << 24)
                    | ((int) Math.round(r / total) << 16)
                    | ((int) Math.round(g / total) << 8)
                    | (int) Math.round(b / total);
        }
    }
}
