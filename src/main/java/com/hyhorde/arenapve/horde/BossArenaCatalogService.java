package com.hyhorde.arenapve.horde;

import com.google.gson.Gson;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

final class BossArenaCatalogService {
    private static final int MIN_NOTIFICATION_RADIUS = 10;
    private static final int MAX_NOTIFICATION_RADIUS = 500;
    private static final int DEFAULT_NOTIFICATION_RADIUS = 100;
    private static final String DEFAULT_ARENA_ICON_ITEM_ID = "Ingredient_Bar_Gold";
    private static final String DEFAULT_BOSS_ICON_ITEM_ID = "Ingredient_Bar_Gold";
    private static final String DEFAULT_BOSS_NPC_ID = "Goblin_Ogre";
    private static final int MAX_LEVEL_OVERRIDE = 300;
    private static final int MAX_BOSS_XP_POINTS = 1000000;
    private static final Set<String> BOSS_SPAWN_TRIGGERS = Set.of("before_boss", "on_spawn", "after_spawn_seconds", "since_last_wave", "boss_hp_percent");
    private static final List<String> BOSS_TIER_OPTIONS = List.of("common", "uncommon", "rare", "epic", "legendary");
    private final PluginBase plugin;
    private final Gson gson;
    private final Path bossesPath;
    private final Path arenasPath;
    private final LinkedHashMap<String, BossDefinition> bossesById;
    private final LinkedHashMap<String, ArenaDefinition> arenasById;

    BossArenaCatalogService(PluginBase plugin, Gson gson, Path pluginDataPath) {
        this.plugin = plugin;
        this.gson = gson;
        this.bossesPath = pluginDataPath.resolve("bosses.json");
        this.arenasPath = pluginDataPath.resolve("arenas.json");
        this.bossesById = new LinkedHashMap<String, BossDefinition>();
        this.arenasById = new LinkedHashMap<String, ArenaDefinition>();
        this.reloadFromDisk();
    }

    synchronized CatalogReloadSummary reloadFromDisk() {
        boolean bossesTemplate = this.loadBossesFromDisk();
        boolean arenasTemplate = this.loadArenasFromDisk();
        return new CatalogReloadSummary(this.bossesById.size(), this.arenasById.size(), bossesTemplate, arenasTemplate);
    }

    synchronized List<String> getBossTierOptions() {
        return new ArrayList<String>(BOSS_TIER_OPTIONS);
    }

    synchronized List<BossDefinitionSnapshot> getBossDefinitionsSnapshot() {
        ArrayList<BossDefinitionSnapshot> rows = new ArrayList<BossDefinitionSnapshot>();
        for (BossDefinition row : this.bossesById.values()) {
            if (row == null || BossArenaCatalogService.isBlank(row.bossId)) {
                continue;
            }
            rows.add(BossDefinitionSnapshot.from(row));
        }
        return rows;
    }

    synchronized List<ArenaDefinitionSnapshot> getArenaDefinitionsSnapshot() {
        ArrayList<ArenaDefinitionSnapshot> rows = new ArrayList<ArenaDefinitionSnapshot>();
        for (ArenaDefinition row : this.arenasById.values()) {
            if (row == null || BossArenaCatalogService.isBlank(row.arenaId)) {
                continue;
            }
            rows.add(ArenaDefinitionSnapshot.from(row));
        }
        return rows;
    }

    synchronized HordeService.OperationResult createBossDraft(String requestedBossId, boolean english) {
        String base = BossArenaCatalogService.clean(requestedBossId);
        if (base.isBlank()) {
            base = this.nextBossId();
        }
        String uniqueId = this.uniqueBossId(base);
        this.bossesById.put(BossArenaCatalogService.key(uniqueId), BossDefinition.defaults(uniqueId));
        this.saveBosses();
        return HordeService.OperationResult.ok(english ? "Boss created: " + uniqueId + "." : "Boss creado: " + uniqueId + ".");
    }

    synchronized HordeService.OperationResult deleteBoss(String bossId, boolean english) {
        String cleanBossId = BossArenaCatalogService.clean(bossId);
        if (cleanBossId.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Select a boss to delete." : "Selecciona un boss para eliminar.");
        }
        BossDefinition removed = this.bossesById.remove(BossArenaCatalogService.key(cleanBossId));
        if (removed == null) {
            return HordeService.OperationResult.fail(english ? "Boss not found." : "Boss no encontrado.");
        }
        if (this.bossesById.isEmpty()) {
            this.addDefaultBossDefinitions();
        }
        this.saveBosses();
        return HordeService.OperationResult.ok(english ? "Deleted boss: " + removed.bossId + "." : "Boss eliminado: " + removed.bossId + ".");
    }

    synchronized HordeService.OperationResult upsertBossFromValues(Map<String, String> values, boolean english) {
        String selected = BossArenaCatalogService.clean(values.get("bossSelected"));
        String requested = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("bossEditName"), selected));
        if (requested.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Boss ID is required." : "El ID del boss es obligatorio.");
        }
        String selectedKey = BossArenaCatalogService.key(selected);
        String requestedKey = BossArenaCatalogService.key(requested);
        BossDefinition existing = selectedKey.isBlank() ? null : this.bossesById.get(selectedKey);
        boolean creating = existing == null;
        if (!selectedKey.equals(requestedKey) && this.bossesById.containsKey(requestedKey)) {
            return HordeService.OperationResult.fail(english ? "A boss with that ID already exists." : "Ya existe un boss con ese ID.");
        }
        BossDefinition target = creating ? BossDefinition.defaults(requested) : existing.copy();
        target.bossId = requested;
        target.npcId = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("bossEditNpcId"), target.npcId));
        target.tier = "common";
        target.iconItemId = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("bossEditIconItemId"), target.iconItemId, DEFAULT_BOSS_ICON_ITEM_ID));
        if (target.iconItemId.isBlank()) {
            target.iconItemId = DEFAULT_BOSS_ICON_ITEM_ID;
        }
        if (target.npcId.isBlank()) {
            return HordeService.OperationResult.fail(english ? "NPC ID is required." : "El NPC ID es obligatorio.");
        }
        try {
            target.amount = BossArenaCatalogService.clamp(BossArenaCatalogService.parseInt(values.get("bossEditAmount"), target.amount), 1, 250);
            target.levelOverride = BossArenaCatalogService.clamp(BossArenaCatalogService.parseInt(values.get("bossEditLevelOverride"), target.levelOverride), 0, MAX_LEVEL_OVERRIDE);
            target.experiencePoints = BossArenaCatalogService.clamp(BossArenaCatalogService.parseInt(values.get("bossEditXpPoints"), target.experiencePoints), 0, MAX_BOSS_XP_POINTS);
            target.lootRadius = BossArenaCatalogService.clamp(BossArenaCatalogService.parseDouble(values.get("bossEditLootRadius"), target.lootRadius), 0.0, 4096.0);
            target.modifiers.apply(values, "bossEdit", true);
            target.perPlayerIncrease.apply(values, "bossEditPp", false);
            target.bossSpawnTrigger = BossArenaCatalogService.normalizeSpawnTrigger(BossArenaCatalogService.firstNonBlank(values.get("bossSpawnTrigger"), target.bossSpawnTrigger));
            target.bossSpawnTriggerValue = BossArenaCatalogService.parseDouble(values.get("bossSpawnTriggerValue"), target.bossSpawnTriggerValue);
            target.useRandomSpawnLocations = BossArenaCatalogService.parseBoolean(values.get("bossWaveRandomLocations"), target.useRandomSpawnLocations);
            target.randomSpawnRadius = BossArenaCatalogService.clamp(BossArenaCatalogService.parseDouble(values.get("bossWaveRandomRadius"), target.randomSpawnRadius), 0.0, 4096.0);
            target.timedProximityEnabled = BossArenaCatalogService.parseBoolean(values.get("bossTimedProximityEnabled"), target.timedProximityEnabled);
            target.timedProximityArenaId = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("bossTimedProximityArena"), target.timedProximityArenaId));
            target.timedProximityRadius = BossArenaCatalogService.clamp(BossArenaCatalogService.parseDouble(values.get("bossTimedProximityRadius"), target.timedProximityRadius), 0.0, 4096.0);
            target.timedProximityCooldownSeconds = BossArenaCatalogService.clamp(BossArenaCatalogService.parseInt(values.get("bossTimedProximityCooldown"), target.timedProximityCooldownSeconds), 0, 86400);
        }
        catch (IllegalArgumentException ex) {
            return HordeService.OperationResult.fail(ex.getMessage());
        }
        if (!selectedKey.isBlank() && !selectedKey.equals(requestedKey)) {
            this.bossesById.remove(selectedKey);
        }
        this.bossesById.put(requestedKey, BossDefinition.sanitize(target));
        this.saveBosses();
        return HordeService.OperationResult.ok(english ? (creating ? "Boss saved: " : "Boss updated: ") + target.bossId + "." : (creating ? "Boss guardado: " : "Boss actualizado: ") + target.bossId + ".");
    }

    synchronized HordeService.OperationResult createArenaFromPlayer(PlayerRef playerRef, World world, String requestedArenaId, boolean english) {
        if (playerRef == null || world == null) {
            return HordeService.OperationResult.fail(english ? "Could not resolve player/world for arena creation." : "No se pudo resolver jugador/mundo para crear la arena.");
        }
        String base = BossArenaCatalogService.clean(requestedArenaId);
        if (base.isBlank()) {
            base = this.nextArenaId();
        }
        String arenaId = this.uniqueArenaId(base);
        Transform transform = playerRef.getTransform();
        Vector3d position = transform == null ? null : transform.getPosition();
        if (position == null) {
            return HordeService.OperationResult.fail(english ? "Could not resolve player position." : "No se pudo resolver la posicion del jugador.");
        }
        ArenaDefinition row = ArenaDefinition.defaults(arenaId, world.getName());
        row.x = position.x;
        row.y = position.y;
        row.z = position.z;
        this.arenasById.put(BossArenaCatalogService.key(arenaId), row);
        this.saveArenas();
        return HordeService.OperationResult.ok(String.format(Locale.ROOT, english ? "Arena created: %s (%.2f, %.2f, %.2f)." : "Arena creada: %s (%.2f, %.2f, %.2f).", row.arenaId, row.x, row.y, row.z));
    }

    synchronized HordeService.OperationResult deleteArena(String arenaId, boolean english) {
        String cleanArenaId = BossArenaCatalogService.clean(arenaId);
        if (cleanArenaId.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Select an arena to delete." : "Selecciona una arena para eliminar.");
        }
        ArenaDefinition removed = this.arenasById.remove(BossArenaCatalogService.key(cleanArenaId));
        if (removed == null) {
            return HordeService.OperationResult.fail(english ? "Arena not found." : "Arena no encontrada.");
        }
        this.saveArenas();
        return HordeService.OperationResult.ok(english ? "Deleted arena: " + removed.arenaId + "." : "Arena eliminada: " + removed.arenaId + ".");
    }

    synchronized HordeService.OperationResult upsertArenaFromValues(Map<String, String> values, String fallbackWorldName, boolean english) {
        String selected = BossArenaCatalogService.clean(values.get("arenaSelected"));
        String requested = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("arenaEditId"), selected));
        if (requested.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Arena ID is required." : "El ID de arena es obligatorio.");
        }
        String selectedKey = BossArenaCatalogService.key(selected);
        String requestedKey = BossArenaCatalogService.key(requested);
        ArenaDefinition existing = selectedKey.isBlank() ? null : this.arenasById.get(selectedKey);
        boolean creating = existing == null;
        if (!selectedKey.equals(requestedKey) && this.arenasById.containsKey(requestedKey)) {
            return HordeService.OperationResult.fail(english ? "An arena with that ID already exists." : "Ya existe una arena con ese ID.");
        }
        ArenaDefinition target = creating ? ArenaDefinition.defaults(requested, fallbackWorldName) : existing.copy();
        target.arenaId = requested;
        target.worldName = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("arenaEditWorld"), target.worldName, fallbackWorldName));
        if (target.worldName.isBlank()) {
            target.worldName = "default";
        }
        target.iconItemId = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(values.get("arenaEditIconItemId"), target.iconItemId, DEFAULT_ARENA_ICON_ITEM_ID));
        if (target.iconItemId.isBlank()) {
            target.iconItemId = DEFAULT_ARENA_ICON_ITEM_ID;
        }
        try {
            target.x = BossArenaCatalogService.parseDouble(values.get("arenaEditX"), target.x);
            target.y = BossArenaCatalogService.parseDouble(values.get("arenaEditY"), target.y);
            target.z = BossArenaCatalogService.parseDouble(values.get("arenaEditZ"), target.z);
            target.notificationRadius = BossArenaCatalogService.clamp(BossArenaCatalogService.parseInt(values.get("arenaEditNotificationRadius"), target.notificationRadius), MIN_NOTIFICATION_RADIUS, MAX_NOTIFICATION_RADIUS);
        }
        catch (IllegalArgumentException ex) {
            return HordeService.OperationResult.fail(ex.getMessage());
        }
        if (!selectedKey.isBlank() && !selectedKey.equals(requestedKey)) {
            this.arenasById.remove(selectedKey);
        }
        this.arenasById.put(requestedKey, ArenaDefinition.sanitize(target));
        this.saveArenas();
        return HordeService.OperationResult.ok(english ? (creating ? "Arena saved: " : "Arena updated: ") + target.arenaId + "." : (creating ? "Arena guardada: " : "Arena actualizada: ") + target.arenaId + ".");
    }

    private boolean loadBossesFromDisk() {
        try {
            Files.createDirectories(this.bossesPath.getParent(), new FileAttribute[0]);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("Could not prepare bosses.json directory: %s", (Object)ex.getMessage());
        }
        boolean templateCreated = false;
        if (!Files.exists(this.bossesPath, new LinkOption[0])) {
            this.saveBosses();
            templateCreated = true;
        }
        this.bossesById.clear();
        try (BufferedReader reader = Files.newBufferedReader(this.bossesPath, StandardCharsets.UTF_8);) {
            BossCatalogFile raw = (BossCatalogFile)this.gson.fromJson((Reader)reader, BossCatalogFile.class);
            if (raw != null && raw.bosses != null) {
                for (BossDefinition row : raw.bosses) {
                    BossDefinition clean = BossDefinition.sanitize(row);
                    if (clean == null || BossArenaCatalogService.isBlank(clean.bossId)) {
                        continue;
                    }
                    this.bossesById.put(BossArenaCatalogService.key(clean.bossId), clean);
                }
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("Could not load bosses.json: %s", (Object)ex.getMessage());
        }
        if (this.bossesById.isEmpty()) {
            this.addDefaultBossDefinitions();
            this.saveBosses();
        }
        return templateCreated;
    }

    private void addDefaultBossDefinitions() {
        this.upsertDefaultBoss("trex", "Cave_Rex");
        this.upsertDefaultBoss("golem", "Golem_Crystal_Flame");
        this.upsertDefaultBoss("goblin_ogre", "Goblin_Ogre");
        this.upsertDefaultBoss("aberrant_zombie", "Aberrant_Zombie");
        this.upsertDefaultBoss("scarak_broodmother", "Scarak_Broodmother");
    }

    private void upsertDefaultBoss(String bossId, String npcId) {
        String safeBossId = this.uniqueBossId(BossArenaCatalogService.clean(bossId));
        BossDefinition row = BossDefinition.defaults(safeBossId);
        row.npcId = BossArenaCatalogService.clean(BossArenaCatalogService.firstNonBlank(npcId, DEFAULT_BOSS_NPC_ID));
        row.levelOverride = 100;
        row.experiencePoints = 1000;
        row.modifiers.hp = 10.0;
        row.modifiers.damage = 3.0;
        row.tier = "common";
        this.bossesById.put(BossArenaCatalogService.key(safeBossId), BossDefinition.sanitize(row));
    }

    private boolean loadArenasFromDisk() {
        try {
            Files.createDirectories(this.arenasPath.getParent(), new FileAttribute[0]);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("Could not prepare arenas.json directory: %s", (Object)ex.getMessage());
        }
        boolean templateCreated = false;
        if (!Files.exists(this.arenasPath, new LinkOption[0])) {
            this.saveArenas();
            templateCreated = true;
        }
        this.arenasById.clear();
        try (BufferedReader reader = Files.newBufferedReader(this.arenasPath, StandardCharsets.UTF_8);) {
            ArenaCatalogFile raw = (ArenaCatalogFile)this.gson.fromJson((Reader)reader, ArenaCatalogFile.class);
            if (raw != null && raw.arenas != null) {
                for (ArenaDefinition row : raw.arenas) {
                    ArenaDefinition clean = ArenaDefinition.sanitize(row);
                    if (clean == null || BossArenaCatalogService.isBlank(clean.arenaId)) {
                        continue;
                    }
                    this.arenasById.put(BossArenaCatalogService.key(clean.arenaId), clean);
                }
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("Could not load arenas.json: %s", (Object)ex.getMessage());
        }
        return templateCreated;
    }

    private void saveBosses() {
        BossCatalogFile data = new BossCatalogFile();
        data.version = 1;
        data.bosses = new ArrayList<BossDefinition>();
        for (BossDefinition row : this.bossesById.values()) {
            if (row == null || BossArenaCatalogService.isBlank(row.bossId)) {
                continue;
            }
            data.bosses.add(BossDefinition.sanitize(row));
        }
        this.writeJson(this.bossesPath, data, "bosses.json");
    }

    private void saveArenas() {
        ArenaCatalogFile data = new ArenaCatalogFile();
        data.version = 1;
        data.arenas = new ArrayList<ArenaDefinition>();
        for (ArenaDefinition row : this.arenasById.values()) {
            if (row == null || BossArenaCatalogService.isBlank(row.arenaId)) {
                continue;
            }
            data.arenas.add(ArenaDefinition.sanitize(row));
        }
        this.writeJson(this.arenasPath, data, "arenas.json");
    }

    private void writeJson(Path path, Object data, String label) {
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, new OpenOption[0]);) {
                this.gson.toJson(data, writer);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("Could not save %s: %s", (Object)label, (Object)ex.getMessage());
        }
    }

    private String nextBossId() {
        int counter = 1;
        while (this.bossesById.containsKey(BossArenaCatalogService.key("boss_" + counter))) {
            ++counter;
        }
        return "boss_" + counter;
    }

    private String uniqueBossId(String requested) {
        String base = BossArenaCatalogService.clean(requested);
        if (base.isBlank()) {
            base = "boss";
        }
        String candidate = base;
        int counter = 1;
        while (this.bossesById.containsKey(BossArenaCatalogService.key(candidate))) {
            ++counter;
            candidate = base + "_" + counter;
        }
        return candidate;
    }

    private String nextArenaId() {
        int counter = 1;
        while (this.arenasById.containsKey(BossArenaCatalogService.key("arena_" + counter))) {
            ++counter;
        }
        return "arena_" + counter;
    }

    private String uniqueArenaId(String requested) {
        String base = BossArenaCatalogService.clean(requested);
        if (base.isBlank()) {
            base = "arena";
        }
        String candidate = base;
        int counter = 1;
        while (this.arenasById.containsKey(BossArenaCatalogService.key(candidate))) {
            ++counter;
            candidate = base + "_" + counter;
        }
        return candidate;
    }

    private static String firstNonBlank(String ... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            return value;
        }
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String key(String value) {
        return BossArenaCatalogService.clean(value).toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int parseInt(String raw, int fallback) {
        if (BossArenaCatalogService.isBlank(raw)) {
            return fallback;
        }
        try {
            return (int)Math.round(Double.parseDouble(raw.trim().replace(',', '.')));
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Invalid integer number: " + raw);
        }
    }

    private static double parseDouble(String raw, double fallback) {
        if (BossArenaCatalogService.isBlank(raw)) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Invalid decimal number: " + raw);
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (BossArenaCatalogService.isBlank(raw)) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized) || "si".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static String normalizeTier(String rawTier) {
        String normalized = BossArenaCatalogService.clean(rawTier).toLowerCase(Locale.ROOT);
        return BOSS_TIER_OPTIONS.contains(normalized) ? normalized : "common";
    }

    private static String normalizeSpawnTrigger(String rawTrigger) {
        String normalized = BossArenaCatalogService.clean(rawTrigger).toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return BOSS_SPAWN_TRIGGERS.contains(normalized) ? normalized : "before_boss";
    }

    static final class CatalogReloadSummary {
        final int bosses;
        final int arenas;
        final boolean bossesTemplateCreated;
        final boolean arenasTemplateCreated;

        CatalogReloadSummary(int bosses, int arenas, boolean bossesTemplateCreated, boolean arenasTemplateCreated) {
            this.bosses = bosses;
            this.arenas = arenas;
            this.bossesTemplateCreated = bossesTemplateCreated;
            this.arenasTemplateCreated = arenasTemplateCreated;
        }
    }

    static final class BossDefinitionSnapshot {
        final String bossId;
        final String npcId;
        final String tier;
        final String iconItemId;
        final int amount;
        final int levelOverride;
        final int experiencePoints;
        final double lootRadius;
        final ScalersSnapshot modifiers;
        final ScalersSnapshot perPlayerIncrease;
        final String bossSpawnTrigger;
        final double bossSpawnTriggerValue;
        final boolean useRandomSpawnLocations;
        final double randomSpawnRadius;
        final boolean timedProximityEnabled;
        final String timedProximityArenaId;
        final double timedProximityRadius;
        final int timedProximityCooldownSeconds;

        private BossDefinitionSnapshot(String bossId, String npcId, String tier, String iconItemId, int amount, int levelOverride, int experiencePoints, double lootRadius, ScalersSnapshot modifiers, ScalersSnapshot perPlayerIncrease, String bossSpawnTrigger, double bossSpawnTriggerValue, boolean useRandomSpawnLocations, double randomSpawnRadius, boolean timedProximityEnabled, String timedProximityArenaId, double timedProximityRadius, int timedProximityCooldownSeconds) {
            this.bossId = bossId;
            this.npcId = npcId;
            this.tier = tier;
            this.iconItemId = iconItemId;
            this.amount = amount;
            this.levelOverride = levelOverride;
            this.experiencePoints = experiencePoints;
            this.lootRadius = lootRadius;
            this.modifiers = modifiers;
            this.perPlayerIncrease = perPlayerIncrease;
            this.bossSpawnTrigger = bossSpawnTrigger;
            this.bossSpawnTriggerValue = bossSpawnTriggerValue;
            this.useRandomSpawnLocations = useRandomSpawnLocations;
            this.randomSpawnRadius = randomSpawnRadius;
            this.timedProximityEnabled = timedProximityEnabled;
            this.timedProximityArenaId = timedProximityArenaId;
            this.timedProximityRadius = timedProximityRadius;
            this.timedProximityCooldownSeconds = timedProximityCooldownSeconds;
        }

        private static BossDefinitionSnapshot from(BossDefinition source) {
            BossDefinition clean = BossDefinition.sanitize(source);
            return new BossDefinitionSnapshot(clean.bossId, clean.npcId, clean.tier, clean.iconItemId, clean.amount, clean.levelOverride, clean.experiencePoints, clean.lootRadius, ScalersSnapshot.from(clean.modifiers), ScalersSnapshot.from(clean.perPlayerIncrease), clean.bossSpawnTrigger, clean.bossSpawnTriggerValue, clean.useRandomSpawnLocations, clean.randomSpawnRadius, clean.timedProximityEnabled, clean.timedProximityArenaId, clean.timedProximityRadius, clean.timedProximityCooldownSeconds);
        }
    }

    static final class ArenaDefinitionSnapshot {
        final String arenaId;
        final String worldName;
        final String iconItemId;
        final double x;
        final double y;
        final double z;
        final int notificationRadius;

        private ArenaDefinitionSnapshot(String arenaId, String worldName, String iconItemId, double x, double y, double z, int notificationRadius) {
            this.arenaId = arenaId;
            this.worldName = worldName;
            this.iconItemId = iconItemId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.notificationRadius = notificationRadius;
        }

        private static ArenaDefinitionSnapshot from(ArenaDefinition source) {
            ArenaDefinition clean = ArenaDefinition.sanitize(source);
            return new ArenaDefinitionSnapshot(clean.arenaId, clean.worldName, clean.iconItemId, clean.x, clean.y, clean.z, clean.notificationRadius);
        }
    }

    static final class ScalersSnapshot {
        final double hp;
        final double damage;
        final double movementSpeed;
        final double size;
        final double attackRate;
        final double turnRate;
        final double regen;
        final double abilityCooldown;
        final double knockbackGiven;
        final double knockbackTaken;

        private ScalersSnapshot(double hp, double damage, double movementSpeed, double size, double attackRate, double turnRate, double regen, double abilityCooldown, double knockbackGiven, double knockbackTaken) {
            this.hp = hp;
            this.damage = damage;
            this.movementSpeed = movementSpeed;
            this.size = size;
            this.attackRate = attackRate;
            this.turnRate = turnRate;
            this.regen = regen;
            this.abilityCooldown = abilityCooldown;
            this.knockbackGiven = knockbackGiven;
            this.knockbackTaken = knockbackTaken;
        }

        private static ScalersSnapshot from(Scalers source) {
            Scalers clean = Scalers.sanitize(source, true);
            return new ScalersSnapshot(clean.hp, clean.damage, clean.movementSpeed, clean.size, clean.attackRate, clean.turnRate, clean.regen, clean.abilityCooldown, clean.knockbackGiven, clean.knockbackTaken);
        }
    }

    private static final class BossCatalogFile {
        private Integer version;
        private List<BossDefinition> bosses;
    }

    private static final class ArenaCatalogFile {
        private Integer version;
        private List<ArenaDefinition> arenas;
    }

    private static final class BossDefinition {
        private String bossId;
        private String npcId;
        private String tier;
        private String iconItemId;
        private int amount;
        private int levelOverride;
        private int experiencePoints;
        private double lootRadius;
        private Scalers modifiers;
        private Scalers perPlayerIncrease;
        private String bossSpawnTrigger;
        private double bossSpawnTriggerValue;
        private boolean useRandomSpawnLocations;
        private double randomSpawnRadius;
        private boolean timedProximityEnabled;
        private String timedProximityArenaId;
        private double timedProximityRadius;
        private int timedProximityCooldownSeconds;

        private static BossDefinition defaults(String bossId) {
            BossDefinition row = new BossDefinition();
            row.bossId = BossArenaCatalogService.clean(bossId);
            row.npcId = DEFAULT_BOSS_NPC_ID;
            row.tier = "common";
            row.iconItemId = DEFAULT_BOSS_ICON_ITEM_ID;
            row.amount = 1;
            row.levelOverride = 0;
            row.experiencePoints = 0;
            row.lootRadius = 24.0;
            row.modifiers = Scalers.defaults(true);
            row.perPlayerIncrease = Scalers.defaults(false);
            row.bossSpawnTrigger = "before_boss";
            row.bossSpawnTriggerValue = 0.0;
            row.useRandomSpawnLocations = false;
            row.randomSpawnRadius = 0.0;
            row.timedProximityEnabled = false;
            row.timedProximityArenaId = "";
            row.timedProximityRadius = 0.0;
            row.timedProximityCooldownSeconds = 0;
            return row;
        }

        private BossDefinition copy() {
            BossDefinition copy = new BossDefinition();
            copy.bossId = this.bossId;
            copy.npcId = this.npcId;
            copy.tier = this.tier;
            copy.iconItemId = this.iconItemId;
            copy.amount = this.amount;
            copy.levelOverride = this.levelOverride;
            copy.experiencePoints = this.experiencePoints;
            copy.lootRadius = this.lootRadius;
            copy.modifiers = this.modifiers == null ? Scalers.defaults(true) : this.modifiers.copy();
            copy.perPlayerIncrease = this.perPlayerIncrease == null ? Scalers.defaults(false) : this.perPlayerIncrease.copy();
            copy.bossSpawnTrigger = this.bossSpawnTrigger;
            copy.bossSpawnTriggerValue = this.bossSpawnTriggerValue;
            copy.useRandomSpawnLocations = this.useRandomSpawnLocations;
            copy.randomSpawnRadius = this.randomSpawnRadius;
            copy.timedProximityEnabled = this.timedProximityEnabled;
            copy.timedProximityArenaId = this.timedProximityArenaId;
            copy.timedProximityRadius = this.timedProximityRadius;
            copy.timedProximityCooldownSeconds = this.timedProximityCooldownSeconds;
            return copy;
        }

        private static BossDefinition sanitize(BossDefinition source) {
            if (source == null) {
                return null;
            }
            BossDefinition clean = source.copy();
            clean.bossId = BossArenaCatalogService.clean(clean.bossId);
            clean.npcId = BossArenaCatalogService.clean(clean.npcId);
            clean.tier = "common";
            clean.iconItemId = BossArenaCatalogService.clean(clean.iconItemId);
            if (clean.iconItemId.isBlank()) {
                clean.iconItemId = DEFAULT_BOSS_ICON_ITEM_ID;
            }
            clean.amount = BossArenaCatalogService.clamp(clean.amount, 1, 250);
            clean.levelOverride = BossArenaCatalogService.clamp(clean.levelOverride, 0, MAX_LEVEL_OVERRIDE);
            clean.experiencePoints = BossArenaCatalogService.clamp(clean.experiencePoints, 0, MAX_BOSS_XP_POINTS);
            clean.lootRadius = BossArenaCatalogService.clamp(clean.lootRadius, 0.0, 4096.0);
            clean.modifiers = Scalers.sanitize(clean.modifiers, true);
            clean.perPlayerIncrease = Scalers.sanitize(clean.perPlayerIncrease, false);
            clean.bossSpawnTrigger = BossArenaCatalogService.normalizeSpawnTrigger(clean.bossSpawnTrigger);
            if ("boss_hp_percent".equals(clean.bossSpawnTrigger)) {
                clean.bossSpawnTriggerValue = BossArenaCatalogService.clamp(clean.bossSpawnTriggerValue, 0.0, 100.0);
            } else {
                clean.bossSpawnTriggerValue = BossArenaCatalogService.clamp(clean.bossSpawnTriggerValue, 0.0, 86400.0);
            }
            clean.randomSpawnRadius = BossArenaCatalogService.clamp(clean.randomSpawnRadius, 0.0, 4096.0);
            clean.timedProximityArenaId = BossArenaCatalogService.clean(clean.timedProximityArenaId);
            clean.timedProximityRadius = BossArenaCatalogService.clamp(clean.timedProximityRadius, 0.0, 4096.0);
            clean.timedProximityCooldownSeconds = BossArenaCatalogService.clamp(clean.timedProximityCooldownSeconds, 0, 86400);
            return clean;
        }
    }

    private static final class Scalers {
        private double hp;
        private double damage;
        private double movementSpeed;
        private double size;
        private double attackRate;
        private double turnRate;
        private double regen;
        private double abilityCooldown;
        private double knockbackGiven;
        private double knockbackTaken;

        private static Scalers defaults(boolean baseValues) {
            Scalers row = new Scalers();
            double value = baseValues ? 1.0 : 0.0;
            row.hp = value;
            row.damage = value;
            row.movementSpeed = value;
            row.size = value;
            row.attackRate = value;
            row.turnRate = value;
            row.regen = value;
            row.abilityCooldown = value;
            row.knockbackGiven = value;
            row.knockbackTaken = value;
            return row;
        }

        private Scalers copy() {
            Scalers copy = new Scalers();
            copy.hp = this.hp;
            copy.damage = this.damage;
            copy.movementSpeed = this.movementSpeed;
            copy.size = this.size;
            copy.attackRate = this.attackRate;
            copy.turnRate = this.turnRate;
            copy.regen = this.regen;
            copy.abilityCooldown = this.abilityCooldown;
            copy.knockbackGiven = this.knockbackGiven;
            copy.knockbackTaken = this.knockbackTaken;
            return copy;
        }

        private static Scalers sanitize(Scalers source, boolean baseValues) {
            Scalers clean = source == null ? Scalers.defaults(baseValues) : source.copy();
            double min = baseValues ? 0.01 : 0.0;
            clean.hp = BossArenaCatalogService.clamp(clean.hp, min, 1000.0);
            clean.damage = BossArenaCatalogService.clamp(clean.damage, min, 1000.0);
            clean.movementSpeed = BossArenaCatalogService.clamp(clean.movementSpeed, min, 100.0);
            clean.size = BossArenaCatalogService.clamp(clean.size, min, 100.0);
            clean.attackRate = BossArenaCatalogService.clamp(clean.attackRate, min, 100.0);
            clean.turnRate = BossArenaCatalogService.clamp(clean.turnRate, min, 100.0);
            clean.regen = BossArenaCatalogService.clamp(clean.regen, min, 100.0);
            clean.abilityCooldown = BossArenaCatalogService.clamp(clean.abilityCooldown, min, 100.0);
            clean.knockbackGiven = BossArenaCatalogService.clamp(clean.knockbackGiven, min, 100.0);
            clean.knockbackTaken = BossArenaCatalogService.clamp(clean.knockbackTaken, min, 100.0);
            return clean;
        }

        private void apply(Map<String, String> values, String prefix, boolean baseValues) {
            this.hp = BossArenaCatalogService.parseDouble(values.get(prefix + "Hp"), this.hp);
            this.damage = BossArenaCatalogService.parseDouble(values.get(prefix + "Damage"), this.damage);
            this.movementSpeed = BossArenaCatalogService.parseDouble(values.get(prefix + "Speed"), this.movementSpeed);
            this.size = BossArenaCatalogService.parseDouble(values.get(prefix + "Size"), this.size);
            this.attackRate = BossArenaCatalogService.parseDouble(values.get(prefix + "AttackRate"), this.attackRate);
            this.turnRate = BossArenaCatalogService.parseDouble(values.get(prefix + "TurnRate"), this.turnRate);
            this.regen = BossArenaCatalogService.parseDouble(values.get(prefix + "Regen"), this.regen);
            this.abilityCooldown = BossArenaCatalogService.parseDouble(values.get(prefix + "AbilityCooldown"), this.abilityCooldown);
            this.knockbackGiven = BossArenaCatalogService.parseDouble(values.get(prefix + "KnockbackGiven"), this.knockbackGiven);
            this.knockbackTaken = BossArenaCatalogService.parseDouble(values.get(prefix + "KnockbackTaken"), this.knockbackTaken);
            Scalers sanitized = Scalers.sanitize(this, baseValues);
            this.hp = sanitized.hp;
            this.damage = sanitized.damage;
            this.movementSpeed = sanitized.movementSpeed;
            this.size = sanitized.size;
            this.attackRate = sanitized.attackRate;
            this.turnRate = sanitized.turnRate;
            this.regen = sanitized.regen;
            this.abilityCooldown = sanitized.abilityCooldown;
            this.knockbackGiven = sanitized.knockbackGiven;
            this.knockbackTaken = sanitized.knockbackTaken;
        }
    }

    private static final class ArenaDefinition {
        private String arenaId;
        private String worldName;
        private String iconItemId;
        private double x;
        private double y;
        private double z;
        private int notificationRadius;

        private static ArenaDefinition defaults(String arenaId, String worldName) {
            ArenaDefinition row = new ArenaDefinition();
            row.arenaId = BossArenaCatalogService.clean(arenaId);
            row.worldName = BossArenaCatalogService.clean(worldName);
            if (row.worldName.isBlank()) {
                row.worldName = "default";
            }
            row.iconItemId = DEFAULT_ARENA_ICON_ITEM_ID;
            row.x = 0.0;
            row.y = 64.0;
            row.z = 0.0;
            row.notificationRadius = DEFAULT_NOTIFICATION_RADIUS;
            return row;
        }

        private ArenaDefinition copy() {
            ArenaDefinition copy = new ArenaDefinition();
            copy.arenaId = this.arenaId;
            copy.worldName = this.worldName;
            copy.iconItemId = this.iconItemId;
            copy.x = this.x;
            copy.y = this.y;
            copy.z = this.z;
            copy.notificationRadius = this.notificationRadius;
            return copy;
        }

        private static ArenaDefinition sanitize(ArenaDefinition source) {
            if (source == null) {
                return null;
            }
            ArenaDefinition clean = source.copy();
            clean.arenaId = BossArenaCatalogService.clean(clean.arenaId);
            clean.worldName = BossArenaCatalogService.clean(clean.worldName);
            if (clean.worldName.isBlank()) {
                clean.worldName = "default";
            }
            clean.iconItemId = BossArenaCatalogService.clean(clean.iconItemId);
            if (clean.iconItemId.isBlank()) {
                clean.iconItemId = DEFAULT_ARENA_ICON_ITEM_ID;
            }
            clean.notificationRadius = BossArenaCatalogService.clamp(clean.notificationRadius, MIN_NOTIFICATION_RADIUS, MAX_NOTIFICATION_RADIUS);
            return clean;
        }
    }
}
