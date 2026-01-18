package me.revqz.minhutBillboards.billboard;

import me.revqz.minhutBillboards.MinhutBillboards;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class BillboardManager {

    private static BillboardManager instance;

    private final Map<String, BillboardData> billboards = new HashMap<>();
    private final Map<UUID, String> frameToBoard = new HashMap<>();
    private final Map<UUID, String> playerLookingAt = new HashMap<>();

    private BukkitTask glowTask;
    private Team glowTeam;

    private static final String BILLBOARD_TAG = "minhut_billboard";

    private BillboardManager() {
        setupGlowTeam();
        startGlowTask();
    }

    public static BillboardManager getInstance() {
        if (instance == null) {
            instance = new BillboardManager();
        }
        return instance;
    }

    private void setupGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team oldTeam = scoreboard.getTeam("billboard_glow");
        if (oldTeam != null) {
            oldTeam.unregister();
        }

        glowTeam = scoreboard.registerNewTeam("billboard_glow");
        glowTeam.color(NamedTextColor.GREEN);
        glowTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private void startGlowTask() {
        if (glowTask != null) {
            glowTask.cancel();
        }

        boolean glowEnabled = MinhutBillboards.getInstance().getConfig().getBoolean("effects.glow_when_looking", true);
        if (!glowEnabled) {
            return;
        }

        int range = MinhutBillboards.getInstance().getConfig().getInt("effects.look_range", 32);

        glowTask = Bukkit.getScheduler().runTaskTimer(MinhutBillboards.getInstance(), () -> {
            if (billboards.isEmpty())
                return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerLooking(player, range);
            }
        }, 5L, 5L);
    }

    private void checkPlayerLooking(Player player, int range) {
        String currentlyLookingAt = playerLookingAt.get(player.getUniqueId());
        String nowLookingAt = null;

        Block targetBlock = player.getTargetBlockExact(range);
        if (targetBlock != null) {
            for (Entity entity : targetBlock.getWorld().getNearbyEntities(
                    targetBlock.getLocation().add(0.5, 0.5, 0.5), 2.5, 2.5, 2.5)) {
                if (entity instanceof ItemFrame frame && frame.getScoreboardTags().contains(BILLBOARD_TAG)) {
                    String billboard = frameToBoard.get(frame.getUniqueId());
                    if (billboard != null) {
                        nowLookingAt = billboard;
                        break;
                    }
                }
            }
        }

        if (!Objects.equals(currentlyLookingAt, nowLookingAt)) {
            if (currentlyLookingAt != null) {
                setGlowState(currentlyLookingAt, false);
            }
            if (nowLookingAt != null) {
                setGlowState(nowLookingAt, true);
            }

            if (nowLookingAt != null) {
                playerLookingAt.put(player.getUniqueId(), nowLookingAt);
            } else {
                playerLookingAt.remove(player.getUniqueId());
            }
        }
    }

    private void setGlowState(String billboardName, boolean glowing) {
        BillboardData data = billboards.get(billboardName);
        if (data == null)
            return;

        for (UUID frameId : data.frameIds) {
            Entity entity = Bukkit.getEntity(frameId);
            if (entity instanceof ItemFrame frame && frame.isValid()) {
                frame.setGlowing(glowing);

                String entry = frame.getUniqueId().toString();
                try {
                    if (glowing) {
                        if (!glowTeam.hasEntry(entry)) {
                            glowTeam.addEntry(entry);
                        }
                    } else {
                        if (glowTeam.hasEntry(entry)) {
                            glowTeam.removeEntry(entry);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public void registerBillboard(String name, List<ItemFrame> frames, List<Location> blockLocations) {
        String lowerName = name.toLowerCase();
        BillboardData data = new BillboardData();

        for (ItemFrame frame : frames) {
            UUID id = frame.getUniqueId();
            data.frameIds.add(id);
            frameToBoard.put(id, lowerName);

            frame.addScoreboardTag(BILLBOARD_TAG);
        }

        data.blockLocations.addAll(blockLocations);
        billboards.put(lowerName, data);

        MinhutBillboards.getInstance().getLogger()
                .info("Registered billboard: " + lowerName + " with " + frames.size() + " frames");
    }

    public void removeBillboard(String name) {
        String lowerName = name.toLowerCase();
        BillboardData data = billboards.remove(lowerName);

        if (data == null)
            return;

        setGlowState(lowerName, false);

        for (UUID frameId : data.frameIds) {
            frameToBoard.remove(frameId);
            Entity entity = Bukkit.getEntity(frameId);
            if (entity != null) {
                entity.remove();
            }
        }

        for (Location loc : data.blockLocations) {
            Block block = loc.getBlock();
            block.setType(Material.AIR);
        }

        playerLookingAt.values().removeIf(board -> board.equals(lowerName));
    }

    public Set<String> getBillboardNames() {
        return new HashSet<>(billboards.keySet());
    }

    public boolean billboardExists(String name) {
        return billboards.containsKey(name.toLowerCase());
    }

    public void shutdown() {
        if (glowTask != null) {
            glowTask.cancel();
        }

        for (String billboard : billboards.keySet()) {
            setGlowState(billboard, false);
        }

        billboards.clear();
        frameToBoard.clear();
        playerLookingAt.clear();
    }

    public void reload() {
        setupGlowTeam();
        startGlowTask();
    }

    private static class BillboardData {
        final List<UUID> frameIds = new ArrayList<>();
        final List<Location> blockLocations = new ArrayList<>();
    }
}
