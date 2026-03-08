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
import com.hyhorde.arenapve.horde.HordeHelpPage;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.Locale;
import javax.annotation.Nonnull;

public final class HordeHelpCommand
extends AbstractPlayerCommand {
    private final HordeService hordeService;
    private final OptionalArg<String> modeArg;

    public HordeHelpCommand(@Nonnull String name, @Nonnull String description, HordeService hordeService) {
        super(name, description);
        this.hordeService = hordeService;
        this.modeArg = this.withOptionalArg("modo", "modo", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String mode = commandContext.provided(this.modeArg) ? ((String)commandContext.get(this.modeArg)).toLowerCase(Locale.ROOT) : "ui";
        if ("chat".equals(mode) || "texto".equals(mode) || "text".equals(mode)) {
            HordeHelpCommand.sendChatHelp(playerRef, this.hordeService);
            return;
        }
        this.openHelpUi(store, ref, playerRef);
    }

    private void openHelpUi(Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            playerRef.sendMessage(Message.raw((String)"No se pudo abrir la ventana de ayuda, mostrando guia en chat."));
            HordeHelpCommand.sendChatHelp(playerRef, this.hordeService);
            return;
        }
        HordeHelpPage.open(playerEntityRef, store, player, playerRef, this.hordeService);
    }

    public static void sendChatHelp(PlayerRef playerRef) {
        HordeHelpCommand.sendChatHelp(playerRef, null);
    }

    public static void sendChatHelp(PlayerRef playerRef, HordeService hordeService) {
        String language = hordeService == null ? "es" : hordeService.getLanguage();
        String availableTypes = "auto, random, bandit, goblin, skeleton, zombie, spider, wolf, slime, beetle";
        if (hordeService != null) {
            String resolved = String.join(", ", hordeService.getEnemyTypeOptionsForCurrentRoles());
            if (!resolved.isBlank()) {
                availableTypes = resolved;
            }
        }
        if (HordeService.isEnglishLanguage(language)) {
            playerRef.sendMessage(Message.raw((String)"[Horde PVE] Quick guide"));
            playerRef.sendMessage(Message.raw((String)"1) /hordapve -> opens config menu"));
            playerRef.sendMessage(Message.raw((String)"2) /hordapve setspawn -> saves horde center"));
            playerRef.sendMessage(Message.raw((String)"3) Adjust rounds, enemies and rewards in the UI, then save"));
            playerRef.sendMessage(Message.raw((String)"4) /hordapve start -> starts the horde"));
            playerRef.sendMessage(Message.raw((String)"5) /hordapve stop -> stops the horde"));
            playerRef.sendMessage(Message.raw((String)"Commands: /horda help, /hordapve status, /hordapve logs, /hordapve hud, /hordapve enemy <type>, /hordapve tipos, /hordapve role <npcRole|auto>, /hordapve roles, /hordapve reward <rounds>, /hordareload config"));
            playerRef.sendMessage(Message.raw((String)"Compatible alias: /hordepve (same subcommands as /hordapve)."));
            playerRef.sendMessage(Message.raw((String)("Types: " + availableTypes)));
            return;
        }
        playerRef.sendMessage(Message.raw((String)"[Horda PVE] Guia rapida"));
        playerRef.sendMessage(Message.raw((String)"1) /hordapve -> abre menu de configuracion"));
        playerRef.sendMessage(Message.raw((String)"2) /hordapve setspawn -> guarda centro de la horda"));
        playerRef.sendMessage(Message.raw((String)"3) Ajusta rondas, tipo, recompensas en la UI y guarda"));
        playerRef.sendMessage(Message.raw((String)"4) /hordapve start -> inicia la horda"));
        playerRef.sendMessage(Message.raw((String)"5) /hordapve stop -> detiene la horda"));
        playerRef.sendMessage(Message.raw((String)"Comandos: /horda help, /hordapve status, /hordapve logs, /hordapve hud, /hordapve enemy <tipo>, /hordapve tipos, /hordapve role <rolNpc|auto>, /hordapve roles, /hordapve reward <rondas>, /hordareload config"));
        playerRef.sendMessage(Message.raw((String)"Alias compatible: /hordepve (mismos subcomandos que /hordapve)."));
        playerRef.sendMessage(Message.raw((String)("Tipos: " + availableTypes)));
    }
}
