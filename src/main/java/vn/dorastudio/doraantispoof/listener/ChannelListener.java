package vn.dorastudio.doraantispoof.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import vn.dorastudio.doraantispoof.DoraAntiSpoof;
import vn.dorastudio.doraantispoof.util.BrandDecoder;

public class ChannelListener implements Listener, PluginMessageListener {

    private final DoraAntiSpoof plugin;

    public ChannelListener(DoraAntiSpoof plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(
            String channel,
            Player player,
            byte[] message
    ) {
        if (!channel.equalsIgnoreCase("minecraft:brand")) {
            return;
        }

        String brand = BrandDecoder.decode(message);
        plugin.getDetectionManager().handleBrand(player, brand);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        Player player = event.getPlayer();

        plugin.getDetectionManager().handleChannel(
                player,
                event.getChannel()
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Chờ client gửi brand sau khi kết nối.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDetectionManager().remove(event.getPlayer());
    }
}
