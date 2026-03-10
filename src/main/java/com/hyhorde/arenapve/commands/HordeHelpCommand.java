package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyhorde.arenapve.horde.HordeService;
import javax.annotation.Nonnull;

public final class HordeHelpCommand
extends AbstractPlayerCommand {
    private final HordeService hordeService;

    public HordeHelpCommand(@Nonnull String name, @Nonnull String description, HordeService hordeService) {
        super(name, description);
        this.hordeService = hordeService;
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        boolean english = HordeService.isEnglishLanguage(this.hordeService.getLanguage());
        playerRef.sendMessage(Message.raw((String)(english ? "[Horde PVE] Help" : "[Horda PVE] Ayuda")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordahelp -> show this help" : "/hordahelp -> muestra esta ayuda")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve -> open configuration (aliases: /hordepve /spawnve /spawnpve)" : "/hordapve -> abre la configuracion (alias: /hordepve /spawnve /spawnpve)")));
        playerRef.sendMessage(Message.raw((String)"/hordapve start | stop | status | logs | setspawn | reload"));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve enemy <category> | enemytypes" : "/hordapve enemy <categoria> | tipos")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve role <npcRole|auto> | roles" : "/hordapve role <rolNpc|auto> | roles")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve reward <rounds>" : "/hordapve reward <rondas>")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve spectator <on|off> | player" : "/hordapve spectator <on|off> | jugador")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordapve arearadius <blocks>" : "/hordapve arearadius <bloques>")));
        playerRef.sendMessage(Message.raw((String)(english ? "/hordareload [config] (mod/jar requires restart)" : "/hordareload [config] (mod/jar requiere reinicio)")));
    }
}
