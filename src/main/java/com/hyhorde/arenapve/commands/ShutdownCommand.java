package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import javax.annotation.Nonnull;

public class ShutdownCommand
extends CommandBase {
    public ShutdownCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    protected void executeSync(@Nonnull CommandContext commandContext) {
        commandContext.sendMessage(Message.raw((String)"Apagando servidor..."));
        HytaleServer.get().shutdownServer();
    }
}


