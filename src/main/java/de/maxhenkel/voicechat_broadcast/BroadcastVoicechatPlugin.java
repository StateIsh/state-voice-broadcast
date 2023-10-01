package de.maxhenkel.voicechat_broadcast;

import com.github.puregero.multilib.MultiLib;
import com.github.puregero.multilib.MultiLibImpl;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.UUID;

public class BroadcastVoicechatPlugin implements VoicechatPlugin {
    /**
     * Only OPs have the broadcast permission by default
     */
    public static Permission BROADCAST_PERMISSION = new Permission("voicechat_broadcast.broadcast", PermissionDefault.OP);
    private static VoicechatServerApi api;

    public static VoicechatServerApi getApi() {
        return api;
    }

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return VoicechatBroadcast.PLUGIN_ID;
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        BroadcastVoicechatPlugin.api = (VoicechatServerApi) api;
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    /**
     * This method is called whenever a player sends audio to the server via the voice chat.
     *
     * @param event the microphone packet event
     */
    private void onMicrophone(MicrophonePacketEvent event) {
        // The connection might be null if the event is caused by other means
        if (event.getSenderConnection() == null) {
            return;
        }
        // Cast the generic player object of the voice chat API to an actual bukkit player
        // This object should always be a bukkit player object on bukkit based servers
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player player)) {
            return;
        }

        // Check if the player has the broadcast permission
        if (!player.hasPermission(BROADCAST_PERMISSION)) {
            return;
        }

        Group group = event.getSenderConnection().getGroup();

        // Check if the player sending the audio is actually in a group
        if (group == null) {
            return;
        }

        // Only broadcast the voice when the group name is "broadcast"
        if (!group.getName().strip().equalsIgnoreCase("broadcast")) {
            return;
        }

        // Cancel the actual microphone packet event that people in that group or close by don't hear the broadcaster twice
        event.cancel();

        VoicechatServerApi api = event.getVoicechat();

        MultiLib.notify("voicechat_broadcast:broadcast_server", api.externalEncodeSoundPacket(null, UUID.randomUUID(), event.getPacket().toStaticSoundPacket(), SoundPacketEvent.SOURCE_PLUGIN));

        // Iterating over every player on the server
        for (Player onlinePlayer : MultiLib.getLocalOnlinePlayers()) {
            // Don't send the audio to the player that is broadcasting
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (!MultiLib.isLocalPlayer(onlinePlayer)) {
                continue;
            }

                VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUniqueId());
                // Check if the player is actually connected to the voice chat
                if (connection == null) {
                    continue;
                }

                // Send a static audio packet of the microphone data to the connection of each player
                api.sendStaticSoundPacketTo(connection, event.getPacket().toStaticSoundPacket());
        }

        //  String serverName = MultiLib.getExternalServerName(onlinePlayer);
        //  api.encodeSoundPacket(serverName, onlinePlayer.getUniqueId(), event.getPacket().toStaticSoundPacket(), SoundPacketEvent.SOURCE_PLUGIN);
    }
}
