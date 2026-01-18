package me.revqz.minhutBillboards.commands;

import me.revqz.minhutBillboards.MinhutBillboards;
import me.revqz.minhutBillboards.billboard.BillboardManager;
import me.revqz.minhutBillboards.billboard.BillboardMapRenderer;
import me.revqz.minhutBillboards.billboard.ImageLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BillboardCommand implements CommandExecutor, TabCompleter {

    private final Map<UUID, String> lastSpawnedBillboard = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /billboard spawn <name>", NamedTextColor.RED));
                    listAvailableBillboards(player);
                    return true;
                }
                spawnBillboard(player, args[1]);
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /billboard remove <name>", NamedTextColor.RED));
                    return true;
                }
                removeBillboard(player, args[1]);
            }
            case "undo" -> undoLastBillboard(player);
            case "list" -> listAvailableBillboards(player);
            case "reload" -> reloadConfig(player);
            default -> sendUsage(player);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "remove", "undo", "list", "reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return getConfiguredBillboardNames();
            } else if (args[0].equalsIgnoreCase("remove")) {
                return new ArrayList<>(BillboardManager.getInstance().getBillboardNames());
            }
        }
        return List.of();
    }

    private List<String> getConfiguredBillboardNames() {
        ConfigurationSection billboards = MinhutBillboards.getInstance().getConfig()
                .getConfigurationSection("billboards");
        if (billboards == null) {
            return List.of();
        }
        return new ArrayList<>(billboards.getKeys(false));
    }

    private void listAvailableBillboards(Player player) {
        List<String> names = getConfiguredBillboardNames();
        if (names.isEmpty()) {
            player.sendMessage(Component.text("No billboards configured in config.yml", NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("Available billboards: ", NamedTextColor.GOLD)
                .append(Component.text(String.join(", ", names), NamedTextColor.YELLOW)));
    }

    private void spawnBillboard(Player player, String billboardName) {
        ConfigurationSection config = MinhutBillboards.getInstance().getConfig()
                .getConfigurationSection("billboards." + billboardName);

        if (config == null) {
            player.sendMessage(Component.text("Billboard '" + billboardName + "' not found!", NamedTextColor.RED));
            listAvailableBillboards(player);
            return;
        }

        int wallWidth = config.getInt("width", 6);
        int wallHeight = config.getInt("height", 5);
        String materialName = config.getString("material", "BLACK_CONCRETE");
        String imageSource = config.getString("image", "");

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid material: " + materialName, NamedTextColor.RED));
            material = Material.BLACK_CONCRETE;
        }

        Block targetBlock = player.getTargetBlockExact(50);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Component.text("Look at a block to spawn the billboard there!", NamedTextColor.RED));
            return;
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        BlockFace facing = getDirectionToPlayer(targetLoc, playerLoc);
        BlockFace frameFacing = facing;

        Location startLoc = targetBlock.getLocation().clone();

        BlockFace widthDirection = getWidthDirection(facing);

        int halfWidth = wallWidth / 2;
        startLoc.add(
                widthDirection.getModX() * -halfWidth,
                0,
                widthDirection.getModZ() * -halfWidth);

        List<Location> blockLocations = new ArrayList<>();

        for (int y = 0; y < wallHeight; y++) {
            for (int w = 0; w < wallWidth; w++) {
                Location blockLoc = startLoc.clone().add(
                        widthDirection.getModX() * w,
                        y,
                        widthDirection.getModZ() * w);

                Block block = blockLoc.getBlock();
                block.setType(material);
                blockLocations.add(blockLoc.clone());
            }
        }

        MapView[][] mapViews = new MapView[wallWidth][wallHeight];
        List<ItemFrame> spawnedFrames = new ArrayList<>();

        int framesPlaced = 0;
        for (int y = 0; y < wallHeight; y++) {
            for (int w = 0; w < wallWidth; w++) {
                Location blockLoc = startLoc.clone().add(
                        widthDirection.getModX() * w,
                        y,
                        widthDirection.getModZ() * w);

                Block frontBlock = blockLoc.getBlock().getRelative(frameFacing);
                Location frameLoc = frontBlock.getLocation();

                final int mapX = w;
                final int mapY = y;

                try {
                    ItemFrame frame = player.getWorld().spawn(frameLoc, ItemFrame.class, itemFrame -> {
                        itemFrame.setFacingDirection(frameFacing, true);
                        itemFrame.setVisible(false);
                        itemFrame.setFixed(true);

                        MapView mapView = Bukkit.createMap(player.getWorld());
                        mapViews[mapX][mapY] = mapView;

                        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                        mapMeta.setMapView(mapView);
                        mapItem.setItemMeta(mapMeta);

                        itemFrame.setItem(mapItem);
                    });
                    spawnedFrames.add(frame);
                    framesPlaced++;
                } catch (Exception e) {
                }
            }
        }

        String uniqueName = billboardName + "_" + System.currentTimeMillis();
        BillboardManager.getInstance().registerBillboard(uniqueName, spawnedFrames, blockLocations);
        lastSpawnedBillboard.put(player.getUniqueId(), uniqueName);

        final int finalFramesPlaced = framesPlaced;

        if (imageSource != null && !imageSource.isEmpty()) {
            player.sendMessage(Component.text("⏳ ", NamedTextColor.YELLOW)
                    .append(Component.text("Loading image...", NamedTextColor.GRAY)));

            final int fw = wallWidth;
            final int fh = wallHeight;

            ImageLoader.loadAndProcessImage(imageSource, wallWidth, wallHeight).thenAccept(tiles -> {
                if (tiles == null) {
                    Bukkit.getScheduler().runTask(MinhutBillboards.getInstance(), () -> {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED)
                                .append(Component.text("Failed to load image!", NamedTextColor.GRAY)));
                    });
                    return;
                }

                Bukkit.getScheduler().runTask(MinhutBillboards.getInstance(), () -> {
                    for (int x = 0; x < fw; x++) {
                        for (int y = 0; y < fh; y++) {
                            MapView mapView = mapViews[x][y];
                            if (mapView != null && tiles[x][y] != null) {
                                for (MapRenderer renderer : mapView.getRenderers()) {
                                    mapView.removeRenderer(renderer);
                                }
                                mapView.addRenderer(new BillboardMapRenderer(tiles[x][y]));
                            }
                        }
                    }
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                            .append(Component.text("Image loaded!", NamedTextColor.GRAY)));
                });
            });
        }

        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("Billboard '" + billboardName + "' spawned! ", NamedTextColor.GRAY))
                .append(Component.text("(" + wallWidth + "x" + wallHeight + ", " + finalFramesPlaced + " maps)",
                        NamedTextColor.DARK_GRAY)));
    }

    private BlockFace getDirectionToPlayer(Location target, Location player) {
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();

        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    private BlockFace getWidthDirection(BlockFace facing) {
        return switch (facing) {
            case NORTH, SOUTH -> BlockFace.EAST;
            case EAST, WEST -> BlockFace.SOUTH;
            default -> BlockFace.EAST;
        };
    }

    private void undoLastBillboard(Player player) {
        String lastBillboard = lastSpawnedBillboard.remove(player.getUniqueId());

        if (lastBillboard == null) {
            player.sendMessage(Component.text("No billboard to undo!", NamedTextColor.RED));
            return;
        }

        if (!BillboardManager.getInstance().getBillboardNames().contains(lastBillboard)) {
            player.sendMessage(Component.text("Billboard already removed!", NamedTextColor.RED));
            return;
        }

        BillboardManager.getInstance().removeBillboard(lastBillboard);
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("Billboard undone!", NamedTextColor.GRAY)));
    }

    private void removeBillboard(Player player, String name) {
        Set<String> billboards = BillboardManager.getInstance().getBillboardNames();

        String toRemove = null;
        for (String billboard : billboards) {
            if (billboard.startsWith(name.toLowerCase())) {
                toRemove = billboard;
                break;
            }
        }

        if (toRemove == null) {
            player.sendMessage(Component.text("No billboard found with name: " + name, NamedTextColor.RED));
            return;
        }

        BillboardManager.getInstance().removeBillboard(toRemove);
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("Billboard removed!", NamedTextColor.GRAY)));
    }

    private void reloadConfig(Player player) {
        MinhutBillboards.getInstance().reloadConfig();
        BillboardManager.getInstance().reload();
        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text("Config reloaded!", NamedTextColor.GRAY)));
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Billboard Commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  /billboard spawn <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Spawn at block you're looking at", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /billboard undo", NamedTextColor.YELLOW)
                .append(Component.text(" - Undo last billboard", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /billboard remove <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a spawned billboard", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /billboard list", NamedTextColor.YELLOW)
                .append(Component.text(" - List available billboards", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /billboard reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload config", NamedTextColor.GRAY)));
    }
}
