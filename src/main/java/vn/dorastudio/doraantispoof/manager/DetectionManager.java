package vn.dorastudio.doraantispoof.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vn.dorastudio.doraantispoof.DoraAntiSpoof;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class DetectionManager {

    private final DoraAntiSpoof plugin;
    private final Map<UUID, String> brands = new ConcurrentHashMap<>();
    private final Set<UUID> checked = ConcurrentHashMap.newKeySet();

    public DetectionManager(DoraAntiSpoof plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("checks.brand.enabled", true)) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID id = player.getUniqueId();

                if (!checked.contains(id)) {
                    checked.add(id);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || brands.containsKey(id)) {
                            return;
                        }

                        if (plugin.getConfig().getBoolean(
                                "checks.brand.flag-missing-brand", false)) {
                            flag(player, "Không nhận được client brand");
                        }
                    }, 100L);
                }
            }
        }, 20L, 20L);
    }

    public void handleBrand(Player player, String brand) {
        if (brand == null || brand.isBlank()) {
            flag(player, "Client brand trống");
            return;
        }

        brands.put(player.getUniqueId(), brand);

        if (!plugin.getConfig().getBoolean("checks.brand.enabled", true)) {
            return;
        }

        if (plugin.getConfig().getBoolean("checks.brand.bedrock-exemption", true)
                && matchesAny(brand, plugin.getConfig().getStringList(
                "checks.brand.bedrock-brands"))) {
            return;
        }

        for (String blocked : plugin.getConfig().getStringList(
                "checks.brand.blocked-brands")) {
            try {
                if (Pattern.compile(blocked, Pattern.CASE_INSENSITIVE)
                        .matcher(brand).find()) {
                    flag(player, "Client brand bị chặn: " + brand);
                    return;
                }
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Regex không hợp lệ trong config: " + blocked
                );
            }
        }

        if (plugin.getConfig().getBoolean("checks.brand.flag-blank", true)
                && brand.trim().isEmpty()) {
            flag(player, "Client brand trống");
        }
    }

    private boolean matchesAny(String value, java.util.List<String> patterns) {
        for (String pattern : patterns) {
            try {
                if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                        .matcher(value).find()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public void handleChannel(Player player, String channel) {
        if (!plugin.getConfig().getBoolean("checks.channels.enabled", true)) {
            return;
        }

        for (String blocked : plugin.getConfig().getStringList(
                "checks.channels.blocked")) {
            try {
                if (Pattern.compile(blocked, Pattern.CASE_INSENSITIVE)
                        .matcher(channel).matches()) {
                    flag(player, "Plugin channel bất thường: " + channel);
                    return;
                }
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Regex channel không hợp lệ: " + blocked
                );
            }
        }
    }

    public void flag(Player player, String reason) {
        String message = plugin.getLang().get(
                "alert",
                player.getName(),
                reason
        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("doraantispoof.alert")) {
                online.sendMessage(plugin.getLang().prefix() + message);
            }
        }

        plugin.getLogger().warning(
                player.getName() + " - " + reason
        );

        if (plugin.getConfig().getBoolean("punishments.kick", false)) {
            player.kickPlayer(plugin.getLang().get("kick-message"));
        }
    }

    public void remove(Player player) {
        brands.remove(player.getUniqueId());
        checked.remove(player.getUniqueId());
    }

    public String getBrand(Player player) {
        return brands.getOrDefault(player.getUniqueId(), "Unknown");
    }
                  }
