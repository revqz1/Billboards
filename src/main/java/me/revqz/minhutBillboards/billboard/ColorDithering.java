package me.revqz.minhutBillboards.billboard;

import org.bukkit.map.MapPalette;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorDithering {

    private static final Color[] MAP_COLORS = extractMapColors();

    @SuppressWarnings("deprecation")
    private static Color[] extractMapColors() {
        java.util.Set<Color> uniqueColors = new java.util.LinkedHashSet<>();

        for (int i = 4; i < 256; i++) {
            try {
                Color c = MapPalette.getColor((byte) i);
                if (c.getAlpha() > 0) {
                    uniqueColors.add(c);
                }
            } catch (Exception e) {
            }
        }

        return uniqueColors.toArray(new Color[0]);
    }

    public static BufferedImage applyFloydSteinbergDithering(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        float[][] red = new float[width][height];
        float[][] green = new float[width][height];
        float[][] blue = new float[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                red[x][y] = (rgb >> 16) & 0xFF;
                green[x][y] = (rgb >> 8) & 0xFF;
                blue[x][y] = rgb & 0xFF;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int oldR = clamp((int) red[x][y]);
                int oldG = clamp((int) green[x][y]);
                int oldB = clamp((int) blue[x][y]);

                Color closest = findClosestColor(oldR, oldG, oldB);
                int newR = closest.getRed();
                int newG = closest.getGreen();
                int newB = closest.getBlue();

                float errR = oldR - newR;
                float errG = oldG - newG;
                float errB = oldB - newB;

                if (x + 1 < width) {
                    red[x + 1][y] += errR * 7f / 16f;
                    green[x + 1][y] += errG * 7f / 16f;
                    blue[x + 1][y] += errB * 7f / 16f;
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    red[x - 1][y + 1] += errR * 3f / 16f;
                    green[x - 1][y + 1] += errG * 3f / 16f;
                    blue[x - 1][y + 1] += errB * 3f / 16f;
                }
                if (y + 1 < height) {
                    red[x][y + 1] += errR * 5f / 16f;
                    green[x][y + 1] += errG * 5f / 16f;
                    blue[x][y + 1] += errB * 5f / 16f;
                }
                if (x + 1 < width && y + 1 < height) {
                    red[x + 1][y + 1] += errR * 1f / 16f;
                    green[x + 1][y + 1] += errG * 1f / 16f;
                    blue[x + 1][y + 1] += errB * 1f / 16f;
                }

                red[x][y] = newR;
                green[x][y] = newG;
                blue[x][y] = newB;
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = clamp((int) red[x][y]);
                int g = clamp((int) green[x][y]);
                int b = clamp((int) blue[x][y]);
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private static Color findClosestColor(int r, int g, int b) {
        Color closest = MAP_COLORS[0];
        double minDistance = Double.MAX_VALUE;

        for (Color mapColor : MAP_COLORS) {
            double dr = (r - mapColor.getRed()) * 0.30;
            double dg = (g - mapColor.getGreen()) * 0.59;
            double db = (b - mapColor.getBlue()) * 0.11;

            double distance = dr * dr + dg * dg + db * db;

            if (distance < minDistance) {
                minDistance = distance;
                closest = mapColor;
            }
        }

        return closest;
    }

    public static BufferedImage applyOrderedDithering(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        int[][] bayerMatrix = {
                { 0, 8, 2, 10 },
                { 12, 4, 14, 6 },
                { 3, 11, 1, 9 },
                { 15, 7, 13, 5 }
        };

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int threshold = bayerMatrix[y % 4][x % 4] * 16 - 128;

                r = clamp(r + threshold / 4);
                g = clamp(g + threshold / 4);
                b = clamp(b + threshold / 4);

                Color closest = findClosestColor(r, g, b);
                result.setRGB(x, y, closest.getRGB());
            }
        }

        return result;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
