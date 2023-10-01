package de.maxhenkel.voicechat_broadcast;

import com.github.puregero.multilib.MultiLib;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

public final class VoicechatBroadcast extends JavaPlugin {

    public static final String PLUGIN_ID = "voicechat_broadcast";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);

    @Nullable
    private BroadcastVoicechatPlugin voicechatPlugin;

    @Override
    public void onEnable() {
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new BroadcastVoicechatPlugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voice chat broadcast plugin");
        } else {
            LOGGER.info("Failed to register voice chat broadcast plugin");
        }

        MultiLib.on(this, "voicechat_broadcast:broadcast_server", (data) -> {
            VoicechatServerApi api = BroadcastVoicechatPlugin.getApi();
            SoundPacket packet = api.externalDecodeSoundPacket(data);
            for (Player onlinePlayer : MultiLib.getLocalOnlinePlayers()) {
                if (!MultiLib.isLocalPlayer(onlinePlayer)) {
                    continue;
                }

                VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUniqueId());
                // Check if the player is actually connected to the voice chat
                if (connection == null) {
                    continue;
                }

                // Send a static audio packet of the microphone data to the connection of each player
                api.sendStaticSoundPacketTo(connection, packet.toStaticSoundPacket());
            }
        });
    }

    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voice chat broadcast plugin");
        }
    }
}
