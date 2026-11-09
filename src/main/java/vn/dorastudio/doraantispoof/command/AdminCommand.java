package vn.dorastudio.doraantispoof.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import vn.dorastudio.doraantispoof.DoraAntiSpoof;

public class AdminCommand implements CommandExecutor {

    private final DoraAntiSpoof plugin;

    public AdminCommand(DoraAntiSpoof plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("doraantispoof.admin")) {
            plugin.getLang().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§8§m------------------------------");
            sender.sendMessage("§b§lDoraAntiSpoof §7v1.0.1");
            sender.sendMessage("§f/doraantispoof reload");
            sender.sendMessage("§f/doraantispoof brand <player>");
            sender.sendMessage("§8§m------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            plugin.getLang().send(sender, "reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("brand")) {
            if (args.length < 2) {
                plugin.getLang().send(sender, "usage");
                return true;
            }

            org.bukkit.entity.Player target =
                    plugin.getServer().getPlayerExact(args[1]);

            if (target == null) {
                plugin.getLang().send(sender, "player-not-found");
                return true;
            }

            sender.sendMessage(
                    "§b" + target.getName() + " §7Client brand: §f"
                            + plugin.getDetectionManager().getBrand(target)
            );
            return true;
        }

        plugin.getLang().send(sender, "usage");
        return true;
    }
}
