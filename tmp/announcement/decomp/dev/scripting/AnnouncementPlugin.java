/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.plugin.JavaPluginInit
 *  javax.annotation.Nonnull
 */
package dev.scripting;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.scripting.commands.AnnouncementCommand;
import dev.scripting.config.ConfigManager;
import dev.scripting.events.EnterAnnouncementEvent;
import java.io.File;
import javax.annotation.Nonnull;

public class AnnouncementPlugin
extends JavaPlugin {
    public AnnouncementPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        this.getCommandRegistry().registerCommand((AbstractCommand)new AnnouncementCommand("an", "Make an announcement"));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, EnterAnnouncementEvent::onPlayerReady);
        File pluginDir = this.getDataDirectory().toFile();
        ConfigManager.load(pluginDir);
    }
}

