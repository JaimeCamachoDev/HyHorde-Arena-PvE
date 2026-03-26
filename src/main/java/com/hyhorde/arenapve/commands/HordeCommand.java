package com.hyhorde.arenapve.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

public class HordeCommand
extends AbstractPlayerCommand {
    private static final int HORDE_SIZE = 12;
    private static final double MIN_RADIUS = 3.0;
    private static final double MAX_RADIUS = 8.0;
    private static final String[] ENEMY_ROLE_HINTS = new String[]{"enemy", "hostile", "bandit", "goblin", "skeleton", "zombie", "spider", "wolf", "slime", "beetle", "crawler"};
    private final HordeService hordeService;
    private final OptionalArg<String> actionArg;

    public HordeCommand(@Nonnull String name, @Nonnull String description, HordeService hordeService) {
        super(name, description);
        this.hordeService = hordeService;
        this.actionArg = this.withOptionalArg("accion", "accion", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String action = HordeCommand.readAction(commandContext, this.actionArg);
        if (!action.isBlank()) {
            this.sendLocalized(playerRef, "The /horda command does not use subcommands. Use /hordahelp.", "El comando /horda no usa subcomandos. Usa /hordahelp.");
            return;
        }
        NPCPlugin npcPlugin = NPCPlugin.get();
        List spawnableRoles = npcPlugin.getRoleTemplateNames(true);
        if (spawnableRoles.isEmpty()) {
            this.sendLocalized(playerRef, "No NPC roles are available to spawn enemies.", "No hay roles de NPC disponibles para spawnear enemigos.");
            return;
        }
        String enemyRole = HordeCommand.chooseEnemyRole(spawnableRoles);
        Transform transform = playerRef.getTransform();
        Vector3d playerPosition = transform.getPosition();
        Vector3f playerRotation = transform.getRotation();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        for (int i = 0; i < 12; ++i) {
            double angle = random.nextDouble(0.0, Math.PI * 2);
            double distance = random.nextDouble(3.0, 8.0);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            Vector3d spawnPosition = new Vector3d(playerPosition).add(offsetX, 0.0, offsetZ);
            try {
                if (npcPlugin.spawnNPC(store, enemyRole, null, spawnPosition, new Vector3f(playerRotation)) == null) continue;
                ++spawned;
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (spawned == 0) {
            this.sendLocalized(playerRef, "Could not create horde (role used: " + enemyRole + ").", "No se pudo generar la horda (rol usado: " + enemyRole + ").");
            return;
        }
        this.sendLocalized(playerRef, "Horde created: " + spawned + "/12 enemies (role: " + enemyRole + ").", "Horda creada: " + spawned + "/12 enemigos (rol: " + enemyRole + ").");
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

    private static String chooseEnemyRole(List<String> roles) {
        for (String hint : ENEMY_ROLE_HINTS) {
            for (String role : roles) {
                if (!role.toLowerCase(Locale.ROOT).equals(hint)) continue;
                return role;
            }
        }
        for (String hint : ENEMY_ROLE_HINTS) {
            for (String role : roles) {
                if (!role.toLowerCase(Locale.ROOT).contains(hint)) continue;
                return role;
            }
        }
        return roles.get(0);
    }

    private static String readAction(CommandContext commandContext, OptionalArg<String> actionArg) {
        try {
            CharSequence[] input = commandContext.getInput(actionArg);
            if (input != null && input.length > 0) {
                for (CharSequence part : input) {
                    if (part == null) {
                        continue;
                    }
                    String trimmedPart = part.toString().trim();
                    if (trimmedPart.isBlank()) {
                        continue;
                    }
                    int separator = trimmedPart.indexOf(32);
                    String token = separator >= 0 ? trimmedPart.substring(0, separator) : trimmedPart;
                    return token.toLowerCase(Locale.ROOT);
                }
            }
        }
        catch (Exception exception) {
            // fallback below
        }
        try {
            if (commandContext.provided(actionArg)) {
                Object raw = commandContext.get(actionArg);
                if (raw != null) {
                    return raw.toString().trim().toLowerCase(Locale.ROOT);
                }
            }
        }
        catch (Exception exception) {
            // ignore
        }
        return "";
    }

}


