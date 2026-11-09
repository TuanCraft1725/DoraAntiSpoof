package vn.dorastudio.doraantispoof;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vn.dorastudio.doraantispoof.command.AdminCommand;
import vn.dorastudio.doraantispoof.listener.ChannelListener;
import vn.dorastudio.doraantispoof.manager.DetectionManager;
import vn.dorastudio.doraantispoof.util.Lang;

public final class DoraAntiSpoof extends JavaPlugin {

    private static DoraAntiSpoof instance;
    private DetectionManager detectionManager;
    private Lang lang;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("vi.lang", false);
        saveResource("en.lang", false);

        lang = new Lang(this);
        detectionManager = new DetectionManager(this);

        getServer().getPluginManager().registerEvents(
                new ChannelListener(this), this
        );

        getServer().getMessenger().registerIncomingPluginChannel(
                this, "minecraft:brand", new ChannelListener(this)
        );

        if (getCommand("doraantispoof") != null) {
            getCommand("doraantispoof").setExecutor(new AdminCommand(this));
        }

        getLogger().info("DoraAntiSpoof v1.0.1 đã được bật!");
        getLogger().info("Ngôn ngữ: " + getConfig().getString("general.language", "vi"));
    }

    @Override
    public void onDisable() {
        getLogger().info("DoraAntiSpoof đã tắt.");
        instance = null;
    }

    public static DoraAntiSpoof getInstance() {
        return instance;
    }

    public DetectionManager getDetectionManager() {
        return detectionManager;
    }

    public Lang getLang() {
        return lang;
    }

    public void reloadPlugin() {
        reloadConfig();
        lang.reload();
    }
}
