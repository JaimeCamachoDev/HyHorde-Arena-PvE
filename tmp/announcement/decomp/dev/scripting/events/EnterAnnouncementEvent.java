/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.util.EventTitleUtil
 */
package dev.scripting.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.scripting.config.ConfigManager;
import java.util.Properties;

public class EnterAnnouncementEvent {
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = player.getPlayerRef();
        Properties cfg = ConfigManager.get();
        String title = cfg.getProperty("title", "Welcome to the server!");
        String subtitle = cfg.getProperty("subtitle", "Your Server");
        EventTitleUtil.showEventTitleToPlayer((PlayerRef)playerRef, (Message)Message.raw((String)title), (Message)Message.raw((String)subtitle), (boolean)true);
    }
}

