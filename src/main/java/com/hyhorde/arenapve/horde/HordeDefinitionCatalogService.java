package com.hyhorde.arenapve.horde;

import com.google.gson.Gson;
import com.hypixel.hytale.server.core.plugin.PluginBase;
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
import java.util.logging.Level;

final class HordeDefinitionCatalogService {
    private static final String DEFAULT_HORDE_ICON_ITEM_ID = "Ingredient_Bar_Gold";
    private static final String DEFAULT_HORDE_ID_WAVE = "wave";
    private static final String DEFAULT_HORDE_ID_TIMED = "timed";
    private static final String DEFAULT_HORDE_ID_HORDE = "horde";
    private final PluginBase plugin;
    private final Gson gson;
    private final Path definitionsPath;
    private final LinkedHashMap<String, HordeDefinition> definitionsById;

    HordeDefinitionCatalogService(PluginBase plugin, Gson gson, Path pluginDataPath) {
        this.plugin = plugin;
        this.gson = gson;
        this.definitionsPath = pluginDataPath.resolve("horde-definitions.json");
        this.definitionsById = new LinkedHashMap<String, HordeDefinition>();
        this.reloadFromDisk();
    }

    synchronized void reloadFromDisk() {
        this.definitionsById.clear();
        this.ensureDataDirectory();
        if (!Files.exists(this.definitionsPath, new LinkOption[0])) {
            this.addDefaultDefinitions(HordeService.HordeConfig.defaults());
            this.saveToDisk();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(this.definitionsPath, StandardCharsets.UTF_8);){
            HordeDefinitionsFile loaded = (HordeDefinitionsFile)this.gson.fromJson((Reader)reader, HordeDefinitionsFile.class);
            if (loaded != null && loaded.definitions != null) {
                for (HordeDefinition row : loaded.definitions) {
                    HordeDefinition clean = HordeDefinition.sanitize(row, HordeService.HordeConfig.defaults());
                    if (clean == null || clean.hordeId == null || clean.hordeId.isBlank()) {
                        continue;
                    }
                    clean = this.migrateLegacyDefaultDefinition(clean, HordeService.HordeConfig.defaults());
                    String rowKey = HordeDefinitionCatalogService.key(clean.hordeId);
                    if (this.definitionsById.containsKey(rowKey)) {
                        continue;
                    }
                    this.definitionsById.put(rowKey, clean);
                }
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo leer horde-definitions.json, se regenerara plantilla: %s", (Object)ex.getMessage());
        }
        if (this.definitionsById.isEmpty()) {
            this.addDefaultDefinitions(HordeService.HordeConfig.defaults());
            this.saveToDisk();
        }
    }

    synchronized List<HordeDefinitionSnapshot> getDefinitionsSnapshot() {
        ArrayList<HordeDefinitionSnapshot> rows = new ArrayList<HordeDefinitionSnapshot>();
        for (HordeDefinition row : this.definitionsById.values()) {
            if (row == null || row.hordeId == null || row.hordeId.isBlank()) {
                continue;
            }
            rows.add(HordeDefinitionSnapshot.from(row));
        }
        return rows;
    }

    synchronized HordeService.OperationResult createDraft(String requestedHordeId, HordeService.HordeConfig fallbackConfig, boolean english) {
        String base = HordeDefinitionCatalogService.clean(requestedHordeId);
        if (base.isBlank()) {
            base = this.nextId();
        }
        String uniqueId = this.uniqueId(base);
        HordeDefinition row = HordeDefinition.defaults(uniqueId, fallbackConfig);
        this.definitionsById.put(HordeDefinitionCatalogService.key(uniqueId), row);
        this.saveToDisk();
        return HordeService.OperationResult.ok(english ? "Horde created: " + uniqueId + "." : "Horda creada: " + uniqueId + ".");
    }

    synchronized HordeService.OperationResult delete(String hordeId, boolean english) {
        String cleanHordeId = HordeDefinitionCatalogService.clean(hordeId);
        if (cleanHordeId.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Select a horde to delete." : "Selecciona una horda para eliminar.");
        }
        HordeDefinition removed = this.definitionsById.remove(HordeDefinitionCatalogService.key(cleanHordeId));
        if (removed == null) {
            return HordeService.OperationResult.fail(english ? "Horde not found." : "Horda no encontrada.");
        }
        if (this.definitionsById.isEmpty()) {
            this.addDefaultDefinitions(HordeService.HordeConfig.defaults());
        }
        this.saveToDisk();
        return HordeService.OperationResult.ok(english ? "Deleted horde: " + removed.hordeId + "." : "Horda eliminada: " + removed.hordeId + ".");
    }

    private void addDefaultDefinitions(HordeService.HordeConfig fallbackConfig) {
        HordeService.HordeConfig config = fallbackConfig == null ? HordeService.HordeConfig.defaults() : fallbackConfig.copy();

        HordeDefinition wave = HordeDefinition.defaults(this.uniqueId(DEFAULT_HORDE_ID_WAVE), config);
        wave.hordeMode = HordeService.normalizeHordeMode("wave");
        wave.iconItemId = "Ingredient_Bar_Iron";
        wave.rounds = HordeDefinitionCatalogService.clamp(Math.max(8, wave.rounds), HordeConfigRules.MIN_ROUNDS, HordeConfigRules.MAX_ROUNDS);
        wave.baseEnemies = HordeDefinitionCatalogService.clamp(Math.max(8, wave.baseEnemies), HordeConfigRules.MIN_ENEMIES_PER_ROUND, HordeConfigRules.MAX_ENEMIES_PER_ROUND);
        wave.enemiesPerRound = HordeDefinitionCatalogService.clamp(Math.max(2, wave.enemiesPerRound), HordeConfigRules.MIN_ENEMY_INCREMENT, HordeConfigRules.MAX_ENEMY_INCREMENT);
        wave.waveDelay = HordeDefinitionCatalogService.clamp(Math.max(12, wave.waveDelay), HordeConfigRules.MIN_WAVE_DELAY_SECONDS, HordeConfigRules.MAX_WAVE_DELAY_SECONDS);
        this.definitionsById.put(HordeDefinitionCatalogService.key(wave.hordeId), HordeDefinition.sanitize(wave, config));

        HordeDefinition timed = HordeDefinition.defaults(this.uniqueId(DEFAULT_HORDE_ID_TIMED), config);
        timed.hordeMode = HordeService.normalizeHordeMode("timed");
        timed.iconItemId = "Ingredient_Crystal_Yellow";
        timed.rounds = HordeDefinitionCatalogService.clamp(Math.max(6, timed.rounds), HordeConfigRules.MIN_ROUNDS, HordeConfigRules.MAX_ROUNDS);
        timed.baseEnemies = HordeDefinitionCatalogService.clamp(Math.max(10, timed.baseEnemies), HordeConfigRules.MIN_ENEMIES_PER_ROUND, HordeConfigRules.MAX_ENEMIES_PER_ROUND);
        timed.enemiesPerRound = HordeDefinitionCatalogService.clamp(Math.max(3, timed.enemiesPerRound), HordeConfigRules.MIN_ENEMY_INCREMENT, HordeConfigRules.MAX_ENEMY_INCREMENT);
        timed.waveDelay = HordeDefinitionCatalogService.clamp(Math.max(180, timed.waveDelay), HordeConfigRules.MIN_WAVE_DELAY_SECONDS, HordeConfigRules.MAX_WAVE_DELAY_SECONDS);
        this.definitionsById.put(HordeDefinitionCatalogService.key(timed.hordeId), HordeDefinition.sanitize(timed, config));

        HordeDefinition horde = HordeDefinition.defaults(this.uniqueId(DEFAULT_HORDE_ID_HORDE), config);
        horde.hordeMode = HordeService.normalizeHordeMode("horde");
        horde.iconItemId = "Ingredient_Bar_Gold";
        horde.rounds = HordeDefinitionCatalogService.clamp(Math.max(1, horde.rounds), HordeConfigRules.MIN_ROUNDS, HordeConfigRules.MAX_ROUNDS);
        horde.baseEnemies = HordeDefinitionCatalogService.clamp(Math.max(16, horde.baseEnemies), HordeConfigRules.MIN_ENEMIES_PER_ROUND, HordeConfigRules.MAX_ENEMIES_PER_ROUND);
        horde.enemiesPerRound = HordeDefinitionCatalogService.clamp(Math.max(0, horde.enemiesPerRound), HordeConfigRules.MIN_ENEMY_INCREMENT, HordeConfigRules.MAX_ENEMY_INCREMENT);
        horde.waveDelay = HordeDefinitionCatalogService.clamp(Math.max(180, horde.waveDelay), HordeConfigRules.MIN_WAVE_DELAY_SECONDS, HordeConfigRules.MAX_WAVE_DELAY_SECONDS);
        this.definitionsById.put(HordeDefinitionCatalogService.key(horde.hordeId), HordeDefinition.sanitize(horde, config));
    }

    private HordeDefinition migrateLegacyDefaultDefinition(HordeDefinition source, HordeService.HordeConfig fallbackConfig) {
        if (source == null) {
            return null;
        }
        HordeDefinition migrated = source.copy();
        String id = HordeDefinitionCatalogService.clean(migrated.hordeId).toLowerCase(Locale.ROOT);
        if ("wave_default".equals(id)) {
            migrated.hordeId = this.uniqueId(DEFAULT_HORDE_ID_WAVE);
            migrated.hordeMode = HordeService.normalizeHordeMode("wave");
            migrated.iconItemId = "Ingredient_Bar_Iron";
            return HordeDefinition.sanitize(migrated, fallbackConfig);
        }
        if ("timed_default".equals(id)) {
            migrated.hordeId = this.uniqueId(DEFAULT_HORDE_ID_TIMED);
            migrated.hordeMode = HordeService.normalizeHordeMode("timed");
            migrated.iconItemId = "Ingredient_Crystal_Yellow";
            return HordeDefinition.sanitize(migrated, fallbackConfig);
        }
        if ("horde_default".equals(id)) {
            migrated.hordeId = this.uniqueId(DEFAULT_HORDE_ID_HORDE);
            migrated.hordeMode = HordeService.normalizeHordeMode("horde");
            migrated.iconItemId = "Ingredient_Bar_Gold";
            return HordeDefinition.sanitize(migrated, fallbackConfig);
        }
        return migrated;
    }

    synchronized HordeService.OperationResult upsertFromValues(Map<String, String> values, HordeService.HordeConfig fallbackConfig, boolean english) {
        String selected = HordeDefinitionCatalogService.clean(values.get("hordeSelected"));
        String requested = HordeDefinitionCatalogService.clean(HordeDefinitionCatalogService.firstNonBlank(values.get("hordeEditId"), selected));
        if (requested.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Horde ID is required." : "El ID de horda es obligatorio.");
        }
        String selectedKey = HordeDefinitionCatalogService.key(selected);
        String requestedKey = HordeDefinitionCatalogService.key(requested);
        HordeDefinition existing = selectedKey.isBlank() ? null : this.definitionsById.get(selectedKey);
        boolean creating = existing == null;
        if (!selectedKey.equals(requestedKey) && this.definitionsById.containsKey(requestedKey)) {
            return HordeService.OperationResult.fail(english ? "A horde with that ID already exists." : "Ya existe una horda con ese ID.");
        }
        HordeDefinition target = creating ? HordeDefinition.defaults(requested, fallbackConfig) : existing.copy();
        target.hordeId = requested;
        target.enemyType = HordeDefinitionCatalogService.clean(HordeDefinitionCatalogService.firstNonBlank(values.get("enemyType"), target.enemyType, fallbackConfig.enemyType));
        if (target.enemyType.isBlank()) {
            target.enemyType = HordeService.HordeConfig.defaults().enemyType;
        }
        target.iconItemId = HordeDefinitionCatalogService.clean(HordeDefinitionCatalogService.firstNonBlank(values.get("hordeEditIconItemId"), target.iconItemId, HordeDefinitionCatalogService.resolveDefaultIconItemId(target.enemyType)));
        if (target.iconItemId.isBlank()) {
            target.iconItemId = HordeDefinitionCatalogService.resolveDefaultIconItemId(target.enemyType);
        }
        target.minRadius = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseDouble(values.get("minRadius"), target.minRadius), HordeConfigRules.MIN_RADIUS, HordeConfigRules.MAX_RADIUS);
        target.maxRadius = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseDouble(values.get("maxRadius"), target.maxRadius), HordeConfigRules.MIN_RADIUS, HordeConfigRules.MAX_RADIUS);
        if (target.maxRadius < target.minRadius) {
            double swap = target.minRadius;
            target.minRadius = target.maxRadius;
            target.maxRadius = swap;
        }
        target.rounds = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseInt(values.get("rounds"), target.rounds), HordeConfigRules.MIN_ROUNDS, HordeConfigRules.MAX_ROUNDS);
        target.baseEnemies = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseInt(values.get("baseEnemies"), target.baseEnemies), HordeConfigRules.MIN_ENEMIES_PER_ROUND, HordeConfigRules.MAX_ENEMIES_PER_ROUND);
        target.enemiesPerRound = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseInt(values.get("enemiesPerRound"), target.enemiesPerRound), HordeConfigRules.MIN_ENEMY_INCREMENT, HordeConfigRules.MAX_ENEMY_INCREMENT);
        target.waveDelay = HordeDefinitionCatalogService.clamp(HordeDefinitionCatalogService.parseInt(values.get("waveDelay"), target.waveDelay), HordeConfigRules.MIN_WAVE_DELAY_SECONDS, HordeConfigRules.MAX_WAVE_DELAY_SECONDS);
        target.hordeMode = HordeService.normalizeHordeMode(HordeDefinitionCatalogService.firstNonBlank(values.get("hordeMode"), target.hordeMode, fallbackConfig.hordeMode));
        target.finalBossEnabled = HordeDefinitionCatalogService.parseBoolean(values.get("finalBossEnabled"), target.finalBossEnabled);
        if (!selectedKey.isBlank() && !selectedKey.equals(requestedKey)) {
            this.definitionsById.remove(selectedKey);
        }
        this.definitionsById.put(requestedKey, HordeDefinition.sanitize(target, fallbackConfig));
        this.saveToDisk();
        return HordeService.OperationResult.ok(english ? (creating ? "Horde saved: " : "Horde updated: ") + target.hordeId + "." : (creating ? "Horda guardada: " : "Horda actualizada: ") + target.hordeId + ".");
    }

    private void ensureDataDirectory() {
        try {
            Path parent = this.definitionsPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo crear carpeta para horde-definitions.json: %s", (Object)ex.getMessage());
        }
    }

    private void saveToDisk() {
        this.ensureDataDirectory();
        HordeDefinitionsFile payload = new HordeDefinitionsFile();
        payload.definitions = new ArrayList<HordeDefinition>(this.definitionsById.values());
        try (BufferedWriter writer = Files.newBufferedWriter(this.definitionsPath, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson((Object)payload, writer);
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo guardar horde-definitions.json: %s", (Object)ex.getMessage());
        }
    }

    private String nextId() {
        int counter = 1;
        while (counter < 100000) {
            String candidate = "horde_" + counter;
            if (!this.definitionsById.containsKey(HordeDefinitionCatalogService.key(candidate))) {
                return candidate;
            }
            ++counter;
        }
        return "horde_" + System.currentTimeMillis();
    }

    private String uniqueId(String requested) {
        String base = HordeDefinitionCatalogService.clean(requested);
        if (base.isBlank()) {
            return this.nextId();
        }
        String key = HordeDefinitionCatalogService.key(base);
        if (!this.definitionsById.containsKey(key)) {
            return base;
        }
        int suffix = 2;
        while (suffix < 100000) {
            String candidate = base + "_" + suffix;
            if (!this.definitionsById.containsKey(HordeDefinitionCatalogService.key(candidate))) {
                return candidate;
            }
            ++suffix;
        }
        return base + "_" + System.currentTimeMillis();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String key(String value) {
        return HordeDefinitionCatalogService.clean(value).toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String ... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String clean = HordeDefinitionCatalogService.clean(value);
            if (!clean.isBlank()) {
                return clean;
            }
        }
        return "";
    }

    private static int parseInt(String raw, int fallback) {
        String clean = HordeDefinitionCatalogService.clean(raw).replace(',', '.');
        if (clean.isBlank()) {
            return fallback;
        }
        try {
            return (int)Math.round(Double.parseDouble(clean));
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        String clean = HordeDefinitionCatalogService.clean(raw).replace(',', '.');
        if (clean.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(clean);
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        String clean = HordeDefinitionCatalogService.clean(raw).toLowerCase(Locale.ROOT);
        if (clean.isBlank()) {
            return fallback;
        }
        if ("true".equals(clean) || "1".equals(clean) || "yes".equals(clean) || "on".equals(clean) || "si".equals(clean)) {
            return true;
        }
        if ("false".equals(clean) || "0".equals(clean) || "no".equals(clean) || "off".equals(clean)) {
            return false;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String resolveDefaultIconItemId(String enemyType) {
        String normalized = HordeDefinitionCatalogService.clean(enemyType).toLowerCase(Locale.ROOT);
        if ("random".equals(normalized) || "random-all".equals(normalized)) {
            return "Potion_Signature_Greater";
        }
        if ("void".equals(normalized) || "scarak".equals(normalized) || "elementals".equals(normalized)) {
            return "Weapon_Wand_Wood";
        }
        return DEFAULT_HORDE_ICON_ITEM_ID;
    }

    static final class HordeDefinitionSnapshot {
        final String hordeId;
        final String enemyType;
        final String iconItemId;
        final double minRadius;
        final double maxRadius;
        final int rounds;
        final int baseEnemies;
        final int enemiesPerRound;
        final int waveDelay;
        final String hordeMode;
        final boolean finalBossEnabled;

        private HordeDefinitionSnapshot(String hordeId, String enemyType, String iconItemId, double minRadius, double maxRadius, int rounds, int baseEnemies, int enemiesPerRound, int waveDelay, String hordeMode, boolean finalBossEnabled) {
            this.hordeId = hordeId;
            this.enemyType = enemyType;
            this.iconItemId = iconItemId;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.rounds = rounds;
            this.baseEnemies = baseEnemies;
            this.enemiesPerRound = enemiesPerRound;
            this.waveDelay = waveDelay;
            this.hordeMode = hordeMode;
            this.finalBossEnabled = finalBossEnabled;
        }

        private static HordeDefinitionSnapshot from(HordeDefinition source) {
            HordeDefinition clean = HordeDefinition.sanitize(source, HordeService.HordeConfig.defaults());
            return new HordeDefinitionSnapshot(clean.hordeId, clean.enemyType, clean.iconItemId, clean.minRadius, clean.maxRadius, clean.rounds, clean.baseEnemies, clean.enemiesPerRound, clean.waveDelay, clean.hordeMode, clean.finalBossEnabled);
        }
    }

    private static final class HordeDefinitionsFile {
        private List<HordeDefinition> definitions;
    }

    private static final class HordeDefinition {
        private String hordeId;
        private String enemyType;
        private String iconItemId;
        private double minRadius;
        private double maxRadius;
        private int rounds;
        private int baseEnemies;
        private int enemiesPerRound;
        private int waveDelay;
        private String hordeMode;
        private boolean finalBossEnabled;

        private static HordeDefinition defaults(String hordeId, HordeService.HordeConfig fallbackConfig) {
            HordeService.HordeConfig config = fallbackConfig == null ? HordeService.HordeConfig.defaults() : fallbackConfig;
            HordeDefinition row = new HordeDefinition();
            row.hordeId = HordeDefinitionCatalogService.clean(hordeId);
            row.enemyType = HordeDefinitionCatalogService.clean(config.enemyType);
            row.iconItemId = HordeDefinitionCatalogService.resolveDefaultIconItemId(row.enemyType);
            row.minRadius = config.minSpawnRadius;
            row.maxRadius = config.maxSpawnRadius;
            row.rounds = config.rounds;
            row.baseEnemies = config.baseEnemiesPerRound;
            row.enemiesPerRound = config.enemiesPerRoundIncrement;
            row.waveDelay = config.waveDelaySeconds;
            row.hordeMode = HordeService.normalizeHordeMode(config.hordeMode);
            row.finalBossEnabled = config.finalBossEnabled;
            return HordeDefinition.sanitize(row, config);
        }

        private HordeDefinition copy() {
            HordeDefinition copy = new HordeDefinition();
            copy.hordeId = this.hordeId;
            copy.enemyType = this.enemyType;
            copy.iconItemId = this.iconItemId;
            copy.minRadius = this.minRadius;
            copy.maxRadius = this.maxRadius;
            copy.rounds = this.rounds;
            copy.baseEnemies = this.baseEnemies;
            copy.enemiesPerRound = this.enemiesPerRound;
            copy.waveDelay = this.waveDelay;
            copy.hordeMode = this.hordeMode;
            copy.finalBossEnabled = this.finalBossEnabled;
            return copy;
        }

        private static HordeDefinition sanitize(HordeDefinition source, HordeService.HordeConfig fallbackConfig) {
            if (source == null) {
                return HordeDefinition.defaults("horde_1", fallbackConfig);
            }
            HordeService.HordeConfig fallback = fallbackConfig == null ? HordeService.HordeConfig.defaults() : fallbackConfig;
            HordeDefinition clean = source.copy();
            clean.hordeId = HordeDefinitionCatalogService.clean(clean.hordeId);
            if (clean.hordeId.isBlank()) {
                clean.hordeId = "horde_1";
            }
            clean.enemyType = HordeDefinitionCatalogService.clean(clean.enemyType);
            if (clean.enemyType.isBlank()) {
                clean.enemyType = HordeDefinitionCatalogService.clean(fallback.enemyType);
            }
            if (clean.enemyType.isBlank()) {
                clean.enemyType = HordeService.HordeConfig.defaults().enemyType;
            }
            clean.iconItemId = HordeDefinitionCatalogService.clean(clean.iconItemId);
            if (clean.iconItemId.isBlank()) {
                clean.iconItemId = HordeDefinitionCatalogService.resolveDefaultIconItemId(clean.enemyType);
            }
            clean.minRadius = HordeDefinitionCatalogService.clamp(clean.minRadius, HordeConfigRules.MIN_RADIUS, HordeConfigRules.MAX_RADIUS);
            clean.maxRadius = HordeDefinitionCatalogService.clamp(clean.maxRadius, HordeConfigRules.MIN_RADIUS, HordeConfigRules.MAX_RADIUS);
            if (clean.maxRadius < clean.minRadius) {
                double swap = clean.minRadius;
                clean.minRadius = clean.maxRadius;
                clean.maxRadius = swap;
            }
            clean.rounds = HordeDefinitionCatalogService.clamp(clean.rounds, HordeConfigRules.MIN_ROUNDS, HordeConfigRules.MAX_ROUNDS);
            clean.baseEnemies = HordeDefinitionCatalogService.clamp(clean.baseEnemies, HordeConfigRules.MIN_ENEMIES_PER_ROUND, HordeConfigRules.MAX_ENEMIES_PER_ROUND);
            clean.enemiesPerRound = HordeDefinitionCatalogService.clamp(clean.enemiesPerRound, HordeConfigRules.MIN_ENEMY_INCREMENT, HordeConfigRules.MAX_ENEMY_INCREMENT);
            clean.waveDelay = HordeDefinitionCatalogService.clamp(clean.waveDelay, HordeConfigRules.MIN_WAVE_DELAY_SECONDS, HordeConfigRules.MAX_WAVE_DELAY_SECONDS);
            clean.hordeMode = HordeService.normalizeHordeMode(HordeDefinitionCatalogService.firstNonBlank(clean.hordeMode, fallback.hordeMode));
            return clean;
        }
    }
}
