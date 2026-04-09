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
        this.sendLocalized(playerRef, "[Horde PVE] Help", "[Horda PVE] Ayuda");
        this.sendLocalized(playerRef, "/hordahelp -> show this help", "/hordahelp -> muestra esta ayuda");
        this.sendLocalized(playerRef, "/hordeconfig -> open configuration (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)", "/hordeconfig -> abre la configuracion (alias: /hconfig /hordecfg /hordepve /spawnve /spawnpve)");
        this.sendLocalized(playerRef, "/hordeconfig start | stop | status | logs | setspawn | reload");
        this.sendLocalized(playerRef, "/hordeconfig enemy <category> | enemytypes", "/hordeconfig enemy <categoria> | tipos");
        this.sendLocalized(playerRef, "/hordeconfig role <npcRole|auto> | roles", "/hordeconfig role <rolNpc|auto> | roles");
        this.sendLocalized(playerRef, "/hordeconfig reward <rounds>", "/hordeconfig reward <rondas>");
        this.sendLocalized(playerRef, "/hordeconfig spectator <on|off> | player", "/hordeconfig spectator <on|off> | jugador");
        this.sendLocalized(playerRef, "/hordeconfig arearadius <blocks>", "/hordeconfig arearadius <bloques>");
        this.sendLocalized(playerRef, "/hordareload [config] (mod/jar requires restart)", "/hordareload [config] (mod/jar requiere reinicio)");
    }

    private void sendLocalized(PlayerRef playerRef, String text) {
        if (playerRef == null) {
            return;
        }
        playerRef.sendMessage(Message.raw((String)com.hyhorde.arenapve.horde.HordeI18n.translateLegacy(this.hordeService.getLanguage(), text)));
    }

    private void sendLocalized(PlayerRef playerRef, String englishText, String spanishText) {
        if (playerRef == null) {
            return;
        }
        playerRef.sendMessage(Message.raw((String)com.hyhorde.arenapve.horde.HordeI18n.translateUi(this.hordeService.getLanguage(), englishText, spanishText)));
    }
}
