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

public final class HordeTeleportCommand
extends AbstractPlayerCommand {
    private final HordeService hordeService;

    public HordeTeleportCommand(@Nonnull String name, @Nonnull String description, HordeService hordeService) {
        super(name, description);
        this.hordeService = hordeService;
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.hordeService.isPluginReloadInProgress()) {
            this.sendLocalized(playerRef, "Plugin reload in progress. Try again in a few seconds.", "Recarga del plugin en progreso. Prueba de nuevo en unos segundos.");
            return;
        }
        this.sendLocalized(playerRef, this.hordeService.teleportPlayerToSelectedArena(playerRef, world).getMessage());
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

