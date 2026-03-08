package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class HordeHelpCommand
extends AbstractPlayerCommand {
    public HordeHelpCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        playerRef.sendMessage(Message.raw((String)"[Horda PVE] Ayuda"));
        playerRef.sendMessage(Message.raw((String)"/hordahelp -> muestra esta ayuda"));
        playerRef.sendMessage(Message.raw((String)"/hordapve -> abre la configuracion (alias: /hordepve)"));
        playerRef.sendMessage(Message.raw((String)"/hordapve start | stop | status | logs | setspawn"));
        playerRef.sendMessage(Message.raw((String)"/hordapve enemy <tipo> | tipos"));
        playerRef.sendMessage(Message.raw((String)"/hordapve role <rolNpc|auto> | roles"));
        playerRef.sendMessage(Message.raw((String)"/hordapve reward <rondas>"));
        playerRef.sendMessage(Message.raw((String)"/hordareload config"));
    }
}
