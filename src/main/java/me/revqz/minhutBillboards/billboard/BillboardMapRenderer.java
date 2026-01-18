package me.revqz.minhutBillboards.billboard;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * Custom renderer that draws a portion of an image onto a map.
 * Each map displays a 128x128 pixel section of the full billboard image.
 */
public class BillboardMapRenderer extends MapRenderer {

    private final BufferedImage tileImage;
    private boolean rendered = false;

    public BillboardMapRenderer(BufferedImage tileImage) {
        super(false); // Not contextual - same image for all players
        this.tileImage = tileImage;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        // Only render once for performance
        if (rendered) {
            return;
        }

        if (tileImage != null) {
            // Draw the image onto the map canvas
            // MapPalette.imageToBytes converts the image to Minecraft's map color palette
            try {
                canvas.drawImage(0, 0, tileImage);
                rendered = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Force re-render on next render call
     */
    public void invalidate() {
        rendered = false;
    }
}
