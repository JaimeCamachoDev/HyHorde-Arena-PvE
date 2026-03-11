package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.Locale;
import javax.annotation.Nonnull;

public final class HordeReloadCommand
extends AbstractPlayerCommand {
    private final PluginBase plugin;
    private final HordeService hordeService;
    private final OptionalArg<String> modeArg;

    public HordeReloadCommand(@Nonnull String name, @Nonnull String description, PluginBase plugin, HordeService hordeService) {
        super(name, description);
        this.plugin = plugin;
        this.hordeService = hordeService;
        this.modeArg = this.withOptionalArg("modo", "modo", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        boolean english = HordeService.isEnglishLanguage(this.hordeService.getLanguage());
        String mode;
        switch (mode = commandContext.provided(this.modeArg) ? ((String)commandContext.get(this.modeArg)).toLowerCase(Locale.ROOT) : "config") {
            case "config": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.reloadConfigFromDisk().getMessage()));
                return;
            }
            case "mod": 
            case "jar": 
            case "plugin": {
                playerRef.sendMessage(Message.raw((String)(english ? "Hot-reload of .jar mods is not supported. Replace the file and restart the server." : "No se soporta recarga en caliente de mods .jar. Reemplaza el archivo y reinicia el servidor.")));
                return;
            }
        }
        playerRef.sendMessage(Message.raw((String)(english ? "Usage: /hordareload [config]" : "Uso: /hordareload [config]")));
        playerRef.sendMessage(Message.raw((String)(english ? "config: reload horde-config.json + enemy-categories.json" : "config: recarga horde-config.json + enemy-categories.json")));
        playerRef.sendMessage(Message.raw((String)(english ? "mod/jar/plugin: requires server restart after replacing the .jar" : "mod/jar/plugin: requiere reiniciar el servidor tras reemplazar el .jar")));
    }
}


