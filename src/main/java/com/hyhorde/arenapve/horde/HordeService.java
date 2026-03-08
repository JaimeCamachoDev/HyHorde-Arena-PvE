package com.hyhorde.arenapve.horde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hyhorde.arenapve.horde.HordeStatusPage;
import it.unimi.dsi.fastutil.Pair;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HordeService {
    private static final Map<String, String[]> ENEMY_TYPE_HINTS = HordeService.buildEnemyTypeHints();
    private static final List<String> ENEMY_TYPE_OPTIONS = new ArrayList<String>(ENEMY_TYPE_HINTS.keySet());
    private static final List<String> RANDOM_ENEMY_TYPE_OPTIONS = HordeService.buildRandomEnemyTypePool();
    private static final List<String> REWARD_ITEM_SUGGESTIONS = HordeService.buildRewardItemSuggestions();
    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 200;
    private static final int MIN_ENEMIES_PER_ROUND = 1;
    private static final int MAX_ENEMIES_PER_ROUND = 250;
    private static final int MIN_ENEMY_INCREMENT = 0;
    private static final int MAX_ENEMY_INCREMENT = 250;
    private static final int MIN_PLAYER_MULTIPLIER = 1;
    private static final int MAX_PLAYER_MULTIPLIER = 20;
    private static final int MIN_WAVE_DELAY_SECONDS = 0;
    private static final int MAX_WAVE_DELAY_SECONDS = 300;
    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 128.0;
    private final PluginBase plugin;
    private final Gson gson;
    private final Path configPath;
    private final Map<UUID, HordeStatusPage> statusPages;
    private HordeConfig config;
    private HordeSession session;

    public HordeService(PluginBase plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configPath = plugin.getDataDirectory().resolve("horde-config.json");
        this.statusPages = new HashMap<UUID, HordeStatusPage>();
        this.config = HordeConfig.defaults();
        this.loadConfig();
    }

    public synchronized HordeConfig getConfigSnapshot() {
        return this.config.copy();
    }

    public synchronized boolean isActive() {
        return this.session != null;
    }

    public synchronized String getStatusLine() {
        if (this.session == null) {
            return "Sin horda activa. Usa /hordapve para abrir la interfaz.";
        }
        int alive = HordeService.countAlive(this.session.activeEnemies);
        String rewardInfo = this.config.rewardItemId == null || this.config.rewardItemId.isBlank() ? "sin item" : this.config.rewardItemId + " x" + this.config.rewardItemQuantity;
        return "Horda activa | Ronda " + this.session.currentRound + "/" + this.config.rounds + " | Enemigos vivos: " + alive + " | Spawn total: " + this.session.totalSpawned + " | Kills detectadas: " + this.session.totalKilled + " | Tipo: " + this.session.enemyType + " | Rol real: " + this.session.role + " | Jugadores x" + this.session.playerMultiplier + " | Recompensa cada: " + this.config.rewardEveryRounds + " ronda(s) | Item: " + rewardInfo;
    }

    public synchronized List<String> getAvailableRoles() {
        return new ArrayList<String>(NPCPlugin.get().getRoleTemplateNames(true));
    }

    public synchronized List<String> getEnemyTypeOptions() {
        return new ArrayList<String>(ENEMY_TYPE_OPTIONS);
    }

    public synchronized List<String> getRewardItemSuggestions() {
        return new ArrayList<String>(REWARD_ITEM_SUGGESTIONS);
    }

    public synchronized String getLogsPathHint() {
        return HordeService.resolveLogsPath(this.plugin.getDataDirectory());
    }

    public synchronized StatusSnapshot getStatusSnapshot() {
        return this.createStatusSnapshot(this.session == null ? null : this.session.world);
    }

    public synchronized OperationResult reloadConfigFromDisk() {
        this.loadConfig();
        this.refreshStatusHud(this.session);
        return OperationResult.ok("Configuracion de hordas recargada desde disco.");
    }

    public synchronized OperationResult openStatusHud(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, PlayerRef playerRef, World world) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return OperationResult.fail("No se pudo abrir el panel de estado ahora mismo.");
        }
        HordeStatusPage previous = this.statusPages.remove(playerRef.getUuid());
        if (previous != null) {
            previous.closeFromService();
        }
        StatusSnapshot snapshot = this.createStatusSnapshot(world);
        HordeStatusPage page = HordeStatusPage.open(playerEntityRef, store, player, playerRef, snapshot, this::unregisterStatusPage);
        this.statusPages.put(playerRef.getUuid(), page);
        return OperationResult.ok("Panel de estado de horda abierto.");
    }

    public synchronized OperationResult setEnemyType(String enemyTypeInput) {
        String enemyType = HordeService.normalizeEnemyType(enemyTypeInput);
        if (!ENEMY_TYPE_HINTS.containsKey(enemyType)) {
            return OperationResult.fail("Tipo invalido. Usa uno de: " + String.join((CharSequence)", ", ENEMY_TYPE_OPTIONS));
        }
        this.config.enemyType = enemyType;
        this.saveConfig();
        if ("auto".equals(enemyType)) {
            return OperationResult.ok("Tipo de enemigo en modo automatico.");
        }
        if ("random".equals(enemyType)) {
            return OperationResult.ok("Tipo de enemigo en modo aleatorio por spawn.");
        }
        return OperationResult.ok("Tipo de enemigo configurado en: " + enemyType);
    }

    public synchronized OperationResult setRewardEveryRounds(int everyRounds) {
        if (everyRounds <= 0 || everyRounds > 200) {
            return OperationResult.fail("reward debe estar entre 1 y 200.");
        }
        this.config.rewardEveryRounds = everyRounds;
        this.saveConfig();
        return OperationResult.ok("Recompensas configuradas cada " + everyRounds + " ronda(s).");
    }

    public synchronized OperationResult setSpawnFromPlayer(PlayerRef playerRef, World world) {
        Transform transform = playerRef.getTransform();
        Vector3d position = transform.getPosition();
        this.config.spawnConfigured = true;
        this.config.worldName = world.getName();
        this.config.spawnX = position.x;
        this.config.spawnY = position.y;
        this.config.spawnZ = position.z;
        this.saveConfig();
        return OperationResult.ok(String.format(Locale.ROOT, "Centro de horda guardado en %.2f %.2f %.2f (mundo: %s).", this.config.spawnX, this.config.spawnY, this.config.spawnZ, this.config.worldName));
    }

    public synchronized OperationResult applyUiConfig(Map<String, String> values, World world) {
        HordeConfig updated = this.config.copy();
        try {
            updated.spawnX = HordeService.parseDouble(values.get("spawnX"), updated.spawnX, "spawnX");
            updated.spawnY = HordeService.parseDouble(values.get("spawnY"), updated.spawnY, "spawnY");
            updated.spawnZ = HordeService.parseDouble(values.get("spawnZ"), updated.spawnZ, "spawnZ");
            updated.minSpawnRadius = HordeService.parseDouble(values.get("minRadius"), updated.minSpawnRadius, "minRadius");
            updated.maxSpawnRadius = HordeService.parseDouble(values.get("maxRadius"), updated.maxSpawnRadius, "maxRadius");
            updated.rounds = HordeService.parseInt(values.get("rounds"), updated.rounds, "rounds");
            updated.baseEnemiesPerRound = HordeService.parseInt(values.get("baseEnemies"), updated.baseEnemiesPerRound, "baseEnemies");
            updated.enemiesPerRoundIncrement = HordeService.parseInt(values.get("enemiesPerRound"), updated.enemiesPerRoundIncrement, "enemiesPerRound");
            updated.waveDelaySeconds = HordeService.parseInt(values.get("waveDelay"), updated.waveDelaySeconds, "waveDelay");
            updated.playerMultiplier = HordeService.parseInt(values.get("playerMultiplier"), updated.playerMultiplier, "playerMultiplier");
            updated.rewardEveryRounds = HordeService.parseInt(values.get("rewardEveryRounds"), updated.rewardEveryRounds, "rewardEveryRounds");
        }
        catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }
        String enemyTypeValue = values.get("enemyType");
        if (enemyTypeValue != null) {
            updated.enemyType = HordeService.normalizeEnemyType(enemyTypeValue);
        }
        if (!ENEMY_TYPE_HINTS.containsKey(updated.enemyType)) {
            return OperationResult.fail("enemyType debe ser uno de: " + String.join((CharSequence)", ", ENEMY_TYPE_OPTIONS));
        }
        if (updated.rewardEveryRounds <= 0 || updated.rewardEveryRounds > 200) {
            return OperationResult.fail("rewardEveryRounds debe estar entre 1 y 200.");
        }
        String rewardItemIdRaw = values.get("rewardItemId");
        if (rewardItemIdRaw != null) {
            updated.rewardItemId = rewardItemIdRaw.trim();
        }
        try {
            updated.rewardItemQuantity = HordeService.parseInt(values.get("rewardItemQuantity"), updated.rewardItemQuantity, "rewardItemQuantity");
        }
        catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }
        if (updated.rewardItemQuantity <= 0 || updated.rewardItemQuantity > 9999) {
            return OperationResult.fail("rewardItemQuantity debe estar entre 1 y 9999.");
        }
        if (updated.rewardItemId != null && !updated.rewardItemId.isBlank()) {
            ItemStack rewardStack = new ItemStack(updated.rewardItemId, updated.rewardItemQuantity);
            if (!rewardStack.isValid() || rewardStack.isEmpty()) {
                return OperationResult.fail("rewardItemId no es valido: " + updated.rewardItemId);
            }
        }
        if (updated.minSpawnRadius < 1.0 || updated.minSpawnRadius > 128.0) {
            return OperationResult.fail("minRadius debe estar entre 1.0 y 128.0.");
        }
        if (updated.maxSpawnRadius < 1.0 || updated.maxSpawnRadius > 128.0) {
            return OperationResult.fail("maxRadius debe estar entre 1.0 y 128.0.");
        }
        if (updated.maxSpawnRadius < updated.minSpawnRadius) {
            return OperationResult.fail("maxRadius debe ser mayor o igual a minRadius.");
        }
        if (updated.rounds < 1 || updated.rounds > 200) {
            return OperationResult.fail("rounds debe estar entre 1 y 200.");
        }
        if (updated.baseEnemiesPerRound < 1 || updated.baseEnemiesPerRound > 250) {
            return OperationResult.fail("baseEnemies debe estar entre 1 y 250.");
        }
        if (updated.enemiesPerRoundIncrement < 0 || updated.enemiesPerRoundIncrement > 250) {
            return OperationResult.fail("enemiesPerRound debe estar entre 0 y 250.");
        }
        if (updated.playerMultiplier < MIN_PLAYER_MULTIPLIER || updated.playerMultiplier > MAX_PLAYER_MULTIPLIER) {
            return OperationResult.fail("playerMultiplier debe estar entre 1 y 20.");
        }
        if (updated.waveDelaySeconds < 0 || updated.waveDelaySeconds > 300) {
            return OperationResult.fail("waveDelay debe estar entre 0 y 300 segundos.");
        }
        updated.spawnConfigured = true;
        updated.worldName = world.getName();
        this.config = updated;
        this.saveConfig();
        return OperationResult.ok("Configuracion de hordas guardada.");
    }

    public synchronized OperationResult start(Store<EntityStore> store, PlayerRef startedBy, World world) {
        HordeSession newSession;
        if (this.session != null) {
            return OperationResult.fail("Ya hay una horda activa.");
        }
        List<String> roles = NPCPlugin.get().getRoleTemplateNames(true);
        if (roles.isEmpty()) {
            return OperationResult.fail("No hay roles de NPC disponibles.");
        }
        if (!this.config.spawnConfigured || !Objects.equals(this.config.worldName, world.getName())) {
            Transform transform = startedBy.getTransform();
            this.config.spawnConfigured = true;
            this.config.worldName = world.getName();
            this.config.spawnX = transform.getPosition().x;
            this.config.spawnY = transform.getPosition().y;
            this.config.spawnZ = transform.getPosition().z;
            this.saveConfig();
        }
        String selectedEnemyType = HordeService.normalizeEnemyType(this.config.enemyType);
        String roleTypeForStart = HordeService.isRandomEnemyType(selectedEnemyType) ? "auto" : selectedEnemyType;
        String selectedRole = HordeService.chooseRole(roles, roleTypeForStart, this.config.npcRole);
        this.session = newSession = new HordeSession(world, store, selectedRole, selectedEnemyType, new ArrayList<String>(roles), this.config.playerMultiplier);
        newSession.ticker = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> this.tickSession(newSession), 1000L, 1000L, TimeUnit.MILLISECONDS);
        world.sendMessage(Message.raw((String)String.format(Locale.ROOT, "Horda PVE iniciada en %.2f %.2f %.2f | %d rondas | tipo: %s | rol: %s | jugadores x%d", this.config.spawnX, this.config.spawnY, this.config.spawnZ, this.config.rounds, selectedEnemyType, selectedRole, this.config.playerMultiplier)));
        this.broadcastHordeStartAnnouncement(selectedEnemyType, selectedRole, this.config.playerMultiplier);
        this.spawnNextRound(newSession, new Vector3f(startedBy.getTransform().getRotation()));
        return OperationResult.ok("Horda iniciada.");
    }

    public synchronized OperationResult stop(boolean cleanupAliveEnemies) {
        if (this.session == null) {
            return OperationResult.fail("No hay horda activa.");
        }
        this.endSession(this.session, "Horda detenida manualmente.", cleanupAliveEnemies);
        return OperationResult.ok("Horda detenida.");
    }

    public synchronized void shutdown() {
        if (this.session == null) {
            this.closeAllStatusPages();
            return;
        }
        this.endSession(this.session, "Horda finalizada por apagado del plugin.", false);
        this.closeAllStatusPages();
    }

    private synchronized void tickSession(HordeSession trackedSession) {
        if (this.session != trackedSession) {
            return;
        }
        World world = trackedSession.world;
        if (!world.isAlive()) {
            this.endSession(trackedSession, "Horda finalizada: el mundo dejo de estar activo.", false);
            return;
        }
        world.execute(() -> {
            HordeService hordeService = this;
            synchronized (hordeService) {
                if (this.session != trackedSession) {
                    return;
                }
                int removed = HordeService.removeInvalidRefs(trackedSession.activeEnemies);
                if (removed > 0) {
                    trackedSession.totalKilled += removed;
                }
                if (trackedSession.roundActive && trackedSession.activeEnemies.isEmpty()) {
                    trackedSession.roundActive = false;
                    this.grantRoundRewards(trackedSession, trackedSession.currentRound);
                    if (trackedSession.currentRound >= this.config.rounds) {
                        this.endSession(trackedSession, "Horda completada. Todas las rondas terminadas.", false);
                        return;
                    }
                    trackedSession.nextRoundAtMillis = System.currentTimeMillis() + (long)this.config.waveDelaySeconds * 1000L;
                    world.sendMessage(Message.raw((String)("Ronda " + trackedSession.currentRound + " completada. La siguiente ronda empieza en " + this.config.waveDelaySeconds + "s.")));
                }
                if (!trackedSession.roundActive && trackedSession.currentRound < this.config.rounds && System.currentTimeMillis() >= trackedSession.nextRoundAtMillis) {
                    this.spawnNextRound(trackedSession, Vector3f.ZERO);
                }
                this.refreshStatusHud(trackedSession);
            }
        });
    }

    private void spawnNextRound(HordeSession sessionToAdvance, Vector3f baseRotation) {
        int nextRound = sessionToAdvance.currentRound + 1;
        int playerMultiplier = Math.max(MIN_PLAYER_MULTIPLIER, sessionToAdvance.playerMultiplier);
        int baseCount = this.config.baseEnemiesPerRound + (nextRound - 1) * this.config.enemiesPerRoundIncrement;
        int targetCount = Math.max(1, baseCount * playerMultiplier);
        Vector3d center = new Vector3d(this.config.spawnX, this.config.spawnY, this.config.spawnZ);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        for (int i = 0; i < targetCount; ++i) {
            double angle = random.nextDouble(0.0, Math.PI * 2);
            double distance = random.nextDouble(this.config.minSpawnRadius, this.config.maxSpawnRadius);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            Vector3d spawnPosition = new Vector3d(center).add(offsetX, 0.0, offsetZ);
            try {
                String roleForSpawn = sessionToAdvance.role;
                if (HordeService.isRandomEnemyType(sessionToAdvance.enemyType)) {
                    String randomEnemyType = HordeService.pickRandomEnemyType();
                    roleForSpawn = HordeService.chooseRole(sessionToAdvance.availableRoles, randomEnemyType, this.config.npcRole);
                }
                Pair created = NPCPlugin.get().spawnNPC(sessionToAdvance.store, roleForSpawn, null, spawnPosition, new Vector3f(baseRotation));
                if (created == null || created.left() == null) continue;
                sessionToAdvance.activeEnemies.add((Ref<EntityStore>)((Ref)created.left()));
                ++sessionToAdvance.totalSpawned;
                ++spawned;
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (spawned == 0) {
            this.endSession(sessionToAdvance, "La horda se cancelo: no se pudo spawnear ningun NPC con el rol " + sessionToAdvance.role + ".", false);
            return;
        }
        sessionToAdvance.currentRound = nextRound;
        sessionToAdvance.roundActive = true;
        sessionToAdvance.nextRoundAtMillis = 0L;
        sessionToAdvance.world.sendMessage(Message.raw((String)("Ronda " + nextRound + "/" + this.config.rounds + " iniciada: " + spawned + " enemigos (x" + playerMultiplier + " jugadores).")));
    }

    private void grantRoundRewards(HordeSession trackedSession, int completedRound) {
        if (completedRound <= 0 || this.config.rewardEveryRounds <= 0) {
            return;
        }
        if (completedRound % this.config.rewardEveryRounds != 0) {
            return;
        }
        trackedSession.world.sendMessage(Message.raw((String)("Recompensa desbloqueada por completar la ronda " + completedRound + ".")));
        if (this.config.rewardItemId == null || this.config.rewardItemId.isBlank()) {
            return;
        }
        ItemStack rewardStack = new ItemStack(this.config.rewardItemId, this.config.rewardItemQuantity);
        if (!rewardStack.isValid() || rewardStack.isEmpty()) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo dropear recompensa: item invalido '%s'.", (Object)this.config.rewardItemId);
            return;
        }
        Vector3d dropPosition = new Vector3d(this.config.spawnX, this.config.spawnY + 1.0, this.config.spawnZ);
        Holder itemEntityHolder = ItemComponent.generateItemDrop(trackedSession.store, rewardStack, dropPosition, Vector3f.ZERO, 0.0f, 0.35f, 0.0f);
        if (itemEntityHolder == null) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo generar la entidad de recompensa para '%s'.", (Object)this.config.rewardItemId);
            return;
        }
        ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
        if (itemComponent != null) {
            itemComponent.setPickupDelay(0.6f);
        }
        trackedSession.store.addEntity(itemEntityHolder, AddReason.SPAWN);
        trackedSession.world.sendMessage(Message.raw((String)("Item recompensa dropeado en el centro: " + this.config.rewardItemId + " x" + this.config.rewardItemQuantity + ".")));
    }

    private void broadcastHordeStartAnnouncement(String enemyType, String role, int playerMultiplier) {
        try {
            String titleText = "HORDA PVE INICIADA";
            String subtitleText = String.format(Locale.ROOT, "Tipo: %s | Rol: %s | Rondas: %d | Jugadores x%d", enemyType, role, this.config.rounds, playerMultiplier);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 4.0f, 1.0f, 1.0f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar anuncio global de inicio de horda: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastHordeEndAnnouncement(String reason, int aliveAtEnd) {
        try {
            String normalizedReason = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
            String titleText = normalizedReason.contains("completada") ? "HORDA PVE COMPLETADA" : "HORDA PVE FINALIZADA";
            String subtitleBase = normalizedReason.contains("completada") ? "Todas las rondas terminadas" : HordeService.compactText(reason, 64);
            String subtitleText = subtitleBase + " | Vivos restantes: " + aliveAtEnd;
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 4.0f, 1.0f, 1.0f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar anuncio global de fin de horda: %s", (Object)ex.getMessage());
        }
    }

    private void endSession(HordeSession trackedSession, String reason, boolean cleanupAliveEnemies) {
        if (this.session != trackedSession) {
            return;
        }
        if (trackedSession.ticker != null) {
            trackedSession.ticker.cancel(false);
        }
        if (cleanupAliveEnemies) {
            for (Ref<EntityStore> enemyRef : trackedSession.activeEnemies) {
                if (enemyRef == null || !enemyRef.isValid()) continue;
                try {
                    trackedSession.store.removeEntity(enemyRef, RemoveReason.REMOVE);
                }
                catch (Exception exception) {}
            }
        }
        int aliveAtEnd = HordeService.countAlive(trackedSession.activeEnemies);
        trackedSession.activeEnemies.clear();
        this.session = null;
        trackedSession.world.sendMessage(Message.raw((String)(reason + " (vivos restantes: " + aliveAtEnd + ").")));
        this.broadcastHordeEndAnnouncement(reason, aliveAtEnd);
        this.refreshStatusHud(null);
    }

    private StatusSnapshot createStatusSnapshot(World requestedWorld) {
        if (this.session == null) {
            String worldName = requestedWorld != null ? requestedWorld.getName() : this.config.worldName;
            return StatusSnapshot.inactive(this.config.rounds, worldName, this.config.enemyType);
        }
        int alive = HordeService.countAlive(this.session.activeEnemies);
        long now = System.currentTimeMillis();
        long elapsedSeconds = Math.max(0L, (now - this.session.startedAtMillis) / 1000L);
        long nextRoundInSeconds = this.session.roundActive ? 0L : Math.max(0L, (this.session.nextRoundAtMillis - now + 999L) / 1000L);
        return StatusSnapshot.active(this.session.currentRound, this.config.rounds, alive, this.session.totalSpawned, this.session.totalKilled, this.session.enemyType + " -> " + this.session.role, elapsedSeconds, nextRoundInSeconds, this.session.world.getName());
    }

    private void refreshStatusHud(HordeSession trackedSession) {
        if (this.statusPages.isEmpty()) {
            return;
        }
        World world = trackedSession == null ? null : trackedSession.world;
        StatusSnapshot snapshot = this.createStatusSnapshot(world);
        ArrayList<UUID> staleEntries = new ArrayList<UUID>();
        for (Map.Entry<UUID, HordeStatusPage> entry : this.statusPages.entrySet()) {
            HordeStatusPage page = entry.getValue();
            if (page == null) {
                staleEntries.add(entry.getKey());
                continue;
            }
            try {
                page.updateSnapshot(snapshot);
            }
            catch (Exception ex) {
                staleEntries.add(entry.getKey());
            }
        }
        for (UUID staleEntry : staleEntries) {
            this.statusPages.remove(staleEntry);
        }
    }

    private void unregisterStatusPage(UUID playerId) {
        this.statusPages.remove(playerId);
    }

    private void closeAllStatusPages() {
        for (HordeStatusPage page : this.statusPages.values()) {
            if (page == null) continue;
            try {
                page.closeFromService();
            }
            catch (Exception exception) {}
        }
        this.statusPages.clear();
    }

    private static String chooseRole(List<String> roles, String selectedEnemyType, String legacyConfiguredRole) {
        String normalizedType = HordeService.normalizeEnemyType(selectedEnemyType);
        if ("random".equals(normalizedType)) {
            normalizedType = "auto";
        }
        String[] preferredHints = ENEMY_TYPE_HINTS.get(normalizedType);
        if (preferredHints == null || preferredHints.length == 0) {
            preferredHints = ENEMY_TYPE_HINTS.get("auto");
        }
        String roleByType = HordeService.findRoleByHints(roles, preferredHints);
        if (roleByType != null) {
            return roleByType;
        }
        if (legacyConfiguredRole != null && !legacyConfiguredRole.isBlank()) {
            for (String role : roles) {
                if (!role.equalsIgnoreCase(legacyConfiguredRole)) continue;
                return role;
            }
        }
        String fallbackRole = HordeService.findRoleByHints(roles, ENEMY_TYPE_HINTS.get("auto"));
        if (fallbackRole != null) {
            return fallbackRole;
        }
        return roles.get(0);
    }

    private static String findRoleByHints(List<String> roles, String[] hints) {
        if (hints == null || hints.length == 0) {
            return null;
        }
        for (String hint : hints) {
            for (String role : roles) {
                if (!role.equalsIgnoreCase(hint)) continue;
                return role;
            }
        }
        for (String hint : hints) {
            for (String role : roles) {
                if (!role.toLowerCase(Locale.ROOT).contains(hint)) continue;
                return role;
            }
        }
        return null;
    }

    private static String normalizeEnemyType(String input) {
        if (input == null || input.isBlank()) {
            return "auto";
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("role")) {
            return "auto";
        }
        if (normalized.equals("aleatorio") || normalized.equals("rand") || normalized.equals("rnd")) {
            return "random";
        }
        return normalized;
    }

    private static Map<String, String[]> buildEnemyTypeHints() {
        LinkedHashMap<String, String[]> hints = new LinkedHashMap<String, String[]>();
        hints.put("auto", new String[]{"enemy", "hostile", "bandit", "goblin", "skeleton", "zombie", "spider", "wolf", "wraith", "void", "demon", "beast"});
        hints.put("random", new String[]{"enemy", "hostile", "bandit", "goblin", "skeleton", "zombie", "spider", "wolf", "wraith", "void", "demon", "beast"});
        hints.put("bandit", new String[]{"bandit", "raider", "outlaw"});
        hints.put("goblin", new String[]{"goblin"});
        hints.put("skeleton", new String[]{"skeleton"});
        hints.put("zombie", new String[]{"zombie", "undead"});
        hints.put("spider", new String[]{"spider", "arachnid"});
        hints.put("wolf", new String[]{"wolf", "direwolf"});
        hints.put("wraith", new String[]{"wraith", "ghost", "specter"});
        hints.put("void", new String[]{"void", "corrupt"});
        hints.put("demon", new String[]{"demon", "fiend"});
        hints.put("beast", new String[]{"beast", "monster"});
        return hints;
    }

    private static List<String> buildRandomEnemyTypePool() {
        ArrayList<String> pool = new ArrayList<String>();
        for (String enemyType : ENEMY_TYPE_HINTS.keySet()) {
            if ("auto".equals(enemyType) || "random".equals(enemyType)) continue;
            pool.add(enemyType);
        }
        if (pool.isEmpty()) {
            pool.add("auto");
        }
        return pool;
    }

    private static List<String> buildRewardItemSuggestions() {
        ArrayList<String> suggestions = new ArrayList<String>();
        suggestions.add("item/weapon/sword-iron");
        suggestions.add("item/weapon/bow-wood");
        suggestions.add("item/armor/chestplate-iron");
        suggestions.add("item/consumable/apple");
        suggestions.add("item/consumable/potion-health-small");
        suggestions.add("item/material/iron-ingot");
        suggestions.add("item/material/gold-ingot");
        suggestions.add("item/resource/wood");
        suggestions.add("item/resource/stone");
        suggestions.add("item/tool/pickaxe-iron");
        return suggestions;
    }

    private static boolean isRandomEnemyType(String enemyType) {
        return "random".equals(HordeService.normalizeEnemyType(enemyType));
    }

    private static String pickRandomEnemyType() {
        if (RANDOM_ENEMY_TYPE_OPTIONS.isEmpty()) {
            return "auto";
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(RANDOM_ENEMY_TYPE_OPTIONS.size());
        return RANDOM_ENEMY_TYPE_OPTIONS.get(randomIndex);
    }

    private static int removeInvalidRefs(Set<Ref<EntityStore>> refs) {
        int removed = 0;
        HashSet<Ref<EntityStore>> stale = new HashSet<Ref<EntityStore>>();
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) continue;
            stale.add(ref);
        }
        for (Ref ref : stale) {
            refs.remove(ref);
            ++removed;
        }
        return removed;
    }

    private static int countAlive(Set<Ref<EntityStore>> refs) {
        int alive = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid()) continue;
            ++alive;
        }
        return alive;
    }

    private static int parseInt(String input, int currentValue, String name) {
        if (input == null || input.isBlank()) {
            return currentValue;
        }
        try {
            return Integer.parseInt(HordeService.normalizeNumberInput(input, false));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " debe ser un numero entero. Valor recibido: " + input);
        }
    }

    private static double parseDouble(String input, double currentValue, String name) {
        if (input == null || input.isBlank()) {
            return currentValue;
        }
        try {
            return Double.parseDouble(HordeService.normalizeNumberInput(input, true));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " debe ser un numero decimal valido. Valor recibido: " + input);
        }
    }

    private static String normalizeNumberInput(String input, boolean decimal) {
        String normalized = input.trim().replace(',', '.');
        if (!decimal && normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }

    private static String compactText(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "Horda finalizada";
        }
        String cleaned = input.trim().replace('\n', ' ').replace('\r', ' ');
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String resolveLogsPath(Path pluginDataDirectory) {
        Path cursor = pluginDataDirectory;
        while (cursor != null) {
            Path fileName = cursor.getFileName();
            if (fileName != null && "mods".equalsIgnoreCase(fileName.toString())) {
                Path saveDirectory = cursor.getParent();
                if (saveDirectory != null) {
                    return saveDirectory.resolve("logs").toString();
                }
                break;
            }
            cursor = cursor.getParent();
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "Hytale", "UserData", "Saves", "Mod-Test", "logs").toString();
        }
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, "AppData", "Roaming", "Hytale", "UserData", "Saves", "Mod-Test", "logs").toString();
    }

    private static String safeRoleValue(String role) {
        return role == null ? "" : role;
    }

    private void loadConfig() {
        try {
            Files.createDirectories(this.plugin.getDataDirectory(), new FileAttribute[0]);
        }
        catch (IOException ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo crear la carpeta de datos para HordeService: %s", (Object)ex.getMessage());
            return;
        }
        if (!Files.exists(this.configPath, new LinkOption[0])) {
            this.saveConfig();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(this.configPath, StandardCharsets.UTF_8);){
            HordeConfig loaded = (HordeConfig)this.gson.fromJson((Reader)reader, HordeConfig.class);
            if (loaded != null) {
                this.config = HordeService.sanitize(loaded);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo leer horde-config.json, se usaran valores por defecto: %s", (Object)ex.getMessage());
            this.config = HordeConfig.defaults();
            this.saveConfig();
        }
    }

    private void saveConfig() {
        try {
            Files.createDirectories(this.plugin.getDataDirectory(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(this.configPath, StandardCharsets.UTF_8, new OpenOption[0]);){
                this.gson.toJson((Object)this.config, (Appendable)writer);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo guardar la configuracion de hordas: %s", (Object)ex.getMessage());
        }
    }

    private static HordeConfig sanitize(HordeConfig source) {
        HordeConfig sanitized = source.copy();
        if (sanitized.rounds < 1) {
            sanitized.rounds = HordeConfig.defaults().rounds;
        } else if (sanitized.rounds > 200) {
            sanitized.rounds = 200;
        }
        if (sanitized.baseEnemiesPerRound < 1) {
            sanitized.baseEnemiesPerRound = HordeConfig.defaults().baseEnemiesPerRound;
        } else if (sanitized.baseEnemiesPerRound > 250) {
            sanitized.baseEnemiesPerRound = 250;
        }
        if (sanitized.enemiesPerRoundIncrement < 0) {
            sanitized.enemiesPerRoundIncrement = HordeConfig.defaults().enemiesPerRoundIncrement;
        } else if (sanitized.enemiesPerRoundIncrement > 250) {
            sanitized.enemiesPerRoundIncrement = 250;
        }
        if (sanitized.waveDelaySeconds < 0) {
            sanitized.waveDelaySeconds = HordeConfig.defaults().waveDelaySeconds;
        } else if (sanitized.waveDelaySeconds > 300) {
            sanitized.waveDelaySeconds = 300;
        }
        if (sanitized.playerMultiplier < MIN_PLAYER_MULTIPLIER) {
            sanitized.playerMultiplier = HordeConfig.defaults().playerMultiplier;
        } else if (sanitized.playerMultiplier > MAX_PLAYER_MULTIPLIER) {
            sanitized.playerMultiplier = MAX_PLAYER_MULTIPLIER;
        }
        if (sanitized.rewardEveryRounds <= 0) {
            sanitized.rewardEveryRounds = HordeConfig.defaults().rewardEveryRounds;
        }
        if (sanitized.rewardItemQuantity <= 0) {
            sanitized.rewardItemQuantity = HordeConfig.defaults().rewardItemQuantity;
        } else if (sanitized.rewardItemQuantity > 9999) {
            sanitized.rewardItemQuantity = 9999;
        }
        if (sanitized.rewardItemId == null) {
            sanitized.rewardItemId = "";
        } else {
            sanitized.rewardItemId = sanitized.rewardItemId.trim();
        }
        sanitized.minSpawnRadius = HordeService.clamp(sanitized.minSpawnRadius, 1.0, 128.0);
        sanitized.maxSpawnRadius = HordeService.clamp(sanitized.maxSpawnRadius, 1.0, 128.0);
        if (sanitized.maxSpawnRadius < sanitized.minSpawnRadius) {
            double temp = sanitized.minSpawnRadius;
            sanitized.minSpawnRadius = sanitized.maxSpawnRadius;
            sanitized.maxSpawnRadius = temp;
        }
        sanitized.enemyType = HordeService.normalizeEnemyType(sanitized.enemyType);
        if (!ENEMY_TYPE_HINTS.containsKey(sanitized.enemyType)) {
            sanitized.enemyType = "auto";
        }
        sanitized.npcRole = HordeService.safeRoleValue(sanitized.npcRole).trim();
        sanitized.worldName = sanitized.worldName == null || sanitized.worldName.isBlank() ? "default" : sanitized.worldName;
        return sanitized;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static final class HordeConfig {
        public boolean spawnConfigured;
        public String worldName;
        public double spawnX;
        public double spawnY;
        public double spawnZ;
        public double minSpawnRadius;
        public double maxSpawnRadius;
        public int rounds;
        public int baseEnemiesPerRound;
        public int enemiesPerRoundIncrement;
        public int waveDelaySeconds;
        public int playerMultiplier;
        public String enemyType;
        public String npcRole;
        public int rewardEveryRounds;
        public String rewardItemId;
        public int rewardItemQuantity;

        public static HordeConfig defaults() {
            HordeConfig defaults = new HordeConfig();
            defaults.spawnConfigured = false;
            defaults.worldName = "default";
            defaults.spawnX = 0.0;
            defaults.spawnY = 64.0;
            defaults.spawnZ = 0.0;
            defaults.minSpawnRadius = 5.0;
            defaults.maxSpawnRadius = 12.0;
            defaults.rounds = 5;
            defaults.baseEnemiesPerRound = 8;
            defaults.enemiesPerRoundIncrement = 2;
            defaults.waveDelaySeconds = 8;
            defaults.playerMultiplier = 1;
            defaults.enemyType = "auto";
            defaults.npcRole = "";
            defaults.rewardEveryRounds = 2;
            defaults.rewardItemId = "";
            defaults.rewardItemQuantity = 1;
            return defaults;
        }

        public HordeConfig copy() {
            HordeConfig copy = new HordeConfig();
            copy.spawnConfigured = this.spawnConfigured;
            copy.worldName = this.worldName;
            copy.spawnX = this.spawnX;
            copy.spawnY = this.spawnY;
            copy.spawnZ = this.spawnZ;
            copy.minSpawnRadius = this.minSpawnRadius;
            copy.maxSpawnRadius = this.maxSpawnRadius;
            copy.rounds = this.rounds;
            copy.baseEnemiesPerRound = this.baseEnemiesPerRound;
            copy.enemiesPerRoundIncrement = this.enemiesPerRoundIncrement;
            copy.waveDelaySeconds = this.waveDelaySeconds;
            copy.playerMultiplier = this.playerMultiplier;
            copy.enemyType = this.enemyType;
            copy.npcRole = this.npcRole;
            copy.rewardEveryRounds = this.rewardEveryRounds;
            copy.rewardItemId = this.rewardItemId;
            copy.rewardItemQuantity = this.rewardItemQuantity;
            return copy;
        }
    }

    private static final class HordeSession {
        private final World world;
        private final Store<EntityStore> store;
        private final String role;
        private final String enemyType;
        private final List<String> availableRoles;
        private final int playerMultiplier;
        private final Set<Ref<EntityStore>> activeEnemies;
        private int currentRound;
        private int totalSpawned;
        private int totalKilled;
        private boolean roundActive;
        private long nextRoundAtMillis;
        private final long startedAtMillis;
        private ScheduledFuture<?> ticker;

        private HordeSession(World world, Store<EntityStore> store, String role, String enemyType, List<String> availableRoles, int playerMultiplier) {
            this.world = world;
            this.store = store;
            this.role = role;
            this.enemyType = enemyType;
            this.availableRoles = availableRoles == null ? new ArrayList<String>() : new ArrayList<String>(availableRoles);
            this.playerMultiplier = Math.max(MIN_PLAYER_MULTIPLIER, playerMultiplier);
            this.activeEnemies = new HashSet<Ref<EntityStore>>();
            this.currentRound = 0;
            this.totalSpawned = 0;
            this.totalKilled = 0;
            this.roundActive = false;
            this.nextRoundAtMillis = 0L;
            this.startedAtMillis = System.currentTimeMillis();
        }
    }

    public static final class StatusSnapshot {
        public final boolean active;
        public final int currentRound;
        public final int totalRounds;
        public final int aliveEnemies;
        public final int totalSpawned;
        public final int totalKilled;
        public final String role;
        public final long elapsedSeconds;
        public final long nextRoundInSeconds;
        public final String worldName;

        private StatusSnapshot(boolean active, int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName) {
            this.active = active;
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
            this.aliveEnemies = aliveEnemies;
            this.totalSpawned = totalSpawned;
            this.totalKilled = totalKilled;
            this.role = role == null ? "" : role;
            this.elapsedSeconds = Math.max(0L, elapsedSeconds);
            this.nextRoundInSeconds = Math.max(0L, nextRoundInSeconds);
            this.worldName = worldName == null ? "default" : worldName;
        }

        public static StatusSnapshot inactive(int totalRounds, String worldName, String enemyType) {
            return new StatusSnapshot(false, 0, totalRounds, 0, 0, 0, enemyType, 0L, 0L, worldName);
        }

        public static StatusSnapshot active(int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName) {
            return new StatusSnapshot(true, currentRound, totalRounds, aliveEnemies, totalSpawned, totalKilled, role, elapsedSeconds, nextRoundInSeconds, worldName);
        }
    }

    public static final class OperationResult {
        private final boolean success;
        private final String message;

        private OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OperationResult ok(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult fail(String message) {
            return new OperationResult(false, message);
        }

        public boolean isSuccess() {
            return this.success;
        }

        public String getMessage() {
            return this.message;
        }
    }
}


