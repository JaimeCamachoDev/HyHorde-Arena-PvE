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
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final List<String> PREFERRED_REWARD_TEST_ITEMS = List.of("Armor_Bronze_Chest", "Armor_Copper_Chest", "Armor_Iron_Chest", "Tool_Watering_Can_State_Filled_Water", "*Container_Bucket_State_Filled_Water", "item/resource/wood", "item/resource/stone", "item/consumable/apple", "item/material/iron_ingot", "item/weapon/sword_iron");
    private static final String DEFAULT_ENEMY_TYPE = "undead";
    private static final Map<String, String[]> ENEMY_TYPE_HINTS = HordeService.buildEnemyTypeHints();
    private static final List<String> ENEMY_TYPE_OPTIONS = HordeService.buildEnemyTypeOptions();
    private static final List<String> RANDOM_ENEMY_TYPE_OPTIONS = HordeService.buildRandomEnemyTypePool();
    private static final List<String> REWARD_ITEM_SUGGESTIONS = HordeService.buildRewardItemSuggestions();
    private static final String REWARD_MODE_RANDOM = "random";
    private static final int MAX_REWARD_SUGGESTIONS = 72;
    private static final String[] BLOCKED_ENEMY_ROLE_HINTS = new String[]{"kitten", "feline"};
    private static final int START_COUNTDOWN_SECONDS = 3;
    private static final long SESSION_TICK_INTERVAL_MILLIS = 250L;
    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 200;
    private static final int MIN_ENEMIES_PER_ROUND = 1;
    private static final int MAX_ENEMIES_PER_ROUND = 400;
    private static final int MIN_ENEMY_INCREMENT = 0;
    private static final int MAX_ENEMY_INCREMENT = 400;
    private static final int MIN_PLAYER_MULTIPLIER = 1;
    private static final int MAX_PLAYER_MULTIPLIER = 20;
    private static final int MIN_WAVE_DELAY_SECONDS = 0;
    private static final int MAX_WAVE_DELAY_SECONDS = 300;
    private static final int MIN_REWARD_ITEM_QUANTITY = 1;
    private static final int MAX_REWARD_ITEM_QUANTITY = 9999;
    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 128.0;
    private static final String LANGUAGE_SPANISH = "es";
    private static final String LANGUAGE_ENGLISH = "en";
    private static final List<String> LANGUAGE_OPTIONS = List.of(LANGUAGE_SPANISH, LANGUAGE_ENGLISH);
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
        String language = HordeService.normalizeLanguage(this.config.language);
        if (this.session == null) {
            if (HordeService.isEnglishLanguage(language)) {
                return "No active horde. Use /hordapve to open the interface.";
            }
            return "Sin horda activa. Usa /hordapve para abrir la interfaz.";
        }
        this.syncSessionPlayers(this.session);
        int alive = HordeService.countAlive(this.session.activeEnemies);
        int totalDeaths = HordeService.getTotalDeaths(this.session.playerStats);
        String rewardInfo = HordeService.formatRewardInfo(this.config.rewardItemId, this.config.rewardItemQuantity, false);
        if (HordeService.isEnglishLanguage(language)) {
            rewardInfo = HordeService.formatRewardInfo(this.config.rewardItemId, this.config.rewardItemQuantity, true);
            return "Horde active | Round " + this.session.currentRound + "/" + this.config.rounds + " | Remaining enemies: " + alive + " | Total spawned: " + this.session.totalSpawned + " | Kills detected: " + this.session.totalKilled + " | Player deaths: " + totalDeaths + " | Type: " + this.session.enemyType + " | Real role: " + this.session.role + " | Players x" + this.session.playerMultiplier + " | Reward every: " + this.config.rewardEveryRounds + " round(s) | Item: " + rewardInfo;
        }
        return "Horda activa | Ronda " + this.session.currentRound + "/" + this.config.rounds + " | Enemigos vivos: " + alive + " | Spawn total: " + this.session.totalSpawned + " | Kills detectadas: " + this.session.totalKilled + " | Tipo: " + this.session.enemyType + " | Rol real: " + this.session.role + " | Jugadores x" + this.session.playerMultiplier + " | Recompensa por cada: " + this.config.rewardEveryRounds + " ronda(s) | Item: " + rewardInfo;
    }

    public synchronized List<String> getAvailableRoles() {
        return new ArrayList<String>(NPCPlugin.get().getRoleTemplateNames(true));
    }

    public synchronized List<String> getEnemyTypeOptions() {
        return new ArrayList<String>(ENEMY_TYPE_OPTIONS);
    }

    public synchronized List<String> getEnemyTypeOptionsForCurrentRoles() {
        List<String> roles = this.getAvailableRoles();
        if (roles.isEmpty()) {
            return this.getEnemyTypeOptions();
        }
        List<String> supported = HordeService.findSupportedEnemyTypes(roles);
        if (!supported.isEmpty()) {
            return supported;
        }
        return this.getEnemyTypeOptions();
    }

    public synchronized List<String> getEnemyTypeDiagnostics() {
        List<String> roles = this.getAvailableRoles();
        ArrayList<String> diagnostics = new ArrayList<String>();
        for (String enemyType : ENEMY_TYPE_OPTIONS) {
            List<String> matches = HordeService.findRolesForEnemyType(roles, enemyType);
            if (matches.isEmpty()) {
                diagnostics.add(enemyType + " -> NO DISPONIBLE");
                continue;
            }
            int previewCount = Math.min(5, matches.size());
            List<String> preview = matches.subList(0, previewCount);
            String suffix = matches.size() > previewCount ? " ... (+" + (matches.size() - previewCount) + " mas)" : "";
            diagnostics.add(enemyType + " -> " + String.join((CharSequence)", ", preview) + suffix);
        }
        return diagnostics;
    }

    public synchronized List<String> getRewardItemSuggestions() {
        return HordeService.buildResolvedRewardSuggestions();
    }

    public synchronized List<String> getLanguageOptions() {
        return new ArrayList<String>(LANGUAGE_OPTIONS);
    }

    public synchronized String getLanguage() {
        return HordeService.normalizeLanguage(this.config.language);
    }

    public synchronized String getConfiguredNpcRole() {
        return HordeService.safeRoleValue(this.config.npcRole);
    }

    public synchronized String getLogsPathHint() {
        return HordeService.resolveLogsPath(this.plugin.getDataDirectory());
    }

    public synchronized StatusSnapshot getStatusSnapshot() {
        return this.createStatusSnapshot(this.session == null ? null : this.session.world);
    }

    public synchronized boolean isTrackingWorld(World world) {
        if (world == null || this.session == null) {
            return false;
        }
        return this.session.world == world;
    }

    public synchronized boolean isTrackedEnemy(Ref<EntityStore> enemyRef) {
        if (this.session == null || enemyRef == null) {
            return false;
        }
        return this.session.activeEnemies.contains(enemyRef);
    }

    public synchronized void registerEnemyKill(Ref<EntityStore> enemyRef, PlayerRef attackerPlayer) {
        if (this.session == null || enemyRef == null || attackerPlayer == null) {
            return;
        }
        if (!this.session.activeEnemies.contains(enemyRef)) {
            return;
        }
        if (!this.session.accountedEnemyDeaths.add(enemyRef)) {
            return;
        }
        PlayerCombatStats attackerStats = this.getOrCreatePlayerStats(this.session, attackerPlayer);
        attackerStats.kills = attackerStats.kills + 1;
        this.refreshStatusHud(this.session);
    }

    public synchronized void registerPlayerDeath(PlayerRef victimPlayer, Ref<EntityStore> attackerRef) {
        if (this.session == null || victimPlayer == null || attackerRef == null) {
            return;
        }
        if (!this.session.activeEnemies.contains(attackerRef)) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID victimId = victimPlayer.getUuid();
        if (victimId == null) {
            return;
        }
        Long lastRecorded = this.session.lastPlayerDeathAt.get(victimId);
        if (lastRecorded != null && now - lastRecorded.longValue() <= 750L) {
            return;
        }
        this.session.lastPlayerDeathAt.put(victimId, now);
        PlayerCombatStats victimStats = this.getOrCreatePlayerStats(this.session, victimPlayer);
        victimStats.deaths = victimStats.deaths + 1;
        this.refreshStatusHud(this.session);
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
        try {
            HordeStatusPage previous = this.statusPages.remove(playerRef.getUuid());
            if (previous != null) {
                previous.closeFromService();
            }
            StatusSnapshot snapshot = this.createStatusSnapshot(world);
            HordeStatusPage page = HordeStatusPage.open(playerEntityRef, store, player, playerRef, snapshot, this::unregisterStatusPage);
            this.statusPages.put(playerRef.getUuid(), page);
            return OperationResult.ok("Panel de estado de horda abierto.");
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo abrir el panel de estado de horda: %s", (Object)ex.getMessage());
            return OperationResult.fail("No se pudo abrir el panel de estado. Revisa logs del servidor.");
        }
    }

    public synchronized OperationResult setEnemyType(String enemyTypeInput) {
        String enemyType = HordeService.normalizeEnemyType(enemyTypeInput);
        if (!ENEMY_TYPE_HINTS.containsKey(enemyType)) {
            return OperationResult.fail("Tipo invalido. Usa uno de: " + String.join((CharSequence)", ", ENEMY_TYPE_OPTIONS));
        }
        List<String> roles = this.getAvailableRoles();
        if (!roles.isEmpty() && HordeService.resolveRoleForEnemyType(roles, enemyType) == null) {
            return OperationResult.fail("La categoria '" + enemyType + "' no tiene NPCs compatibles en este modpack. Usa /hordapve tipos para revisar.");
        }
        this.config.enemyType = enemyType;
        this.saveConfig();
        return OperationResult.ok("Categoria de enemigos configurada en: " + enemyType);
    }

    public synchronized OperationResult setNpcRole(String npcRoleInput) {
        String raw = HordeService.safeRoleValue(npcRoleInput).trim();
        if (raw.isEmpty() || "auto".equalsIgnoreCase(raw) || "clear".equalsIgnoreCase(raw) || "none".equalsIgnoreCase(raw) || "default".equalsIgnoreCase(raw)) {
            this.config.npcRole = "";
            this.saveConfig();
            return OperationResult.ok("Override de rol NPC desactivado. Se volvera a usar enemyType.");
        }
        List<String> roles = this.getAvailableRoles();
        if (roles.isEmpty()) {
            return OperationResult.fail("No hay roles de NPC disponibles para validar '" + raw + "'.");
        }
        String resolvedRole = HordeService.findRoleByExactName(roles, raw);
        if (resolvedRole == null) {
            List<String> similarRoles = HordeService.findRolesByContains(roles, raw);
            if (similarRoles.size() == 1) {
                resolvedRole = similarRoles.get(0);
            } else if (similarRoles.size() > 1) {
                return OperationResult.fail("Rol ambiguo '" + raw + "'. Coincidencias: " + String.join((CharSequence)", ", similarRoles));
            }
        }
        if (resolvedRole == null) {
            return OperationResult.fail("Rol NPC no encontrado: '" + raw + "'. Usa /hordapve roles.");
        }
        if (HordeService.isBlockedEnemyRole(resolvedRole)) {
            return OperationResult.fail("El rol '" + resolvedRole + "' esta bloqueado para hordas (mascota/no hostil).");
        }
        this.config.npcRole = resolvedRole;
        this.saveConfig();
        return OperationResult.ok("Rol NPC forzado a: " + resolvedRole);
    }

    public synchronized OperationResult setRewardEveryRounds(int everyRounds) {
        if (everyRounds <= 0 || everyRounds > 200) {
            return OperationResult.fail("reward debe estar entre 1 y 200.");
        }
        this.config.rewardEveryRounds = everyRounds;
        this.saveConfig();
        return OperationResult.ok("Recompensas configuradas cada " + everyRounds + " ronda(s).");
    }

    public synchronized OperationResult setLanguage(String languageInput) {
        String language = HordeService.normalizeLanguage(languageInput);
        this.config.language = language;
        this.saveConfig();
        this.refreshStatusHud(this.session);
        if (HordeService.isEnglishLanguage(language)) {
            return OperationResult.ok("Language updated to English.");
        }
        return OperationResult.ok("Idioma actualizado a Espanol.");
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
        String languageValue = values.get("language");
        if (languageValue != null && !languageValue.isBlank()) {
            updated.language = HordeService.normalizeLanguage(languageValue);
        } else {
            updated.language = HordeService.normalizeLanguage(updated.language);
        }
        if (!ENEMY_TYPE_HINTS.containsKey(updated.enemyType)) {
            return OperationResult.fail("enemyType debe ser uno de: " + String.join((CharSequence)", ", ENEMY_TYPE_OPTIONS));
        }
        List<String> roles = this.getAvailableRoles();
        if (!roles.isEmpty() && HordeService.resolveRoleForEnemyType(roles, updated.enemyType) == null) {
            return OperationResult.fail("enemyType '" + updated.enemyType + "' no tiene NPCs compatibles en este modpack.");
        }
        if (updated.rewardEveryRounds <= 0 || updated.rewardEveryRounds > 200) {
            return OperationResult.fail("rewardEveryRounds debe estar entre 1 y 200.");
        }
        String rewardItemIdRaw = values.get("rewardItemId");
        if (rewardItemIdRaw != null) {
            updated.rewardItemId = HordeService.normalizeRewardItemId(rewardItemIdRaw);
        }
        try {
            updated.rewardItemQuantity = HordeService.parseInt(values.get("rewardItemQuantity"), updated.rewardItemQuantity, "rewardItemQuantity");
        }
        catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }
        if (updated.rewardItemQuantity < MIN_REWARD_ITEM_QUANTITY || updated.rewardItemQuantity > MAX_REWARD_ITEM_QUANTITY) {
            return OperationResult.fail("rewardItemQuantity debe estar entre " + MIN_REWARD_ITEM_QUANTITY + " y " + MAX_REWARD_ITEM_QUANTITY + ".");
        }
        boolean randomRewardMode = HordeService.isRandomRewardMode(updated.rewardItemId);
        if (updated.rewardItemId == null || updated.rewardItemId.isBlank() || (!randomRewardMode && !HordeService.isUsableRewardItemId(updated.rewardItemId, updated.rewardItemQuantity))) {
            String fallbackItemId = HordeService.resolveGuaranteedRewardTestItemId(updated.rewardItemQuantity);
            if (fallbackItemId == null || fallbackItemId.isBlank()) {
                // No bloqueamos la UI/arranque por recompensa: se resolvera al dropear.
                updated.rewardItemId = "";
            } else {
                updated.rewardItemId = fallbackItemId;
            }
        }
        if (updated.minSpawnRadius < MIN_RADIUS || updated.minSpawnRadius > MAX_RADIUS) {
            return OperationResult.fail("minRadius debe estar entre " + MIN_RADIUS + " y " + MAX_RADIUS + ".");
        }
        if (updated.maxSpawnRadius < MIN_RADIUS || updated.maxSpawnRadius > MAX_RADIUS) {
            return OperationResult.fail("maxRadius debe estar entre " + MIN_RADIUS + " y " + MAX_RADIUS + ".");
        }
        if (updated.maxSpawnRadius < updated.minSpawnRadius) {
            return OperationResult.fail("maxRadius debe ser mayor o igual a minRadius.");
        }
        if (updated.rounds < MIN_ROUNDS || updated.rounds > MAX_ROUNDS) {
            return OperationResult.fail("rounds debe estar entre " + MIN_ROUNDS + " y " + MAX_ROUNDS + ".");
        }
        if (updated.baseEnemiesPerRound < MIN_ENEMIES_PER_ROUND || updated.baseEnemiesPerRound > MAX_ENEMIES_PER_ROUND) {
            return OperationResult.fail("baseEnemies debe estar entre " + MIN_ENEMIES_PER_ROUND + " y " + MAX_ENEMIES_PER_ROUND + ".");
        }
        if (updated.enemiesPerRoundIncrement < MIN_ENEMY_INCREMENT || updated.enemiesPerRoundIncrement > MAX_ENEMY_INCREMENT) {
            return OperationResult.fail("enemiesPerRound debe estar entre " + MIN_ENEMY_INCREMENT + " y " + MAX_ENEMY_INCREMENT + ".");
        }
        if (updated.playerMultiplier < MIN_PLAYER_MULTIPLIER || updated.playerMultiplier > MAX_PLAYER_MULTIPLIER) {
            return OperationResult.fail("playerMultiplier debe estar entre " + MIN_PLAYER_MULTIPLIER + " y " + MAX_PLAYER_MULTIPLIER + ".");
        }
        if (updated.waveDelaySeconds < MIN_WAVE_DELAY_SECONDS || updated.waveDelaySeconds > MAX_WAVE_DELAY_SECONDS) {
            return OperationResult.fail("waveDelay debe estar entre " + MIN_WAVE_DELAY_SECONDS + " y " + MAX_WAVE_DELAY_SECONDS + " segundos.");
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
        String forcedRole = HordeService.findRoleByExactName(roles, this.config.npcRole);
        if (forcedRole == null && this.config.npcRole != null && !this.config.npcRole.isBlank()) {
            this.plugin.getLogger().at(Level.WARNING).log("El rol NPC forzado '%s' no existe. Se desactivara override.", (Object)this.config.npcRole);
            this.config.npcRole = "";
            this.saveConfig();
        }
        List<String> supportedEnemyTypes = HordeService.findSupportedEnemyTypes(roles);
        String selectedRole = null;
        if (forcedRole != null) {
            selectedRole = forcedRole;
        } else if (HordeService.isRandomEnemyType(selectedEnemyType)) {
            String initialRandomType = HordeService.pickRandomEnemyType(supportedEnemyTypes);
            selectedRole = HordeService.pickRandomRoleForEnemyType(roles, initialRandomType);
            if (selectedRole == null) {
                selectedRole = HordeService.resolveAutoRole(roles);
            }
        } else {
            selectedRole = HordeService.resolveRoleForEnemyType(roles, selectedEnemyType);
            if (selectedRole == null) {
                return OperationResult.fail("La categoria '" + selectedEnemyType + "' no tiene NPCs compatibles en este modpack. Usa /hordapve tipos.");
            }
        }
        if (selectedRole == null) {
            return OperationResult.fail("No se pudo resolver un rol NPC compatible para iniciar la horda.");
        }
        Vector3f startRotation = new Vector3f(startedBy.getTransform().getRotation());
        long firstRoundAtMillis = System.currentTimeMillis() + (long)START_COUNTDOWN_SECONDS * 1000L;
        this.session = newSession = new HordeSession(world, store, selectedRole, selectedEnemyType, new ArrayList<String>(roles), this.config.playerMultiplier, forcedRole, supportedEnemyTypes, startRotation, firstRoundAtMillis);
        this.syncSessionPlayers(newSession);
        newSession.ticker = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> this.tickSession(newSession), 0L, SESSION_TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        String roleSuffix = forcedRole == null ? selectedRole : selectedRole + " (forzado)";
        world.sendMessage(Message.raw((String)String.format(Locale.ROOT, "Horda PVE preparada en %.2f %.2f %.2f | %d rondas | tipo: %s | rol: %s | jugadores x%d | inicio en %ds", this.config.spawnX, this.config.spawnY, this.config.spawnZ, this.config.rounds, selectedEnemyType, roleSuffix, this.config.playerMultiplier, START_COUNTDOWN_SECONDS)));
        this.broadcastHordeStartAnnouncement(selectedEnemyType, selectedRole, this.config.playerMultiplier);
        return OperationResult.ok("Horda iniciada. Cuenta atras: 3..2..1.");
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
        try {
            world.execute(() -> {
                HordeService hordeService = this;
                synchronized (hordeService) {
                    try {
                        if (this.session != trackedSession) {
                            return;
                        }
                        int removed = HordeService.removeInvalidRefs(trackedSession.activeEnemies, trackedSession.accountedEnemyDeaths);
                        if (removed > 0) {
                            trackedSession.totalKilled += removed;
                        }
                        HordeService.removeInvalidRefs(trackedSession.spawnedEnemies, null);
                        this.syncSessionPlayers(trackedSession);
                        long now = System.currentTimeMillis();
                        if (!trackedSession.roundActive && trackedSession.currentRound == 0 && trackedSession.nextRoundAtMillis > 0L) {
                            int secondsRemaining = HordeService.computeRemainingSeconds(now, trackedSession.nextRoundAtMillis);
                            if (now < trackedSession.nextRoundAtMillis) {
                                if (secondsRemaining > 0 && secondsRemaining <= START_COUNTDOWN_SECONDS && secondsRemaining != trackedSession.lastStartCountdownAnnouncement) {
                                    this.broadcastHordeCountdownAnnouncement(secondsRemaining, trackedSession.enemyType, trackedSession.role);
                                    trackedSession.world.sendMessage(Message.raw((String)("La horda comienza en " + secondsRemaining + "...")));
                                    trackedSession.lastStartCountdownAnnouncement = secondsRemaining;
                                }
                                this.refreshStatusHud(trackedSession);
                                return;
                            }
                            this.broadcastHordeCountdownGoAnnouncement(1, this.config.rounds);
                            trackedSession.lastStartCountdownAnnouncement = 0;
                            trackedSession.nextRoundAtMillis = 0L;
                            this.spawnNextRound(trackedSession, new Vector3f(trackedSession.startRotation));
                            this.refreshStatusHud(trackedSession);
                            return;
                        }
                        if (trackedSession.roundActive && trackedSession.activeEnemies.isEmpty()) {
                            trackedSession.roundActive = false;
                            this.grantRoundRewards(trackedSession, trackedSession.currentRound);
                            boolean rewardUnlocked = trackedSession.currentRound > 0 && this.config.rewardEveryRounds > 0 && trackedSession.currentRound % this.config.rewardEveryRounds == 0;
                            boolean finalRound = trackedSession.currentRound >= this.config.rounds;
                            int nextDelaySeconds = finalRound ? 0 : this.config.waveDelaySeconds;
                            this.broadcastRoundCompleteAnnouncement(trackedSession.currentRound, this.config.rounds, nextDelaySeconds, trackedSession.totalKilled, trackedSession.totalSpawned, rewardUnlocked, finalRound);
                            if (finalRound) {
                                this.endSession(trackedSession, "Horda completada. Todas las rondas terminadas.", false);
                                return;
                            }
                            trackedSession.nextRoundAtMillis = now + (long)this.config.waveDelaySeconds * 1000L;
                            trackedSession.lastIntermissionCountdownAnnouncement = -1;
                            world.sendMessage(Message.raw((String)("Ronda " + trackedSession.currentRound + " completada. La siguiente ronda empieza en " + this.config.waveDelaySeconds + "s.")));
                        }
                        if (!trackedSession.roundActive && trackedSession.currentRound > 0 && trackedSession.currentRound < this.config.rounds) {
                            if (trackedSession.nextRoundAtMillis > now) {
                                int secondsRemaining = HordeService.computeRemainingSeconds(now, trackedSession.nextRoundAtMillis);
                                if (secondsRemaining > 0 && secondsRemaining <= START_COUNTDOWN_SECONDS && secondsRemaining != trackedSession.lastIntermissionCountdownAnnouncement) {
                                    this.broadcastRoundCountdownAnnouncement(trackedSession.currentRound + 1, this.config.rounds, secondsRemaining);
                                    trackedSession.lastIntermissionCountdownAnnouncement = secondsRemaining;
                                }
                            } else {
                                this.spawnNextRound(trackedSession, Vector3f.ZERO);
                            }
                        }
                        this.refreshStatusHud(trackedSession);
                    }
                    catch (Exception ex) {
                        this.plugin.getLogger().at(Level.WARNING).log("Error interno en tick de horda: %s", (Object)ex.getMessage());
                    }
                }
            });
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo ejecutar tick de horda en el mundo activo: %s", (Object)ex.getMessage());
            this.endSession(trackedSession, "Horda finalizada por error interno en tick.", false);
        }
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
                if (sessionToAdvance.forcedRole != null && !sessionToAdvance.forcedRole.isBlank()) {
                    roleForSpawn = sessionToAdvance.forcedRole;
                } else if (HordeService.isRandomEnemyType(sessionToAdvance.enemyType)) {
                    String randomEnemyType = HordeService.pickRandomEnemyType(sessionToAdvance.randomEnemyTypes);
                    String resolvedRandomRole = HordeService.pickRandomRoleForEnemyType(sessionToAdvance.availableRoles, randomEnemyType);
                    if (resolvedRandomRole != null) {
                        roleForSpawn = resolvedRandomRole;
                    }
                } else {
                    String resolvedCategoryRole = HordeService.pickRandomRoleForEnemyType(sessionToAdvance.availableRoles, sessionToAdvance.enemyType);
                    if (resolvedCategoryRole != null) {
                        roleForSpawn = resolvedCategoryRole;
                    }
                }
                if (roleForSpawn == null || roleForSpawn.isBlank()) {
                    continue;
                }
                Pair created = NPCPlugin.get().spawnNPC(sessionToAdvance.store, roleForSpawn, null, spawnPosition, new Vector3f(baseRotation));
                if (created == null || created.left() == null) continue;
                Ref<EntityStore> enemyRef = (Ref<EntityStore>)((Ref)created.left());
                sessionToAdvance.activeEnemies.add(enemyRef);
                sessionToAdvance.spawnedEnemies.add(enemyRef);
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
        this.broadcastRoundStartAnnouncement(nextRound, this.config.rounds, spawned, playerMultiplier);
    }

    private void grantRoundRewards(HordeSession trackedSession, int completedRound) {
        if (completedRound <= 0 || this.config.rewardEveryRounds <= 0) {
            return;
        }
        if (completedRound % this.config.rewardEveryRounds != 0) {
            return;
        }
        trackedSession.world.sendMessage(Message.raw((String)("Recompensa desbloqueada por completar la ronda " + completedRound + ".")));
        int rewardQuantity = Math.max(1, this.config.rewardItemQuantity);
        String configuredRewardItemId = HordeService.normalizeRewardItemId(this.config.rewardItemId);
        boolean configuredRandom = HordeService.isRandomRewardMode(configuredRewardItemId);
        boolean configuredValid = configuredRandom || HordeService.isUsableRewardItemId(configuredRewardItemId, rewardQuantity);
        String rewardItemId = this.resolveRewardItemIdForDrop(rewardQuantity);
        if (rewardItemId == null || rewardItemId.isBlank()) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo dropear recompensa: no hay rewardItemId valido configurado o disponible.");
            trackedSession.world.sendMessage(Message.raw((String)"No se pudo dropear recompensa: configura un item valido en RewardItemId."));
            return;
        }
        if (!configuredValid && !configuredRandom) {
            this.config.rewardItemId = rewardItemId;
            this.saveConfig();
            trackedSession.world.sendMessage(Message.raw((String)("RewardItemId vacio/invalido. Se usara automaticamente: " + rewardItemId + ".")));
        }
        ItemStack rewardStack = new ItemStack(rewardItemId, rewardQuantity);
        Vector3d dropPosition = new Vector3d(this.config.spawnX, this.config.spawnY + 1.0, this.config.spawnZ);
        Holder itemEntityHolder = ItemComponent.generateItemDrop(trackedSession.store, rewardStack, dropPosition, Vector3f.ZERO, 0.0f, 0.35f, 0.0f);
        if (itemEntityHolder == null) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo generar la entidad de recompensa para '%s'.", (Object)rewardItemId);
            return;
        }
        ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
        if (itemComponent != null) {
            itemComponent.setPickupDelay(0.6f);
        }
        trackedSession.store.addEntity(itemEntityHolder, AddReason.SPAWN);
        trackedSession.world.sendMessage(Message.raw((String)("Item recompensa dropeado en el centro: " + rewardItemId + " x" + rewardQuantity + ".")));
    }

    private String resolveRewardItemIdForDrop(int quantity) {
        String configured = HordeService.normalizeRewardItemId(this.config.rewardItemId);
        if (HordeService.isRandomRewardMode(configured)) {
            return this.pickRandomRewardItemId(quantity);
        }
        if (HordeService.isUsableRewardItemId(configured, quantity)) {
            return configured;
        }
        for (String suggestion : this.getRewardItemSuggestions()) {
            String normalized = HordeService.normalizeRewardItemId(suggestion);
            if (HordeService.isRandomRewardMode(normalized)) {
                continue;
            }
            if (!HordeService.isUsableRewardItemId(normalized, quantity)) {
                continue;
            }
            return normalized;
        }
        return HordeService.resolveGuaranteedRewardTestItemId(quantity);
    }

    private String pickRandomRewardItemId(int quantity) {
        ArrayList<String> pool = new ArrayList<String>();
        for (String suggestion : this.getRewardItemSuggestions()) {
            String normalized = HordeService.normalizeRewardItemId(suggestion);
            if (normalized.isBlank() || HordeService.isRandomRewardMode(normalized) || !HordeService.isUsableRewardItemId(normalized, quantity)) continue;
            pool.add(normalized);
        }
        if (pool.isEmpty()) {
            return HordeService.resolveGuaranteedRewardTestItemId(quantity);
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(pool.size());
        return pool.get(randomIndex);
    }

    private void broadcastHordeStartAnnouncement(String enemyType, String role, int playerMultiplier) {
        try {
            String titleText = "HORDA PVE PREPARADA";
            String subtitleText = String.format(Locale.ROOT, "Tipo: %s | Rol: %s | Rondas: %d | Jugadores x%d | Inicio en %ds", enemyType, role, this.config.rounds, playerMultiplier, START_COUNTDOWN_SECONDS);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 1.2f, 0.1f, 0.15f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar anuncio global de inicio de horda: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastHordeCountdownAnnouncement(int secondsRemaining, String enemyType, String role) {
        try {
            String titleText = "HORDA EN " + secondsRemaining + "...";
            String subtitleText = String.format(Locale.ROOT, "Tipo: %s | Rol: %s", enemyType, role);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 0.8f, 0.15f, 0.15f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar countdown de horda: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastHordeCountdownGoAnnouncement(int round, int totalRounds) {
        try {
            String titleText = "A LUCHAR";
            String subtitleText = String.format(Locale.ROOT, "Ronda %d/%d iniciada", round, totalRounds);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 1.6f, 0.15f, 0.2f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar anuncio GO de horda: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastRoundCompleteAnnouncement(int completedRound, int totalRounds, int nextDelaySeconds, int totalKilled, int totalSpawned, boolean rewardUnlocked, boolean finalRound) {
        try {
            String titleText = String.format(Locale.ROOT, "RONDA %d/%d COMPLETADA", completedRound, totalRounds);
            String rewardText = rewardUnlocked ? " | Recompensa desbloqueada" : "";
            String subtitleText = finalRound ? String.format(Locale.ROOT, "Horda completada | Kills: %d | Spawn: %d%s", totalKilled, totalSpawned, rewardText) : String.format(Locale.ROOT, "Siguiente en %ds | Kills: %d | Spawn: %d%s", nextDelaySeconds, totalKilled, totalSpawned, rewardText);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", finalRound ? 3.2f : 2.8f, 0.4f, 0.5f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar anuncio de ronda completada: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastRoundCountdownAnnouncement(int round, int totalRounds, int secondsRemaining) {
        try {
            String titleText = String.format(Locale.ROOT, "RONDA %d EN %d...", round, secondsRemaining);
            String subtitleText = String.format(Locale.ROOT, "Preparados para %d/%d", round, totalRounds);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 0.9f, 0.12f, 0.12f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar countdown entre rondas: %s", (Object)ex.getMessage());
        }
    }

    private void broadcastRoundStartAnnouncement(int round, int totalRounds, int spawned, int playerMultiplier) {
        try {
            String titleText = String.format(Locale.ROOT, "RONDA %d/%d", round, totalRounds);
            String subtitleText = String.format(Locale.ROOT, "Enemigos: %d | Jugadores x%d", spawned, playerMultiplier);
            EventTitleUtil.showEventTitleToUniverse(Message.raw((String)titleText), Message.raw((String)subtitleText), true, "", 1.7f, 0.15f, 0.2f);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo mostrar inicio de ronda: %s", (Object)ex.getMessage());
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
        int cleanedEnemies = cleanupAliveEnemies ? this.cleanupTrackedEnemies(trackedSession) : 0;
        int aliveAtEnd = HordeService.countAlive(trackedSession.activeEnemies);
        trackedSession.activeEnemies.clear();
        trackedSession.spawnedEnemies.clear();
        this.session = null;
        String cleanInfo = cleanupAliveEnemies ? " | limpiados: " + cleanedEnemies : "";
        trackedSession.world.sendMessage(Message.raw((String)(reason + " (vivos restantes: " + aliveAtEnd + cleanInfo + ").")));
        this.broadcastHordeEndAnnouncement(reason, aliveAtEnd);
        this.refreshStatusHud(null);
    }

    private int cleanupTrackedEnemies(HordeSession trackedSession) {
        LinkedHashSet<Ref<EntityStore>> refsToCleanup = new LinkedHashSet<Ref<EntityStore>>();
        refsToCleanup.addAll(trackedSession.activeEnemies);
        refsToCleanup.addAll(trackedSession.spawnedEnemies);
        int cleaned = 0;
        for (Ref<EntityStore> enemyRef : refsToCleanup) {
            if (enemyRef == null || !enemyRef.isValid()) {
                continue;
            }
            try {
                trackedSession.store.removeEntity(enemyRef, RemoveReason.REMOVE);
                ++cleaned;
            }
            catch (Exception exception) {
                // ignore individual remove failures and continue cleanup
            }
        }
        return cleaned;
    }

    private StatusSnapshot createStatusSnapshot(World requestedWorld) {
        String language = HordeService.normalizeLanguage(this.config.language);
        if (this.session == null) {
            String worldName = requestedWorld != null ? requestedWorld.getName() : this.config.worldName;
            return StatusSnapshot.inactive(this.config.rounds, worldName, this.config.enemyType, language);
        }
        this.syncSessionPlayers(this.session);
        int alive = HordeService.countAlive(this.session.activeEnemies);
        int totalDeaths = HordeService.getTotalDeaths(this.session.playerStats);
        long now = System.currentTimeMillis();
        long elapsedSeconds = Math.max(0L, (now - this.session.startedAtMillis) / 1000L);
        long nextRoundInSeconds = this.session.roundActive ? 0L : Math.max(0L, (this.session.nextRoundAtMillis - now + 999L) / 1000L);
        List<PlayerSnapshot> players = HordeService.buildPlayerSnapshots(this.session.playerStats);
        return StatusSnapshot.active(this.session.currentRound, this.config.rounds, alive, this.session.totalSpawned, this.session.totalKilled, totalDeaths, this.session.enemyType + " -> " + this.session.role, elapsedSeconds, nextRoundInSeconds, this.session.world.getName(), language, players);
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

    private void syncSessionPlayers(HordeSession trackedSession) {
        if (trackedSession == null || trackedSession.world == null) {
            return;
        }
        for (PlayerRef playerRef : trackedSession.world.getPlayerRefs()) {
            if (playerRef == null || playerRef.getUuid() == null) continue;
            this.getOrCreatePlayerStats(trackedSession, playerRef);
        }
    }

    private PlayerCombatStats getOrCreatePlayerStats(HordeSession trackedSession, PlayerRef playerRef) {
        UUID playerId;
        if (trackedSession == null || playerRef == null || (playerId = playerRef.getUuid()) == null) {
            return null;
        }
        PlayerCombatStats stats = trackedSession.playerStats.get(playerId);
        if (stats == null) {
            stats = new PlayerCombatStats(HordeService.safePlayerName(playerRef));
            trackedSession.playerStats.put(playerId, stats);
        } else {
            stats.username = HordeService.safePlayerName(playerRef);
        }
        return stats;
    }

    private static String safePlayerName(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUsername() == null || playerRef.getUsername().isBlank()) {
            return "Jugador";
        }
        return playerRef.getUsername();
    }

    private static List<PlayerSnapshot> buildPlayerSnapshots(Map<UUID, PlayerCombatStats> statsByPlayer) {
        ArrayList<PlayerSnapshot> lines = new ArrayList<PlayerSnapshot>();
        for (Map.Entry<UUID, PlayerCombatStats> entry : statsByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerCombatStats stats = entry.getValue();
            if (playerId == null || stats == null) continue;
            lines.add(new PlayerSnapshot(playerId, stats.username, Math.max(0, stats.kills), Math.max(0, stats.deaths)));
        }
        lines.sort(Comparator.comparingInt((PlayerSnapshot row) -> row.kills).reversed().thenComparingInt(row -> row.deaths).thenComparing(row -> row.username, String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    private static int getTotalDeaths(Map<UUID, PlayerCombatStats> statsByPlayer) {
        int total = 0;
        for (PlayerCombatStats stats : statsByPlayer.values()) {
            if (stats == null) continue;
            total += Math.max(0, stats.deaths);
        }
        return total;
    }

    private static String resolveRoleForEnemyType(List<String> roles, String selectedEnemyType) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        List<String> matches = HordeService.findRolesForEnemyType(roles, selectedEnemyType);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private static String resolveAutoRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        String fallbackRole = HordeService.resolveRoleForEnemyType(roles, DEFAULT_ENEMY_TYPE);
        if (fallbackRole != null) {
            return fallbackRole;
        }
        return HordeService.findFirstAllowedRole(roles);
    }

    private static List<String> findRolesForEnemyType(List<String> roles, String selectedEnemyType) {
        ArrayList<String> matches = new ArrayList<String>();
        if (roles == null || roles.isEmpty()) {
            return matches;
        }
        String normalizedType = HordeService.normalizeEnemyType(selectedEnemyType);
        String[] preferredHints = ENEMY_TYPE_HINTS.get(normalizedType);
        if (preferredHints == null || preferredHints.length == 0) {
            return matches;
        }
        LinkedHashSet<String> orderedMatches = new LinkedHashSet<String>();
        for (String hint : preferredHints) {
            if (hint == null || hint.isBlank()) {
                continue;
            }
            for (String role : roles) {
                if (HordeService.isBlockedEnemyRole(role)) {
                    continue;
                }
                if (role.equalsIgnoreCase(hint)) {
                    orderedMatches.add(role);
                }
            }
        }
        for (String hint : preferredHints) {
            if (hint == null || hint.isBlank()) {
                continue;
            }
            String normalizedHint = hint.toLowerCase(Locale.ROOT);
            for (String role : roles) {
                if (HordeService.isBlockedEnemyRole(role)) {
                    continue;
                }
                if (role.equalsIgnoreCase(hint)) {
                    continue;
                }
                if (!role.toLowerCase(Locale.ROOT).contains(normalizedHint)) {
                    continue;
                }
                orderedMatches.add(role);
            }
        }
        matches.addAll(orderedMatches);
        return matches;
    }

    private static String pickRandomRoleForEnemyType(List<String> roles, String selectedEnemyType) {
        List<String> matches = HordeService.findRolesForEnemyType(roles, selectedEnemyType);
        if (matches.isEmpty()) {
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(matches.size());
        return matches.get(randomIndex);
    }

    private static List<String> findSupportedEnemyTypes(List<String> roles) {
        ArrayList<String> supported = new ArrayList<String>();
        if (roles == null || roles.isEmpty()) {
            return supported;
        }
        for (String enemyType : ENEMY_TYPE_OPTIONS) {
            if (HordeService.resolveRoleForEnemyType(roles, enemyType) == null) {
                continue;
            }
            supported.add(enemyType);
        }
        return supported;
    }

    private static String findRoleByHints(List<String> roles, String[] hints) {
        if (hints == null || hints.length == 0) {
            return null;
        }
        for (String hint : hints) {
            for (String role : roles) {
                if (HordeService.isBlockedEnemyRole(role)) continue;
                if (!role.equalsIgnoreCase(hint)) continue;
                return role;
            }
        }
        for (String hint : hints) {
            for (String role : roles) {
                if (HordeService.isBlockedEnemyRole(role)) continue;
                if (!role.toLowerCase(Locale.ROOT).contains(hint)) continue;
                return role;
            }
        }
        return null;
    }

    private static String findRoleByExactName(List<String> roles, String requestedRole) {
        if (roles == null || roles.isEmpty() || requestedRole == null || requestedRole.isBlank()) {
            return null;
        }
        for (String role : roles) {
            if (HordeService.isBlockedEnemyRole(role)) continue;
            if (!role.equalsIgnoreCase(requestedRole)) {
                continue;
            }
            return role;
        }
        return null;
    }

    private static List<String> findRolesByContains(List<String> roles, String token) {
        ArrayList<String> matches = new ArrayList<String>();
        if (roles == null || roles.isEmpty() || token == null || token.isBlank()) {
            return matches;
        }
        String normalizedToken = token.trim().toLowerCase(Locale.ROOT);
        for (String role : roles) {
            if (HordeService.isBlockedEnemyRole(role)) continue;
            if (!role.toLowerCase(Locale.ROOT).contains(normalizedToken)) {
                continue;
            }
            matches.add(role);
        }
        return matches;
    }

    private static String findFirstAllowedRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        for (String role : roles) {
            if (HordeService.isBlockedEnemyRole(role)) continue;
            return role;
        }
        return null;
    }

    private static boolean isBlockedEnemyRole(String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        String normalizedRole = role.toLowerCase(Locale.ROOT);
        if (normalizedRole.equals("cat") || normalizedRole.contains("/cat") || normalizedRole.contains("cat/") || normalizedRole.contains("-cat") || normalizedRole.contains("cat-") || normalizedRole.contains("_cat") || normalizedRole.contains("cat_")) {
            return true;
        }
        for (String blockedHint : BLOCKED_ENEMY_ROLE_HINTS) {
            if (!normalizedRole.contains(blockedHint)) continue;
            return true;
        }
        return false;
    }

    public static boolean isEnglishLanguage(String language) {
        return LANGUAGE_ENGLISH.equals(HordeService.normalizeLanguage(language));
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return LANGUAGE_SPANISH;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if ("english".equals(normalized) || "ing".equals(normalized) || "en_us".equals(normalized) || "en-gb".equals(normalized) || normalized.contains("english") || normalized.endsWith("(en)")) {
            return LANGUAGE_ENGLISH;
        }
        if ("espanol".equals(normalized) || "espa\u00f1ol".equals(normalized) || "spa".equals(normalized) || normalized.contains("espanol") || normalized.contains("espa\u00f1ol") || normalized.endsWith("(es)")) {
            return LANGUAGE_SPANISH;
        }
        if (!LANGUAGE_OPTIONS.contains(normalized)) {
            return LANGUAGE_SPANISH;
        }
        return normalized;
    }

    public static String getLanguageDisplay(String language) {
        String normalized = HordeService.normalizeLanguage(language);
        if (LANGUAGE_ENGLISH.equals(normalized)) {
            return "English (en)";
        }
        return "Espanol (es)";
    }

    private static String normalizeEnemyType(String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_ENEMY_TYPE;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u');
        normalized = normalized.replace('_', '-').replace(' ', '-');
        switch (normalized) {
            case "undead":
            case "no-muerto":
            case "no-muertos":
            case "nomuerto":
            case "nomuertos":
            case "zombie":
            case "zombies":
            case "skeleton":
            case "skeletons": {
                return "undead";
            }
            case "goblin":
            case "goblins": {
                return "goblins";
            }
            case "scarak":
            case "insecto":
            case "insectos":
            case "colmena":
            case "hive": {
                return "scarak";
            }
            case "void":
            case "vacio":
            case "corrupcion":
            case "corruption": {
                return "void";
            }
            case "wild":
            case "agresiva":
            case "agresivas":
            case "agresivo":
            case "agresivos":
            case "criatura":
            case "criaturas":
            case "criaturas-agresivas":
            case "bestia":
            case "bestias":
            case "bandit":
            case "outlander":
            case "trork":
            case "spider":
            case "wolf":
            case "slime":
            case "beetle": {
                return "wild";
            }
            case "elemental":
            case "elementales":
            case "elementals": {
                return "elementals";
            }
            case "auto":
            case "role":
            case "random":
            case "aleatorio":
            case "aleatoria":
            case "rand":
            case "rnd": {
                return DEFAULT_ENEMY_TYPE;
            }
        }
        return normalized;
    }

    private static Map<String, String[]> buildEnemyTypeHints() {
        LinkedHashMap<String, String[]> hints = new LinkedHashMap<String, String[]>();
        hints.put("undead", new String[]{"Chicken_Undead", "Cow_Undead", "Aberrant_Zombie", "Burnt_Zombie", "Burnt_Skeleton_Soldier", "Burnt_Skeleton_Archer", "Burnt_Skeleton_Gunner", "Burnt_Skeleton_Lancer", "Burnt_Skeleton_Knight", "Burnt_Skeleton_Praetorian", "Burnt_Skeleton_Wizard", "Dungeon_Skeleton_Sand_Archer", "Dungeon_Skeleton_Sand_Assassin", "Dungeon_Skeleton_Sand_Mage", "Armored_Skeleton_Horse"});
        hints.put("goblins", new String[]{"Goblin_Scavenger", "Goblin_Thief", "Goblin_Scrapper", "Goblin_Miner", "Goblin_Lobber", "Goblin_Ogre", "Goblin_Hermit", "Goblin_Duke"});
        hints.put("scarak", new String[]{"Dungeon_Scarak_Fighter", "Dungeon_Scarak_Defender", "Dungeon_Scarak_Seeker", "Dungeon_Scarak_Louse", "Dungeon_Scarak_Broodmother_Young"});
        hints.put("void", new String[]{"Crawler_Void", "VoidSpawn", "VoidTaken"});
        hints.put("wild", new String[]{"Crocodile", "Black_Wolf", "Cave_Raptor", "Cave_Rex", "Cave_Spider", "Spider", "Feran_Burrower", "Feran_Longtooth", "Feran_Sharptooth"});
        hints.put("elementals", new String[]{"Earthen_Golem", "Ember_Golem", "Frost_Elemental", "Fire_Elemental"});
        return hints;
    }

    private static List<String> buildEnemyTypeOptions() {
        return new ArrayList<String>(ENEMY_TYPE_HINTS.keySet());
    }

    private static List<String> buildRandomEnemyTypePool() {
        ArrayList<String> pool = new ArrayList<String>();
        for (String enemyType : ENEMY_TYPE_OPTIONS) {
            pool.add(enemyType);
        }
        if (pool.isEmpty()) {
            pool.add(DEFAULT_ENEMY_TYPE);
        }
        return pool;
    }

    private static List<String> buildRewardItemSuggestions() {
        LinkedHashSet<String> suggestions = new LinkedHashSet<String>();
        suggestions.add(REWARD_MODE_RANDOM);
        String guaranteedTestItem = HordeService.resolveGuaranteedRewardTestItemId(1);
        if (guaranteedTestItem != null && !guaranteedTestItem.isBlank()) {
            suggestions.add(guaranteedTestItem);
        }
        HordeService.addUsableRewardSuggestions(suggestions, PREFERRED_REWARD_TEST_ITEMS);
        HordeService.addUsableRewardSuggestions(suggestions, HordeService.buildFallbackRewardSuggestions());
        ArrayList<String> result = new ArrayList<String>(suggestions);
        if (result.size() > MAX_REWARD_SUGGESTIONS) {
            return new ArrayList<String>(result.subList(0, MAX_REWARD_SUGGESTIONS));
        }
        return result;
    }

    private static List<String> buildResolvedRewardSuggestions() {
        if (!REWARD_ITEM_SUGGESTIONS.isEmpty()) {
            return new ArrayList<String>(REWARD_ITEM_SUGGESTIONS);
        }
        LinkedHashSet<String> resolved = new LinkedHashSet<String>();
        String guaranteedTestItem = HordeService.resolveGuaranteedRewardTestItemId(1);
        if (guaranteedTestItem != null && !guaranteedTestItem.isBlank()) {
            resolved.add(guaranteedTestItem);
        }
        HordeService.addUsableRewardSuggestions(resolved, HordeService.buildFallbackRewardSuggestions());
        return new ArrayList<String>(resolved);
    }

    private static String resolveDefaultRewardItemId() {
        List<String> resolved = HordeService.buildResolvedRewardSuggestions();
        if (resolved.isEmpty()) {
            return HordeService.resolveGuaranteedRewardTestItemId(1);
        }
        return resolved.get(0);
    }

    private static String resolveGuaranteedRewardTestItemId(int quantity) {
        int safeQuantity = Math.max(MIN_REWARD_ITEM_QUANTITY, quantity);
        for (String preferred : PREFERRED_REWARD_TEST_ITEMS) {
            String normalized = HordeService.normalizeRewardItemId(preferred);
            if (!HordeService.isUsableRewardItemId(normalized, safeQuantity)) {
                continue;
            }
            return normalized;
        }
        for (String fallback : HordeService.buildFallbackRewardSuggestions()) {
            String normalized = HordeService.normalizeRewardItemId(fallback);
            if (!HordeService.isUsableRewardItemId(normalized, safeQuantity)) {
                continue;
            }
            return normalized;
        }
        return "";
    }

    private static String normalizeRewardItemId(String input) {
        if (input == null) {
            return "";
        }
        String raw = input.trim();
        if (raw.isBlank()) {
            return "";
        }
        if (HordeService.isRandomRewardAlias(raw)) {
            return REWARD_MODE_RANDOM;
        }
        LinkedHashSet<String> candidates = HordeService.buildRewardItemIdCandidates(raw);
        String firstCandidate = "";
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (firstCandidate.isBlank()) {
                firstCandidate = candidate;
            }
            String resolved = HordeService.resolveRewardItemAssetId(candidate);
            if (HordeService.isUsableRewardItemIdRaw(resolved, 1)) {
                return resolved;
            }
        }
        return firstCandidate.isBlank() ? raw : firstCandidate;
    }

    private static boolean isRandomRewardAlias(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return "random".equals(normalized) || "rand".equals(normalized) || "rnd".equals(normalized) || "aleatorio".equals(normalized) || "aleatoria".equals(normalized);
    }

    private static boolean isRandomRewardMode(String itemId) {
        return REWARD_MODE_RANDOM.equalsIgnoreCase(itemId == null ? "" : itemId.trim());
    }

    private static LinkedHashSet<String> buildRewardItemIdCandidates(String rawInput) {
        LinkedHashSet<String> candidates = new LinkedHashSet<String>();
        if (rawInput == null || rawInput.isBlank()) {
            return candidates;
        }
        String raw = rawInput.trim();
        HordeService.addRewardItemCandidateVariants(candidates, raw);
        String withoutIndex = raw.replaceFirst("^\\d+\\.\\s*", "");
        HordeService.addRewardItemCandidateVariants(candidates, withoutIndex);
        String withoutTypeLabel = withoutIndex.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
        HordeService.addRewardItemCandidateVariants(candidates, withoutTypeLabel);
        int metadataStart = withoutTypeLabel.indexOf(" [");
        if (metadataStart > 0) {
            HordeService.addRewardItemCandidateVariants(candidates, withoutTypeLabel.substring(0, metadataStart).trim());
        }
        String[] tokens = withoutTypeLabel.split("\\s+");
        for (String token : tokens) {
            HordeService.addRewardItemCandidateVariants(candidates, token);
        }
        return candidates;
    }

    private static void addRewardItemCandidateVariants(Set<String> candidates, String rawCandidate) {
        if (rawCandidate == null || rawCandidate.isBlank()) {
            return;
        }
        String candidate = HordeService.cleanRewardToken(rawCandidate);
        if (candidate.isBlank() || candidate.matches("\\d+") || candidate.contains(" ")) {
            return;
        }
        if (candidate.startsWith("server.") || candidate.startsWith("client.") || candidate.endsWith(".name")) {
            return;
        }
        candidates.add(candidate);
        String lower = candidate.toLowerCase(Locale.ROOT);
        candidates.add(lower);
        if (candidate.startsWith("*")) {
            String noStar = candidate.substring(1).trim();
            if (!noStar.isBlank()) {
                candidates.add(noStar);
                candidates.add(noStar.toLowerCase(Locale.ROOT));
            }
        } else {
            candidates.add("*" + candidate);
            candidates.add("*" + lower);
        }
        if (!candidate.startsWith("item/")) {
            candidates.add("item/" + candidate);
            candidates.add("item/" + lower);
        }
        if (candidate.contains("-")) {
            String underscored = candidate.replace('-', '_');
            candidates.add(underscored);
            candidates.add(underscored.toLowerCase(Locale.ROOT));
        }
        if (candidate.contains("_")) {
            String dashed = candidate.replace('_', '-');
            candidates.add(dashed);
            candidates.add(dashed.toLowerCase(Locale.ROOT));
        }
    }

    private static String cleanRewardToken(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        while (!cleaned.isEmpty() && HordeService.isRewardTokenDelimiter(cleaned.charAt(0))) {
            cleaned = cleaned.substring(1).trim();
        }
        while (!cleaned.isEmpty() && HordeService.isRewardTokenDelimiter(cleaned.charAt(cleaned.length() - 1))) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static boolean isRewardTokenDelimiter(char value) {
        return Character.isWhitespace(value) || value == '[' || value == ']' || value == '(' || value == ')' || value == '{' || value == '}' || value == ',' || value == ';' || value == '"' || value == '\'' || value == '`' || value == '.';
    }

    private static String resolveRewardItemAssetId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        String normalizedCandidate = candidate.trim();
        try {
            Map<String, Item> items = Item.getAssetMap().getAssetMap();
            if (items == null || items.isEmpty()) {
                return normalizedCandidate;
            }
            String exact = HordeService.findRewardAssetKey(items, normalizedCandidate);
            if (exact != null) {
                return exact;
            }
            if (normalizedCandidate.startsWith("*")) {
                String noStar = normalizedCandidate.substring(1);
                if (!noStar.isBlank()) {
                    String resolvedNoStar = HordeService.findRewardAssetKey(items, noStar);
                    if (resolvedNoStar != null) {
                        return resolvedNoStar;
                    }
                }
            } else {
                String withStar = "*" + normalizedCandidate;
                String resolvedWithStar = HordeService.findRewardAssetKey(items, withStar);
                if (resolvedWithStar != null) {
                    return resolvedWithStar;
                }
            }
            return normalizedCandidate;
        }
        catch (Exception ex) {
            return normalizedCandidate;
        }
    }

    private static String findRewardAssetKey(Map<String, Item> items, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        Item direct = items.get(candidate);
        if (direct != null && direct != Item.UNKNOWN) {
            return candidate;
        }
        for (Map.Entry<String, Item> entry : items.entrySet()) {
            String key = entry.getKey();
            Item value = entry.getValue();
            if (key == null || value == null || value == Item.UNKNOWN || !key.equalsIgnoreCase(candidate)) continue;
            return key;
        }
        return null;
    }

    private static boolean isUsableRewardItemId(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank() || HordeService.isRandomRewardMode(itemId)) {
            return false;
        }
        String resolved = HordeService.resolveRewardItemAssetId(itemId);
        return HordeService.isUsableRewardItemIdRaw(resolved, quantity);
    }

    private static boolean isUsableRewardItemIdRaw(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        try {
            ItemStack stack = new ItemStack(itemId, Math.max(1, quantity));
            if (!stack.isValid() || stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            if (item == null || item == Item.UNKNOWN) {
                return false;
            }
            String resolvedId = item.getId();
            return resolvedId != null && !resolvedId.isBlank() && !"Unknown".equalsIgnoreCase(resolvedId);
        }
        catch (Exception ex) {
            return false;
        }
    }

    private static void addUsableRewardSuggestions(Set<String> target, List<String> candidates) {
        if (target == null || candidates == null || candidates.isEmpty()) {
            return;
        }
        for (String candidate : candidates) {
            String normalized = HordeService.normalizeRewardItemId(candidate);
            if (normalized.isBlank() || HordeService.isRandomRewardMode(normalized) || !HordeService.isUsableRewardItemIdRaw(normalized, 1)) continue;
            target.add(normalized);
        }
    }

    private static List<String> buildFallbackRewardSuggestions() {
        ArrayList<String> fallback = new ArrayList<String>();
        try {
            Map<String, Item> items = Item.getAssetMap().getAssetMap();
            if (items == null || items.isEmpty()) {
                return fallback;
            }
            for (Map.Entry<String, Item> entry : items.entrySet()) {
                String itemId = entry.getKey();
                Item asset = entry.getValue();
                if (itemId == null || itemId.isBlank() || asset == null || asset == Item.UNKNOWN) continue;
                String resolved = HordeService.resolveRewardItemAssetId(itemId);
                if (resolved.isBlank() || !HordeService.isUsableRewardItemIdRaw(resolved, 1) || !HordeService.isLikelyRewardItem(resolved)) continue;
                fallback.add(resolved);
            }
            fallback.sort(String.CASE_INSENSITIVE_ORDER);
            if (fallback.size() > MAX_REWARD_SUGGESTIONS) {
                return new ArrayList<String>(fallback.subList(0, MAX_REWARD_SUGGESTIONS));
            }
        }
        catch (Exception exception) {
            // ignore and keep fallback empty
        }
        return fallback;
    }

    private static boolean isLikelyRewardItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        if (normalized.contains("deco_") || normalized.contains("container_") || normalized.contains("scenery_")) {
            return false;
        }
        return normalized.contains("armor_") || normalized.contains("weapon_") || normalized.contains("tool_") || normalized.contains("fish_") || normalized.contains("consumable") || normalized.contains("potion") || normalized.contains("food") || normalized.contains("resource") || normalized.contains("material") || normalized.contains("ingot") || normalized.contains("gem") || normalized.contains("rune") || normalized.contains("item/weapon") || normalized.contains("item/tool") || normalized.contains("item/armor") || normalized.contains("item/consumable") || normalized.contains("item/resource") || normalized.contains("item/material");
    }

    private static boolean isRandomEnemyType(String enemyType) {
        return "random".equals(HordeService.normalizeEnemyType(enemyType));
    }

    private static int computeRemainingSeconds(long nowMillis, long targetMillis) {
        long remaining = Math.max(0L, targetMillis - nowMillis);
        return (int)((remaining + 999L) / 1000L);
    }

    private static String pickRandomEnemyType(List<String> preferredTypes) {
        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(preferredTypes.size());
            return preferredTypes.get(randomIndex);
        }
        return HordeService.pickRandomEnemyType();
    }

    private static String pickRandomEnemyType() {
        if (RANDOM_ENEMY_TYPE_OPTIONS.isEmpty()) {
            return DEFAULT_ENEMY_TYPE;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(RANDOM_ENEMY_TYPE_OPTIONS.size());
        return RANDOM_ENEMY_TYPE_OPTIONS.get(randomIndex);
    }

    private static int removeInvalidRefs(Set<Ref<EntityStore>> refs, Set<Ref<EntityStore>> accountedEnemyDeaths) {
        int removed = 0;
        HashSet<Ref<EntityStore>> stale = new HashSet<Ref<EntityStore>>();
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) continue;
            stale.add(ref);
        }
        for (Ref ref : stale) {
            refs.remove(ref);
            if (accountedEnemyDeaths != null) {
                accountedEnemyDeaths.remove(ref);
            }
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

    private static String formatRewardInfo(String rewardItemId, int rewardQuantity, boolean english) {
        String itemId = rewardItemId == null ? "" : rewardItemId.trim();
        if (itemId.isBlank()) {
            return english ? "no item" : "sin item";
        }
        if (HordeService.isRandomRewardMode(itemId)) {
            return (english ? "random" : "aleatorio") + " x" + Math.max(1, rewardQuantity);
        }
        return itemId + " x" + Math.max(1, rewardQuantity);
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
            Path saveLogs = Path.of(appData, "Hytale", "UserData", "Saves", "Mod-Test", "logs");
            if (Files.exists(saveLogs, new LinkOption[0])) {
                return saveLogs.toString();
            }
            Path legacyLogs = Path.of(appData, "Hytale", "UserData", "Logs");
            if (Files.exists(legacyLogs, new LinkOption[0])) {
                return legacyLogs.toString();
            }
            return saveLogs.toString();
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
        if (sanitized.rounds < MIN_ROUNDS) {
            sanitized.rounds = HordeConfig.defaults().rounds;
        } else if (sanitized.rounds > MAX_ROUNDS) {
            sanitized.rounds = MAX_ROUNDS;
        }
        if (sanitized.baseEnemiesPerRound < MIN_ENEMIES_PER_ROUND) {
            sanitized.baseEnemiesPerRound = HordeConfig.defaults().baseEnemiesPerRound;
        } else if (sanitized.baseEnemiesPerRound > MAX_ENEMIES_PER_ROUND) {
            sanitized.baseEnemiesPerRound = MAX_ENEMIES_PER_ROUND;
        }
        if (sanitized.enemiesPerRoundIncrement < MIN_ENEMY_INCREMENT) {
            sanitized.enemiesPerRoundIncrement = HordeConfig.defaults().enemiesPerRoundIncrement;
        } else if (sanitized.enemiesPerRoundIncrement > MAX_ENEMY_INCREMENT) {
            sanitized.enemiesPerRoundIncrement = MAX_ENEMY_INCREMENT;
        }
        if (sanitized.waveDelaySeconds < MIN_WAVE_DELAY_SECONDS) {
            sanitized.waveDelaySeconds = HordeConfig.defaults().waveDelaySeconds;
        } else if (sanitized.waveDelaySeconds > MAX_WAVE_DELAY_SECONDS) {
            sanitized.waveDelaySeconds = MAX_WAVE_DELAY_SECONDS;
        }
        if (sanitized.playerMultiplier < MIN_PLAYER_MULTIPLIER) {
            sanitized.playerMultiplier = HordeConfig.defaults().playerMultiplier;
        } else if (sanitized.playerMultiplier > MAX_PLAYER_MULTIPLIER) {
            sanitized.playerMultiplier = MAX_PLAYER_MULTIPLIER;
        }
        if (sanitized.rewardEveryRounds <= 0) {
            sanitized.rewardEveryRounds = HordeConfig.defaults().rewardEveryRounds;
        }
        if (sanitized.rewardItemQuantity < MIN_REWARD_ITEM_QUANTITY) {
            sanitized.rewardItemQuantity = HordeConfig.defaults().rewardItemQuantity;
        } else if (sanitized.rewardItemQuantity > MAX_REWARD_ITEM_QUANTITY) {
            sanitized.rewardItemQuantity = MAX_REWARD_ITEM_QUANTITY;
        }
        String safeRewardItemId = HordeService.resolveGuaranteedRewardTestItemId(sanitized.rewardItemQuantity);
        if (sanitized.rewardItemId == null || sanitized.rewardItemId.isBlank()) {
            sanitized.rewardItemId = safeRewardItemId;
        } else {
            sanitized.rewardItemId = HordeService.normalizeRewardItemId(sanitized.rewardItemId.trim());
            if (!sanitized.rewardItemId.isBlank() && !HordeService.isRandomRewardMode(sanitized.rewardItemId) && !HordeService.isUsableRewardItemId(sanitized.rewardItemId, sanitized.rewardItemQuantity)) {
                sanitized.rewardItemId = safeRewardItemId;
            }
        }
        sanitized.minSpawnRadius = HordeService.clamp(sanitized.minSpawnRadius, MIN_RADIUS, MAX_RADIUS);
        sanitized.maxSpawnRadius = HordeService.clamp(sanitized.maxSpawnRadius, MIN_RADIUS, MAX_RADIUS);
        if (sanitized.maxSpawnRadius < sanitized.minSpawnRadius) {
            double temp = sanitized.minSpawnRadius;
            sanitized.minSpawnRadius = sanitized.maxSpawnRadius;
            sanitized.maxSpawnRadius = temp;
        }
        sanitized.enemyType = HordeService.normalizeEnemyType(sanitized.enemyType);
        if (!ENEMY_TYPE_HINTS.containsKey(sanitized.enemyType)) {
            sanitized.enemyType = DEFAULT_ENEMY_TYPE;
        }
        sanitized.npcRole = HordeService.safeRoleValue(sanitized.npcRole).trim();
        if ("auto".equalsIgnoreCase(sanitized.npcRole) || "none".equalsIgnoreCase(sanitized.npcRole) || "clear".equalsIgnoreCase(sanitized.npcRole) || "default".equalsIgnoreCase(sanitized.npcRole)) {
            sanitized.npcRole = "";
        }
        sanitized.language = HordeService.normalizeLanguage(sanitized.language);
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
        public String language;
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
            defaults.baseEnemiesPerRound = 10;
            defaults.enemiesPerRoundIncrement = 3;
            defaults.waveDelaySeconds = 8;
            defaults.playerMultiplier = 1;
            defaults.enemyType = DEFAULT_ENEMY_TYPE;
            defaults.npcRole = "";
            defaults.language = LANGUAGE_SPANISH;
            defaults.rewardEveryRounds = 2;
            defaults.rewardItemId = HordeService.resolveDefaultRewardItemId();
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
            copy.language = this.language;
            copy.rewardEveryRounds = this.rewardEveryRounds;
            copy.rewardItemId = this.rewardItemId;
            copy.rewardItemQuantity = this.rewardItemQuantity;
            return copy;
        }
    }

    private static final class PlayerCombatStats {
        private String username;
        private int kills;
        private int deaths;

        private PlayerCombatStats(String username) {
            this.username = username == null || username.isBlank() ? "Jugador" : username;
            this.kills = 0;
            this.deaths = 0;
        }
    }

    private static final class HordeSession {
        private final World world;
        private final Store<EntityStore> store;
        private final String role;
        private final String forcedRole;
        private final String enemyType;
        private final List<String> availableRoles;
        private final List<String> randomEnemyTypes;
        private final int playerMultiplier;
        private final Vector3f startRotation;
        private final Set<Ref<EntityStore>> activeEnemies;
        private final Set<Ref<EntityStore>> spawnedEnemies;
        private final Set<Ref<EntityStore>> accountedEnemyDeaths;
        private final Map<UUID, PlayerCombatStats> playerStats;
        private final Map<UUID, Long> lastPlayerDeathAt;
        private int currentRound;
        private int totalSpawned;
        private int totalKilled;
        private boolean roundActive;
        private int lastStartCountdownAnnouncement;
        private int lastIntermissionCountdownAnnouncement;
        private long nextRoundAtMillis;
        private final long startedAtMillis;
        private ScheduledFuture<?> ticker;

        private HordeSession(World world, Store<EntityStore> store, String role, String enemyType, List<String> availableRoles, int playerMultiplier, String forcedRole, List<String> randomEnemyTypes, Vector3f startRotation, long firstRoundAtMillis) {
            this.world = world;
            this.store = store;
            this.role = role;
            this.forcedRole = forcedRole;
            this.enemyType = enemyType;
            this.availableRoles = availableRoles == null ? new ArrayList<String>() : new ArrayList<String>(availableRoles);
            this.randomEnemyTypes = randomEnemyTypes == null ? new ArrayList<String>() : new ArrayList<String>(randomEnemyTypes);
            this.playerMultiplier = Math.max(MIN_PLAYER_MULTIPLIER, playerMultiplier);
            this.startRotation = startRotation == null ? Vector3f.ZERO : new Vector3f(startRotation);
            this.activeEnemies = new HashSet<Ref<EntityStore>>();
            this.spawnedEnemies = new HashSet<Ref<EntityStore>>();
            this.accountedEnemyDeaths = new HashSet<Ref<EntityStore>>();
            this.playerStats = new HashMap<UUID, PlayerCombatStats>();
            this.lastPlayerDeathAt = new HashMap<UUID, Long>();
            this.currentRound = 0;
            this.totalSpawned = 0;
            this.totalKilled = 0;
            this.roundActive = false;
            this.lastStartCountdownAnnouncement = -1;
            this.lastIntermissionCountdownAnnouncement = -1;
            this.nextRoundAtMillis = Math.max(0L, firstRoundAtMillis);
            this.startedAtMillis = System.currentTimeMillis();
        }
    }

    public static final class PlayerSnapshot {
        public final UUID playerId;
        public final String username;
        public final int kills;
        public final int deaths;

        private PlayerSnapshot(UUID playerId, String username, int kills, int deaths) {
            this.playerId = playerId;
            this.username = username == null || username.isBlank() ? "Jugador" : username;
            this.kills = Math.max(0, kills);
            this.deaths = Math.max(0, deaths);
        }
    }

    public static final class StatusSnapshot {
        public final boolean active;
        public final int currentRound;
        public final int totalRounds;
        public final int aliveEnemies;
        public final int totalSpawned;
        public final int totalKilled;
        public final int totalDeaths;
        public final String role;
        public final long elapsedSeconds;
        public final long nextRoundInSeconds;
        public final String worldName;
        public final String language;
        public final List<PlayerSnapshot> players;

        private StatusSnapshot(boolean active, int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, int totalDeaths, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName, String language, List<PlayerSnapshot> players) {
            this.active = active;
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
            this.aliveEnemies = aliveEnemies;
            this.totalSpawned = totalSpawned;
            this.totalKilled = totalKilled;
            this.totalDeaths = totalDeaths;
            this.role = role == null ? "" : role;
            this.elapsedSeconds = Math.max(0L, elapsedSeconds);
            this.nextRoundInSeconds = Math.max(0L, nextRoundInSeconds);
            this.worldName = worldName == null ? "default" : worldName;
            this.language = HordeService.normalizeLanguage(language);
            this.players = players == null ? List.of() : List.copyOf(players);
        }

        public PlayerSnapshot findPlayer(UUID playerId) {
            if (playerId == null || this.players.isEmpty()) {
                return null;
            }
            for (PlayerSnapshot row : this.players) {
                if (row.playerId == null || !row.playerId.equals((Object)playerId)) continue;
                return row;
            }
            return null;
        }

        public static StatusSnapshot inactive(int totalRounds, String worldName, String enemyType, String language) {
            return new StatusSnapshot(false, 0, totalRounds, 0, 0, 0, 0, enemyType, 0L, 0L, worldName, language, List.of());
        }

        public static StatusSnapshot active(int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, int totalDeaths, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName, String language, List<PlayerSnapshot> players) {
            return new StatusSnapshot(true, currentRound, totalRounds, aliveEnemies, totalSpawned, totalKilled, totalDeaths, role, elapsedSeconds, nextRoundInSeconds, worldName, language, players);
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



