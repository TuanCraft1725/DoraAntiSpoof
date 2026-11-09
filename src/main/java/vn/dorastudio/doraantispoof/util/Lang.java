package vn.dorastudio.doraantispoof.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.dorastudio.doraantispoof.DoraAntiSpoof;

import java.io.File;

public class Lang {

    private final DoraAntiSpoof plugin;
    private YamlConfiguration messages;

    public Lang(DoraAntiSpoof plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getConfig()
                .getString("general.language", "vi");

        String fileName = language.equalsIgnoreCase("en")
                ? "en.lang"
                : "vi.lang";

        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String message = messages.getString(key, "&cMissing message: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String get(String key, String player, String detail) {
        return get(key)
                .replace("%player%", player)
                .replace("%detail%", detail);
    }

    public String prefix() {
        return get("prefix");
    }

    public void send(org.bukkit.command.CommandSender sender, String key) {
        sender.sendMessage(prefix() + get(key));
    }
}
