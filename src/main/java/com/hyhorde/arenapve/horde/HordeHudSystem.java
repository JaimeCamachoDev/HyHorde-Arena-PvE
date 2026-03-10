package com.hyhorde.arenapve.horde;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HordeHudSystem
extends EntityTickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger(HordeHudSystem.class.getName());
    private static final Query<EntityStore> QUERY = Archetype.of(Player.getComponentType(), PlayerRef.getComponentType());
    private static final int UPDATE_INTERVAL = 8;
    private final HordeService hordeService;
    private final Map<UUID, HordeRuntimeHud> huds;
    private final Map<UUID, Integer> tickCounters;

    public HordeHudSystem(HordeService hordeService) {
        this.hordeService = hordeService;
        this.huds = new ConcurrentHashMap<UUID, HordeRuntimeHud>();
        this.tickCounters = new ConcurrentHashMap<UUID, Integer>();
    }

    public void tick(float deltaTime, int tickCounter, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        EntityStore entityStore = (EntityStore)store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        boolean hordeActiveInWorld = world != null && this.hordeService.isTrackingWorld(world);
        for (int entityIndex = 0; entityIndex < chunk.size(); ++entityIndex) {
            Player player = (Player)chunk.getComponent(entityIndex, Player.getComponentType());
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null || playerRef == null || playerRef.getUuid() == null) {
                continue;
            }
            UUID playerId = playerRef.getUuid();
            boolean shouldShowHud = hordeActiveInWorld && this.hordeService.isArenaPlayer(playerRef);
            if (!shouldShowHud) {
                if (this.huds.containsKey(playerId) || this.tickCounters.containsKey(playerId)) {
                    this.removeHudForPlayer(player, playerRef, playerId);
                } else {
                    this.tickCounters.remove(playerId);
                }
                continue;
            }
            HordeRuntimeHud hud = this.huds.get(playerId);
            if (hud == null) {
                hud = this.createHudForPlayer(player, playerRef, playerId);
                if (hud == null) {
                    continue;
                }
            }
            int localCounter = this.tickCounters.getOrDefault(playerId, 0) + 1;
            if (localCounter < UPDATE_INTERVAL) {
                this.tickCounters.put(playerId, localCounter);
                continue;
            }
            this.tickCounters.put(playerId, 0);
            try {
                hud.setSnapshot(this.hordeService.getStatusSnapshot());
                hud.updateHud(playerRef);
            }
            catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to update Horde HUD for player: " + playerRef.getUsername(), ex);
            }
        }
    }

    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    private HordeRuntimeHud createHudForPlayer(Player player, PlayerRef playerRef, UUID playerId) {
        try {
            HordeRuntimeHud hud = new HordeRuntimeHud(playerRef, this.hordeService);
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)hud);
            hud.show();
            hud.setSnapshot(this.hordeService.getStatusSnapshot());
            hud.updateHud(playerRef);
            this.huds.put(playerId, hud);
            this.tickCounters.put(playerId, 0);
            return hud;
        }
        catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to create Horde HUD for player: " + playerRef.getUsername(), ex);
            return null;
        }
    }

    private void removeHudForPlayer(Player player, PlayerRef playerRef, UUID playerId) {
        HordeRuntimeHud removedHud = this.huds.remove(playerId);
        Integer removedCounter = this.tickCounters.remove(playerId);
        if (removedHud == null && removedCounter == null) {
            return;
        }
        try {
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)null);
        }
        catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to clear Horde HUD binding for player: " + playerRef.getUsername(), ex);
        }
    }

    private static final class HordeRuntimeHud
    extends CustomUIHud {
        private static final String LAYOUT = "Hud/HordeArenaHud.ui";
        private final HordeService hordeService;
        private HordeService.StatusSnapshot snapshot;

        private HordeRuntimeHud(PlayerRef playerRef, HordeService hordeService) {
            super(playerRef);
            this.hordeService = hordeService;
            this.snapshot = hordeService.getStatusSnapshot();
        }

        private void setSnapshot(HordeService.StatusSnapshot snapshot) {
            this.snapshot = snapshot == null ? this.hordeService.getStatusSnapshot() : snapshot;
        }

        public void build(UICommandBuilder commandBuilder) {
            commandBuilder.append(LAYOUT);
            this.updateHudValues(commandBuilder);
        }

        public void updateHud(PlayerRef playerRef) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            try {
                this.updateHudValues(commandBuilder);
                this.update(false, commandBuilder);
            }
            catch (Exception ex) {
                UICommandBuilder rebuildBuilder = new UICommandBuilder();
                try {
                    this.build(rebuildBuilder);
                    this.update(true, rebuildBuilder);
                }
                catch (Exception ignored) {
                    // keep HUD silent if update fails repeatedly
                }
            }
        }

        private void updateHudValues(UICommandBuilder commandBuilder) {
            HordeService.StatusSnapshot status = this.snapshot == null ? this.hordeService.getStatusSnapshot() : this.snapshot;
            boolean english = HordeService.isEnglishLanguage(status.language);
            String worldText = status.worldName == null || status.worldName.isBlank() ? "default" : status.worldName;
            String stateLine = english ? "State: " + (status.active ? "Active" : "Inactive") + " | World: " + worldText : "Estado: " + (status.active ? "Activa" : "Inactiva") + " | Mundo: " + worldText;
            String roundLine = english ? "Round: " + status.currentRound + "/" + status.totalRounds : "Ronda: " + status.currentRound + "/" + status.totalRounds;
            String enemiesLine = english ? "Enemies alive: " + status.aliveEnemies : "Enemigos vivos: " + status.aliveEnemies;
            String killsLine = english ? "Kills: " + status.totalKilled + " | Deaths: " + status.totalDeaths : "Bajas: " + status.totalKilled + " | Muertes: " + status.totalDeaths;
            String nextLine = english ? "Next round: " + (status.nextRoundInSeconds > 0L ? status.nextRoundInSeconds + "s" : "-") : "Siguiente: " + (status.nextRoundInSeconds > 0L ? status.nextRoundInSeconds + "s" : "-");
            String rewardLine = this.buildRewardLine(status, english);
            commandBuilder.set("#TitleLabel.Text", english ? "HORDE PVE" : "HORDA PVE").set("#StateLine.Text", stateLine).set("#RoundLine.Text", roundLine).set("#EnemiesLine.Text", enemiesLine).set("#KillsLine.Text", killsLine).set("#NextLine.Text", nextLine).set("#RewardLine.Text", rewardLine);
        }

        private String buildRewardLine(HordeService.StatusSnapshot status, boolean english) {
            HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
            String itemId = config.rewardItemId == null ? "" : config.rewardItemId.trim();
            int quantity = Math.max(1, config.rewardItemQuantity);
            String mode;
            if (itemId.isBlank()) {
                mode = english ? "none" : "ninguno";
            } else if ("random".equalsIgnoreCase(itemId)) {
                mode = english ? "random" : "aleatorio";
            } else {
                mode = itemId;
            }
            if (english) {
                return "Reward: " + mode + " x" + quantity + " | Every " + config.rewardEveryRounds + " round(s)";
            }
            return "Recompensa: " + mode + " x" + quantity + " | Cada " + config.rewardEveryRounds + " ronda(s)";
        }
    }
}
