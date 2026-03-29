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
        if (this.hordeService.isPluginReloadInProgress() && !"status".equals(action) && !"logs".equals(action) && !"log".equals(action)) {
            this.sendLocalized(playerRef, "Plugin reload in progress. Try again in a few seconds.", "Recarga del plugin en progreso. Prueba de nuevo en unos segundos.");
            return;
        }
        switch (action) {
            case "menu":
            case "ui": {
                this.openUi(store, ref, playerRef, world);
                return;
            }
            case "start": {
                this.sendLocalized(playerRef, this.hordeService.start(store, playerRef, world).getMessage());
                return;
            }
            case "stop": {
                this.sendLocalized(playerRef, this.hordeService.stop(true).getMessage());
                return;
            }
            case "status": {
                this.sendLocalized(playerRef, this.hordeService.getStatusLine());
                return;
            }
            case "logs":
            case "log": {
                this.sendLocalized(playerRef, "Logs path: " + this.hordeService.getLogsPathHint(), "Ruta de logs: " + this.hordeService.getLogsPathHint());
                return;
            }
            case "setspawn":
            case "spawn": {
                this.sendLocalized(playerRef, this.hordeService.setSpawnFromPlayer(playerRef, world).getMessage());
                return;
            }
            case "reload":
            case "reloadconfig": {
                this.sendLocalized(playerRef, this.hordeService.reloadConfigFromDisk().getMessage());
                return;
            }
            case "reloadmod":
            case "reloadplugin": {
                this.sendLocalized(playerRef, "Hot-reload of .jar mods is not supported. Replace the file and restart the server.", "No se soporta recarga en caliente de mods .jar. Reemplaza el archivo y reinicia el servidor.");
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
            case "spectator":
            case "spectate":
            case "espectador": {
                this.handleSpectatorPreference(value, playerRef);
                return;
            }
            case "player":
            case "jugador": {
                this.sendLocalized(playerRef, this.hordeService.setSpectatorPreference(playerRef, false).getMessage());
                return;
            }
            case "arearadius":
            case "radarena":
            case "radioarena": {
                this.handleArenaRadius(value, playerRef);
                return;
            }
            default: {
                this.sendLocalized(playerRef, "Invalid subcommand: " + action + ". Use /hordahelp.", "Subcomando no valido: " + action + ". Usa /hordahelp.");
            }
        }
    }

    private void openUi(Store<EntityStore> store, Ref<EntityStore> playerEntityRef, PlayerRef playerRef, World world) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            this.sendLocalized(playerRef, "Could not open the interface right now. Use /hordahelp.", "No se pudo abrir la interfaz ahora mismo. Usa /hordahelp.");
            return;
        }
        HordeService.OperationResult bootstrapResult = this.hordeService.ensureUiBootstrapPresets(playerRef, world);
        if (bootstrapResult != null && !bootstrapResult.isSuccess()) {
            this.sendLocalized(playerRef, bootstrapResult.getMessage());
        }
        try {
            HordeConfigPage.open(playerEntityRef, store, player, playerRef, this.hordeService);
        }
        catch (Exception ex) {
            this.sendLocalized(playerRef, "The interface failed to open. Check server logs.", "La interfaz fallo al abrirse. Revisa logs del servidor.");
        }
    }

    private void handleEnemyType(String value, PlayerRef playerRef) {
        String enemyType = value == null ? "" : value.trim();
        if (enemyType.isBlank()) {
            List<String> options = this.hordeService.getEnemyTypeOptionsForCurrentRoles();
            String usage = options.isEmpty() ? "undead|goblins|scarak|void|wild|elementals" : String.join("|", options);
            this.sendLocalized(playerRef, "Usage: /hordeconfig enemy <" + usage + ">", "Uso: /hordeconfig enemy <" + usage + ">");
            return;
        }
        this.sendLocalized(playerRef, this.hordeService.setEnemyType(enemyType).getMessage());
    }

    private void handleEnemyTypes(PlayerRef playerRef) {
        List<String> diagnostics = this.hordeService.getEnemyTypeDiagnostics();
        this.sendLocalized(playerRef, "Detected horde categories and roles:", "Categorias de horda y roles detectados:");
        for (String entry : diagnostics) {
            this.sendLocalized(playerRef, " - " + entry);
        }
    }

    private void handleRole(String value, PlayerRef playerRef) {
        String requestedRole = value == null ? "" : value.trim();
        if (requestedRole.isBlank()) {
            String currentRole = this.hordeService.getConfiguredNpcRole();
            String roleStateEnglish = currentRole == null || currentRole.isBlank() ? "no override (using enemyType category)" : currentRole;
            String roleStateSpanish = currentRole == null || currentRole.isBlank() ? "sin override (por categoria enemyType)" : currentRole;
            this.sendLocalized(playerRef, "Current NPC role: " + roleStateEnglish, "Rol NPC actual: " + roleStateSpanish);
            this.sendLocalized(playerRef, "Usage: /hordeconfig role <npcRole|auto>", "Uso: /hordeconfig role <rolNpc|auto>");
            return;
        }
        this.sendLocalized(playerRef, this.hordeService.setNpcRole(requestedRole).getMessage());
    }

    private void handleRoles(PlayerRef playerRef) {
        List<String> roles = this.hordeService.getAvailableRoles();
        if (roles.isEmpty()) {
            this.sendLocalized(playerRef, "No NPC roles available.", "No hay roles NPC disponibles.");
            return;
        }
        this.sendLocalized(playerRef, "Available NPC roles (" + roles.size() + "):", "Roles NPC disponibles (" + roles.size() + "):");
        this.sendLocalized(playerRef, String.join(", ", roles));
    }

    private void handleReward(String value, PlayerRef playerRef) {
        int everyRounds;
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            this.sendLocalized(playerRef, "Usage: /hordeconfig reward <rounds>", "Uso: /hordeconfig reward <rondas>");
            return;
        }
        try {
            everyRounds = Integer.parseInt(raw);
        }
        catch (Exception ex) {
            this.sendLocalized(playerRef, "Reward value must be a positive integer.", "El valor de reward debe ser un numero entero positivo.");
            return;
        }
        this.sendLocalized(playerRef, this.hordeService.setRewardEveryRounds(everyRounds).getMessage());
    }

    private void handleSpectatorPreference(String value, PlayerRef playerRef) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank()) {
            boolean spectator = this.hordeService.isSpectatorPreferenceEnabled(playerRef);
            String currentEnglish = spectator ? "SPECTATOR" : "PLAYER";
            String currentSpanish = spectator ? "ESPECTADOR" : "JUGADOR";
            this.sendLocalized(playerRef, "Current pre-start role: " + currentEnglish, "Rol previo al inicio: " + currentSpanish);
            this.sendLocalized(playerRef, "Usage: /hordeconfig spectator <on|off>", "Uso: /hordeconfig spectator <on|off>");
            return;
        }
        if ("on".equals(raw) || "true".equals(raw) || "1".equals(raw) || "yes".equals(raw) || "si".equals(raw) || "espectador".equals(raw) || "spectator".equals(raw)) {
            this.sendLocalized(playerRef, this.hordeService.setSpectatorPreference(playerRef, true).getMessage());
            return;
        }
        if ("off".equals(raw) || "false".equals(raw) || "0".equals(raw) || "no".equals(raw) || "jugador".equals(raw) || "player".equals(raw)) {
            this.sendLocalized(playerRef, this.hordeService.setSpectatorPreference(playerRef, false).getMessage());
            return;
        }
        this.sendLocalized(playerRef, "Usage: /hordeconfig spectator <on|off>", "Uso: /hordeconfig spectator <on|off>");
    }

    private void handleArenaRadius(String value, PlayerRef playerRef) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            this.sendLocalized(playerRef, String.format(Locale.ROOT, "Current arena radius: %.2f blocks. Usage: /hordeconfig arearadius <value>", this.hordeService.getArenaJoinRadius()), String.format(Locale.ROOT, "Radio de arena actual: %.2f bloques. Uso: /hordeconfig arearadius <valor>", this.hordeService.getArenaJoinRadius()));
            return;
        }
        double radius;
        try {
            radius = Double.parseDouble(raw);
        }
        catch (Exception ex) {
            this.sendLocalized(playerRef, "Arena radius must be a valid number.", "El radio de arena debe ser un numero valido.");
            return;
        }
        this.sendLocalized(playerRef, this.hordeService.setArenaJoinRadius(radius).getMessage());
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
