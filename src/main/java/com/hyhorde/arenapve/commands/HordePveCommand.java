package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyhorde.arenapve.horde.HordeConfigPage;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;

public class HordePveCommand
extends AbstractPlayerCommand {
    private final HordeService hordeService;
    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> valueArg;

    public HordePveCommand(@Nonnull String name, @Nonnull String description, HordeService hordeService) {
        super(name, description);
        this.hordeService = hordeService;
        this.actionArg = this.withOptionalArg("accion", "accion", (ArgumentType)ArgTypes.STRING);
        this.valueArg = this.withOptionalArg("valor", "valor", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String action;
        switch (action = commandContext.provided(this.actionArg) ? ((String)commandContext.get(this.actionArg)).toLowerCase(Locale.ROOT) : "menu") {
            case "menu": 
            case "ui": {
                this.openUi(store, ref, playerRef);
                return;
            }
            case "start": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.start(store, playerRef, world).getMessage()));
                return;
            }
            case "stop": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.stop(true).getMessage()));
                return;
            }
            case "status": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.getStatusLine()));
                return;
            }
            case "hud": 
            case "panel": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.openStatusHud(ref, store, playerRef, world).getMessage()));
                return;
            }
            case "setspawn": 
            case "spawn": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.setSpawnFromPlayer(playerRef, world).getMessage()));
                return;
            }
            case "role": {
                this.handleRole(commandContext, playerRef);
                return;
            }
            case "roles": {
                this.handleRoles(playerRef);
                return;
            }
            case "reward": {
                this.handleReward(commandContext, playerRef);
                return;
            }
        }
        this.sendHelp(playerRef);
    }

    private void openUi(Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            playerRef.sendMessage(Message.raw((String)"No se pudo abrir la interfaz ahora mismo. Prueba /hordapve help."));
            return;
        }
        HordeConfigPage.open(playerEntityRef, store, player, playerRef, this.hordeService);
    }

    private void handleRole(CommandContext commandContext, PlayerRef playerRef) {
        if (!commandContext.provided(this.valueArg)) {
            playerRef.sendMessage(Message.raw((String)"Uso: /hordapve role <nombre-rol|auto>"));
            return;
        }
        String role = (String)commandContext.get(this.valueArg);
        playerRef.sendMessage(Message.raw((String)this.hordeService.setConfiguredRole(role).getMessage()));
    }

    private void handleRoles(PlayerRef playerRef) {
        List<String> roles = this.hordeService.getAvailableRoles();
        if (roles.isEmpty()) {
            playerRef.sendMessage(Message.raw((String)"No hay roles NPC disponibles en este servidor."));
            return;
        }
        int previewCount = Math.min(25, roles.size());
        String preview = String.join((CharSequence)", ", roles.subList(0, previewCount));
        String suffix = roles.size() > previewCount ? " ... +" + (roles.size() - previewCount) + " mas" : "";
        playerRef.sendMessage(Message.raw((String)("Roles NPC (" + roles.size() + "): " + preview + suffix)));
    }

    private void sendHelp(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw((String)"Comandos de horda PVE:"));
        playerRef.sendMessage(Message.raw((String)"/hordapve -> abre interfaz"));
        playerRef.sendMessage(Message.raw((String)"/hordapve start|stop|status"));
        playerRef.sendMessage(Message.raw((String)"/hordapve hud -> abre panel de estado en vivo"));
        playerRef.sendMessage(Message.raw((String)"/hordapve reward <x> -> recompensa cada x rondas"));
        playerRef.sendMessage(Message.raw((String)"/hordapve setspawn -> guarda tu posicion como centro"));
        playerRef.sendMessage(Message.raw((String)"/hordapve role <rol|auto>"));
        playerRef.sendMessage(Message.raw((String)"/hordapve roles -> lista roles disponibles"));
    }

    private void handleReward(CommandContext commandContext, PlayerRef playerRef) {
        int everyRounds;
        if (!commandContext.provided(this.valueArg)) {
            playerRef.sendMessage(Message.raw((String)"Uso: /hordapve reward <rondas>"));
            return;
        }
        String raw = (String)commandContext.get(this.valueArg);
        try {
            everyRounds = Integer.parseInt(raw.trim());
        }
        catch (Exception ex) {
            playerRef.sendMessage(Message.raw((String)"El valor de reward debe ser un numero entero positivo."));
            return;
        }
        playerRef.sendMessage(Message.raw((String)this.hordeService.setRewardEveryRounds(everyRounds).getMessage()));
    }
}


