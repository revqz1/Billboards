package me.revqz.minhutBillboards.billboard;

import me.revqz.minhutBillboards.MinhutBillboards;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class ImageLoader {

    private static final int MAP_SIZE = 128;

    public static CompletableFuture<BufferedImage> loadImageFromUrl(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new java.net.URI(imageUrl).toURL();
                BufferedImage image = ImageIO.read(url);
                if (image == null) {
                    MinhutBillboards.getInstance().getLogger().warning("Failed to load image from URL: " + imageUrl);
                    return null;
                }
                return image;
            } catch (Exception e) {
                MinhutBillboards.getInstance().getLogger().severe("Error loading image from URL: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public static CompletableFuture<BufferedImage> loadImageFromFile(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File imagesFolder = new File(MinhutBillboards.getInstance().getDataFolder(), "images");
                if (!imagesFolder.exists()) {
                    imagesFolder.mkdirs();
                    MinhutBillboards.getInstance().getLogger()
                            .info("Created images folder at: " + imagesFolder.getAbsolutePath());
                }

                File imageFile = new File(imagesFolder, filename);
                if (!imageFile.exists()) {
                    MinhutBillboards.getInstance().getLogger()
                            .warning("Image file not found: " + imageFile.getAbsolutePath());
                    return null;
                }

                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    MinhutBillboards.getInstance().getLogger().warning("Failed to read image file: " + filename);
                    return null;
                }
                return image;
            } catch (Exception e) {
                MinhutBillboards.getInstance().getLogger().severe("Error loading image from file: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public static CompletableFuture<BufferedImage> loadImage(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return loadImageFromUrl(source);
        } else {
            return loadImageFromFile(source);
        }
    }

    public static BufferedImage resizeImage(BufferedImage image, int widthBlocks, int heightBlocks) {
        int targetWidth = widthBlocks * MAP_SIZE;
        int targetHeight = heightBlocks * MAP_SIZE;

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    public static BufferedImage[][] splitIntoTiles(BufferedImage image, int widthBlocks, int heightBlocks) {
        BufferedImage[][] tiles = new BufferedImage[widthBlocks][heightBlocks];

        for (int x = 0; x < widthBlocks; x++) {
            for (int y = 0; y < heightBlocks; y++) {
                int pixelX = x * MAP_SIZE;
                int pixelY = (heightBlocks - 1 - y) * MAP_SIZE;

                tiles[x][y] = image.getSubimage(pixelX, pixelY, MAP_SIZE, MAP_SIZE);
            }
        }

        return tiles;
    }

    public static CompletableFuture<BufferedImage[][]> loadAndProcessImage(String source, int widthBlocks,
            int heightBlocks) {
        return loadImage(source).thenApply(image -> {
            if (image == null) {
                return null;
            }

            BufferedImage resized = resizeImage(image, widthBlocks, heightBlocks);

            String ditheringMode = MinhutBillboards.getInstance().getConfig()
                    .getString("effects.dithering", "floyd_steinberg");

            BufferedImage processed;
            switch (ditheringMode.toLowerCase()) {
                case "floyd_steinberg" -> {
                    MinhutBillboards.getInstance().getLogger().info("Applying Floyd-Steinberg dithering...");
                    processed = ColorDithering.applyFloydSteinbergDithering(resized);
                }
                case "ordered" -> {
                    MinhutBillboards.getInstance().getLogger().info("Applying ordered dithering...");
                    processed = ColorDithering.applyOrderedDithering(resized);
                }
                case "none" -> {
                    processed = resized;
                }
                default -> {
                    processed = ColorDithering.applyFloydSteinbergDithering(resized);
                }
            }

            return splitIntoTiles(processed, widthBlocks, heightBlocks);
        });
    }
}
