package me.revqz.minhutBillboards.billboard;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

public class BillboardMapRenderer extends MapRenderer {

    private final BufferedImage tileImage;
    private boolean rendered = false;

    public BillboardMapRenderer(BufferedImage tileImage) {
        super(false);
        this.tileImage = tileImage;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (rendered) {
            return;
        }
        if (tileImage != null) {
            try {
                canvas.drawImage(0, 0, tileImage);
                rendered = true;
            } catch (Exception e) {
            }
        }
    }

    public void invalidate() {
        rendered = false;
    }
}
