package me.revqz.minhutBillboards;

import me.revqz.minhutBillboards.billboard.BillboardManager;
import me.revqz.minhutBillboards.commands.BillboardCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MinhutBillboards extends JavaPlugin {

    private static MinhutBillboards instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        File imagesFolder = new File(getDataFolder(), "images");
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
            getLogger().info("Created images folder at: " + imagesFolder.getAbsolutePath());
        }

        BillboardManager.getInstance();

        BillboardCommand billboardCommand = new BillboardCommand();
        getCommand("billboard").setExecutor(billboardCommand);
        getCommand("billboard").setTabCompleter(billboardCommand);

        getLogger().info("MinhutBillboards enabled!");
    }

    @Override
    public void onDisable() {
        BillboardManager.getInstance().shutdown();
        getLogger().info("MinhutBillboards disabled!");
    }

    public static MinhutBillboards getInstance() {
        return instance;
    }
}
