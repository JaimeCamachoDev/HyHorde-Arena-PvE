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
        String[] actionAndValue = this.resolveActionAndValue(commandContext);
        String action = actionAndValue[0];
        String value = actionAndValue[1];
        switch (action) {
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
            case "logs":
            case "log": {
                playerRef.sendMessage(Message.raw((String)("Ruta de logs: " + this.hordeService.getLogsPathHint())));
                return;
            }
            case "setspawn":
            case "spawn": {
                playerRef.sendMessage(Message.raw((String)this.hordeService.setSpawnFromPlayer(playerRef, world).getMessage()));
                return;
            }
            case "enemy":
            case "enemigo":
            case "tipo":
            case "enemytype": {
                this.handleEnemyType(value, playerRef);
                return;
            }
            case "enemies":
            case "enemigos":
            case "tipos":
            case "enemytypes": {
                this.handleEnemyTypes(playerRef);
                return;
            }
            case "role":
            case "npcrole": {
                this.handleRole(value, playerRef);
                return;
            }
            case "roles":
            case "npcroles": {
                this.handleRoles(playerRef);
                return;
            }
            case "reward": {
                this.handleReward(value, playerRef);
                return;
            }
            default: {
                playerRef.sendMessage(Message.raw((String)("Subcomando no valido: " + action + ". Usa /hordahelp.")));
            }
        }
    }

    private void openUi(Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            playerRef.sendMessage(Message.raw((String)"No se pudo abrir la interfaz ahora mismo. Usa /hordahelp."));
            return;
        }
        try {
            HordeConfigPage.open(playerEntityRef, store, player, playerRef, this.hordeService);
        }
        catch (Exception ex) {
            playerRef.sendMessage(Message.raw((String)"La interfaz fallo al abrirse. Revisa logs del servidor."));
        }
    }

    private void handleEnemyType(String value, PlayerRef playerRef) {
        String enemyType = value == null ? "" : value.trim();
        if (enemyType.isBlank()) {
            List<String> options = this.hordeService.getEnemyTypeOptionsForCurrentRoles();
            String usage = options.isEmpty() ? "undead|goblins|scarak|void|wild|elementals" : String.join("|", options);
            playerRef.sendMessage(Message.raw((String)("Uso: /hordapve enemy <" + usage + ">")));
            return;
        }
        playerRef.sendMessage(Message.raw((String)this.hordeService.setEnemyType(enemyType).getMessage()));
    }

    private void handleEnemyTypes(PlayerRef playerRef) {
        List<String> diagnostics = this.hordeService.getEnemyTypeDiagnostics();
        playerRef.sendMessage(Message.raw((String)"Categorias de horda y roles detectados:"));
        for (String entry : diagnostics) {
            playerRef.sendMessage(Message.raw((String)(" - " + entry)));
        }
    }

    private void handleRole(String value, PlayerRef playerRef) {
        String requestedRole = value == null ? "" : value.trim();
        if (requestedRole.isBlank()) {
            String currentRole = this.hordeService.getConfiguredNpcRole();
            String roleState = currentRole == null || currentRole.isBlank() ? "sin override (por categoria enemyType)" : currentRole;
            playerRef.sendMessage(Message.raw((String)("Rol NPC actual: " + roleState)));
            playerRef.sendMessage(Message.raw((String)"Uso: /hordapve role <rolNpc|auto>"));
            return;
        }
        playerRef.sendMessage(Message.raw((String)this.hordeService.setNpcRole(requestedRole).getMessage()));
    }

    private void handleRoles(PlayerRef playerRef) {
        List<String> roles = this.hordeService.getAvailableRoles();
        if (roles.isEmpty()) {
            playerRef.sendMessage(Message.raw((String)"No hay roles NPC disponibles."));
            return;
        }
        playerRef.sendMessage(Message.raw((String)("Roles NPC disponibles (" + roles.size() + "):")));
        playerRef.sendMessage(Message.raw((String)String.join(", ", roles)));
    }

    private void handleReward(String value, PlayerRef playerRef) {
        int everyRounds;
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            playerRef.sendMessage(Message.raw((String)"Uso: /hordapve reward <rondas>"));
            return;
        }
        try {
            everyRounds = Integer.parseInt(raw);
        }
        catch (Exception ex) {
            playerRef.sendMessage(Message.raw((String)"El valor de reward debe ser un numero entero positivo."));
            return;
        }
        playerRef.sendMessage(Message.raw((String)this.hordeService.setRewardEveryRounds(everyRounds).getMessage()));
    }

    private String[] resolveActionAndValue(CommandContext commandContext) {
        String actionRaw = HordePveCommand.readOptionalArgText(commandContext, this.actionArg);
        String valueRaw = HordePveCommand.readOptionalArgText(commandContext, this.valueArg);
        if (actionRaw.isBlank()) {
            return new String[]{"menu", ""};
        }
        String action = actionRaw.trim();
        int separator = action.indexOf(32);
        if (separator >= 0) {
            String actionToken = action.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String extraValue = action.substring(separator + 1).trim();
            if (valueRaw.isBlank()) {
                valueRaw = extraValue;
            }
            return new String[]{actionToken, valueRaw.trim()};
        }
        return new String[]{action.toLowerCase(Locale.ROOT), valueRaw.trim()};
    }

    private static String readOptionalArgText(CommandContext commandContext, OptionalArg<String> optionalArg) {
        try {
            CharSequence[] input = commandContext.getInput(optionalArg);
            if (input != null && input.length > 0) {
                return String.join((CharSequence)" ", input).trim();
            }
        }
        catch (Exception exception) {
            // fallback below
        }
        try {
            if (commandContext.provided(optionalArg)) {
                Object raw = commandContext.get(optionalArg);
                return raw == null ? "" : raw.toString().trim();
            }
        }
        catch (Exception exception) {
            // no-op
        }
        return "";
    }
}
