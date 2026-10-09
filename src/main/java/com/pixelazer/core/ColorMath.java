package com.pixelazer.core;

final class ColorMath {
    private ColorMath() {}

    static double luminance(int argb) {
        double r = ((argb >>> 16) & 0xff) / 255.0;
        double g = ((argb >>> 8) & 0xff) / 255.0;
        double b = (argb & 0xff) / 255.0;
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
    }

    static double rgbDistance(int a, int b) {
        double dr = ((a >>> 16) & 0xff) - ((b >>> 16) & 0xff);
        double dg = ((a >>> 8) & 0xff) - ((b >>> 8) & 0xff);
        double db = (a & 0xff) - (b & 0xff);
        return Math.sqrt(dr * dr + dg * dg + db * db) / 441.67295593;
    }

    static double[] lab(int argb) {
        double r = linear(((argb >>> 16) & 0xff) / 255.0);
        double g = linear(((argb >>> 8) & 0xff) / 255.0);
        double b = linear((argb & 0xff) / 255.0);
        double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047;
        double y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b);
        double z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883;
        double fx = pivot(x), fy = pivot(y), fz = pivot(z);
        return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    static double labDistance(double[] a, double[] b) {
        double dl = a[0] - b[0], da = a[1] - b[1], db = a[2] - b[2];
        return dl * dl + da * da + db * db;
    }

    private static double linear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double pivot(double value) {
        double epsilon = 216.0 / 24389.0;
        return value > epsilon ? Math.cbrt(value) : (24389.0 / 27.0 * value + 16) / 116;
    }
}
