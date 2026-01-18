package me.revqz.minhutBillboards.billboard;

import me.revqz.minhutBillboards.MinhutBillboards;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Handles loading and processing images for billboards.
 * Images can be loaded from URLs or local files in the images folder.
 */
public class ImageLoader {

    private static final int MAP_SIZE = 128; // Minecraft maps are 128x128 pixels

    /**
     * Loads an image from a URL asynchronously.
     */
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

    /**
     * Loads an image from the local images folder asynchronously.
     */
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

    /**
     * Loads an image from either a URL or local file.
     * If the source starts with http:// or https://, it's treated as a URL.
     * Otherwise, it's treated as a filename in the images folder.
     */
    public static CompletableFuture<BufferedImage> loadImage(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return loadImageFromUrl(source);
        } else {
            return loadImageFromFile(source);
        }
    }

    /**
     * Resizes an image to fit the billboard dimensions.
     */
    public static BufferedImage resizeImage(BufferedImage image, int widthBlocks, int heightBlocks) {
        int targetWidth = widthBlocks * MAP_SIZE;
        int targetHeight = heightBlocks * MAP_SIZE;

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();

        // Use highest quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Splits an image into 128x128 tiles for map rendering.
     * Tiles are arranged in a 2D array [x][y] where:
     * - x goes from left to right (0 to widthBlocks-1)
     * - y goes from BOTTOM to TOP (0 is bottom, heightBlocks-1 is top)
     */
    public static BufferedImage[][] splitIntoTiles(BufferedImage image, int widthBlocks, int heightBlocks) {
        BufferedImage[][] tiles = new BufferedImage[widthBlocks][heightBlocks];

        for (int x = 0; x < widthBlocks; x++) {
            for (int y = 0; y < heightBlocks; y++) {
                int pixelX = x * MAP_SIZE;
                int pixelY = (heightBlocks - 1 - y) * MAP_SIZE; // Flip Y

                tiles[x][y] = image.getSubimage(pixelX, pixelY, MAP_SIZE, MAP_SIZE);
            }
        }

        return tiles;
    }

    /**
     * Loads, resizes, dithers, and splits an image in one operation.
     * Supports both URLs and local filenames.
     * Applies dithering based on config settings for better color accuracy.
     */
    public static CompletableFuture<BufferedImage[][]> loadAndProcessImage(String source, int widthBlocks,
            int heightBlocks) {
        return loadImage(source).thenApply(image -> {
            if (image == null) {
                return null;
            }

            // Resize the image first
            BufferedImage resized = resizeImage(image, widthBlocks, heightBlocks);

            // Apply dithering based on config
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
