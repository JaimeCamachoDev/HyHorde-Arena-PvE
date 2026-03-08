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
        if (commandContext.provided(this.actionArg)) {
            String action = ((String)commandContext.get(this.actionArg)).trim().toLowerCase(Locale.ROOT);
            if ("help".equals(action) || "ayuda".equals(action) || "?".equals(action)) {
                HordeHelpCommand.sendChatHelp(playerRef, this.hordeService);
                return;
            }
        }
        NPCPlugin npcPlugin = NPCPlugin.get();
        List spawnableRoles = npcPlugin.getRoleTemplateNames(true);
        if (spawnableRoles.isEmpty()) {
            playerRef.sendMessage(Message.raw((String)"No hay roles de NPC disponibles para spawnear enemigos."));
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
            playerRef.sendMessage(Message.raw((String)("No se pudo generar la horda (rol usado: " + enemyRole + ").")));
            return;
        }
        playerRef.sendMessage(Message.raw((String)("Horda creada: " + spawned + "/12 enemigos (rol: " + enemyRole + ").")));
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
}


