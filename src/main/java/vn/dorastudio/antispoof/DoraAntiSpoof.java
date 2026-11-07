package vn.dorastudio.antispoof;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerPluginMessageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class DoraAntiSpoof extends JavaPlugin implements Listener {

    private final Map<UUID, String> brands = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> channels = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("================================");
        getLogger().info(" DoraAntiSpoof 1.0.1");
        getLogger().info(" Dora Studio Protection System");
        getLogger().info(" Plugin enabled successfully!");
        getLogger().info("================================");
    }

    @Override
    public void onDisable() {
        brands.clear();
        channels.clear();
        getLogger().info("DoraAntiSpoof disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        channels.put(
            player.getUniqueId(),
            ConcurrentHashMap.newKeySet()
        );

        channels.get(player.getUniqueId())
            .addAll(player.getListeningPluginChannels());

        long delay = Math.max(
            1,
            getConfig().getLong("delay-in-seconds", 5)
        );

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                inspect(player);
            }
        }, delay * 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        brands.remove(uuid);
        channels.remove(uuid);
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        channels.computeIfAbsent(
            event.getPlayer().getUniqueId(),
            key -> ConcurrentHashMap.newKeySet()
        ).add(event.getChannel().toLowerCase(Locale.ROOT));
    }

    @EventHandler
    public void onBrandMessage(PlayerPluginMessageEvent event) {
        String channel = event.getChannel();

        if (!channel.equalsIgnoreCase("minecraft:brand")
            && !channel.equalsIgnoreCase("MC|Brand")) {
            return;
        }

        String brand = readBrand(event.getData());

        if (brand != null && !brand.isBlank()) {
            brands.put(event.getPlayer().getUniqueId(), brand);
            debug("Client brand: " + event.getPlayer().getName()
                + " -> " + brand);
        }
    }

    private String readBrand(byte[] data) {
        try {
            DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(data));

            int length = readVarInt(input);

            if (length < 0 || length > 256) {
                return null;
            }

            byte[] bytes = new byte[length];
            input.readFully(bytes);

            return new String(bytes, StandardCharsets.UTF_8).trim();

        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private int readVarInt(DataInputStream input) throws IOException {
        int value = 0;
        int position = 0;
        byte current;

        do {
            current = input.readByte();
            value |= (current & 0x7F) << position;
            position += 7;

            if (position >= 35) {
                throw new IOException("Invalid VarInt");
            }

        } while ((current & 0x80) != 0);

        return value;
    }

    private void inspect(Player player) {
        String brand = brands.get(player.getUniqueId());

        Set<String> playerChannels = new HashSet<>(
            channels.getOrDefault(player.getUniqueId(), Set.of())
        );

        playerChannels.addAll(player.getListeningPluginChannels());

        if (getConfig().getBoolean("no-brand-check.enabled", true)
            && (brand == null || brand.isBlank())) {

            violation(
                player,
                "no-brand-check",
                "Không nhận được client brand",
                ""
            );
            return;
        }

        if (brand != null) {
            String matched = findBrand(brand);

            if (matched != null) {
                String path = "client-brands." + matched;

                for (String required :
                    getConfig().getStringList(path + ".required-channels")) {

                    if (!playerChannels.contains(
                        required.toLowerCase(Locale.ROOT))) {

                        violation(
                            player,
                            path,
                            "Thiếu channel bắt buộc: " + required,
                            brand
                        );
                        return;
                    }
                }

                if ((matched.equals("vanilla")
                    || matched.equals("vanilla-variation"))
                    && getConfig().getBoolean(
                        "vanillaspoof-check.enabled", true)
                    && hasModdedChannel(playerChannels)) {

                    violation(
                        player,
                        "vanillaspoof-check",
                        "Brand vanilla có channel modded",
                        brand
                    );
                    return;
                }

            } else if (getConfig().getBoolean(
                "non-vanilla-check.enabled", false)
                && getConfig().getBoolean(
                    "non-vanilla-check.strict-check", false)) {

                violation(
                    player,
                    "non-vanilla-check",
                    "Client brand không được nhận diện",
                    brand
                );
                return;
            }
        }

        if (getConfig().getBoolean("blocked-channels.enabled", true)) {
            for (String channel : playerChannels) {
                if (isBlocked(channel)) {
                    violation(
                        player,
                        "blocked-channels",
                        "Channel đáng ngờ: " + channel,
                        brand == null ? "" : brand
                    );
                    return;
                }
            }
        }
    }

    private String findBrand(String brand) {
        if (!getConfig().isConfigurationSection("client-brands")) {
            return null;
        }

        for (String key : Objects.requireNonNull(
            getConfig().getConfigurationSection("client-brands")
        ).getKeys(false)) {

            if (key.equals("defaults")) continue;

            String path = "client-brands." + key;

            if (!getConfig().getBoolean(path + ".enabled", true)) {
                continue;
            }

            String regex = getConfig().getString(path + ".regex", "");

            try {
                if (!regex.isEmpty()
                    && Pattern.compile(regex).matcher(brand).matches()) {
                    return key;
                }
            } catch (PatternSyntaxException exception) {
                getLogger().warning("Regex không hợp lệ: " + path);
            }
        }

        return null;
    }

    private boolean hasModdedChannel(Set<String> playerChannels) {
        for (String channel : playerChannels) {
            String lower = channel.toLowerCase(Locale.ROOT);

            if (lower.startsWith("fabric:")
                || lower.startsWith("forge:")
                || lower.startsWith("fml:")
                || lower.startsWith("labymod:")) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlocked(String channel) {
        String mode = getConfig().getString(
            "blocked-channels.whitelist-mode", "SIMPLE"
        );

        if (mode == null || mode.equalsIgnoreCase("FALSE")) {
            return false;
        }

        for (String regex :
            getConfig().getStringList(
                "blocked-channels.modifiedchannels")) {

            try {
                if (Pattern.compile(
                    regex,
                    Pattern.CASE_INSENSITIVE
                ).matcher(channel).matches()) {
                    return true;
                }
            } catch (PatternSyntaxException exception) {
                getLogger().warning(
                    "Blocked channel regex không hợp lệ: " + regex
                );
            }
        }

        return false;
    }

    private void violation(
        Player player,
        String path,
        String reason,
        String brand
    ) {
        boolean alert = getConfig().getBoolean(
            path + ".alert",
            getConfig().getBoolean(
                "client-brands.defaults.alert", true
            )
        );

        if (alert && getConfig().getBoolean(
            "global-alerts.enabled", true)) {

            String format = getConfig().getString(
                "global-alerts.format",
                "&8[&b✦ DORA ANTISPOOF&8] &f{player} &7- &e{reason}"
            );

            String message = format
                .replace("{player}", player.getName())
                .replace("{reason}", reason)
                .replace("{brand}", brand);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("doraantispoof.alerts")) {
                    online.sendMessage(color(message));
                }
            }
        }

        boolean punish = getConfig().getBoolean(
            path + ".punish",
            getConfig().getBoolean(
                "client-brands.defaults.punish", false
            )
        );

        if (punish && getConfig().getBoolean(
            "punishments.kick.enabled", false)) {

            player.kickPlayer(color(
                getConfig().getString(
                    "punishments.kick.message",
                    "&cKết nối bị từ chối."
                )
            ));

        } else if (punish && getConfig().getBoolean(
            "punishments.command.enabled", false)) {

            String command = getConfig().getString(
                "punishments.command.value", ""
            );

            if (command != null && !command.isBlank()) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{player}", player.getName())
                );
            }
        }
    }

    private void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
            '&',
            text == null ? "" : text
        );
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (!sender.hasPermission("doraantispoof.admin")) {
            sender.sendMessage(color(
                getConfig().getString(
                    "messages.no-permission",
                    "&cBạn không có quyền."
                )
            ));
            return true;
        }

        if (args.length == 1
            && args[0].equalsIgnoreCase("reload")) {

            reloadConfig();

            sender.sendMessage(color(
                getConfig().getString(
                    "messages.reloaded",
                    "&aĐã tải lại cấu hình."
                )
            ));
            return true;
        }

        if (args.length == 1
            && args[0].equalsIgnoreCase("info")) {

            sender.sendMessage(color(
                getConfig().getString(
                    "messages.info",
                    "&bDoraAntiSpoof 1.0.1"
                )
            ));
            return true;
        }

        sender.sendMessage(color(
            getConfig().getString(
                "messages.usage",
                "&c/doraantispoof <reload|info>"
            )
        ));

        return true;
    }
}
