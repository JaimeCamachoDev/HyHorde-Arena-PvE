package com.hyhorde.arenapve.horde;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HordeConfigPage
extends CustomUIPage {
    private static final String LAYOUT = "Pages/HordeConfigPage.ui";
    private final HordeService hordeService;

    private HordeConfigPage(PlayerRef playerRef, HordeService hordeService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.hordeService = hordeService;
    }

    public static void open(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, HordeService hordeService) {
        HordeConfigPage page = new HordeConfigPage(playerRef, hordeService);
        player.getPageManager().openCustomPage(playerEntityRef, store, (CustomUIPage)page);
    }

    public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
        HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
        boolean english = HordeService.isEnglishLanguage(config.language);
        boolean active = this.hordeService.isActive();
        List<String> enemyTypeOptions = this.hordeService.getEnemyTypeOptionsForCurrentRoles();
        List<String> rewardCategoryOptions = this.hordeService.getRewardCategoryOptions();
        String rewardCategory = HordeConfigPage.firstNonEmpty(config.rewardCategory, this.hordeService.getRewardCategory());
        List<String> rewardItemSuggestions = this.hordeService.getRewardItemSuggestions(rewardCategory);
        commandBuilder.append(LAYOUT).set("#SpawnX.Value", HordeConfigPage.formatDouble(config.spawnX)).set("#SpawnY.Value", HordeConfigPage.formatDouble(config.spawnY)).set("#SpawnZ.Value", HordeConfigPage.formatDouble(config.spawnZ)).set("#MinRadius.Value", HordeConfigPage.formatDouble(config.minSpawnRadius)).set("#MaxRadius.Value", HordeConfigPage.formatDouble(config.maxSpawnRadius)).set("#Rounds.Value", Integer.toString(config.rounds)).set("#BaseEnemies.Value", Integer.toString(config.baseEnemiesPerRound)).set("#EnemiesPerRound.Value", Integer.toString(config.enemiesPerRoundIncrement)).set("#WaveDelay.Value", Integer.toString(config.waveDelaySeconds)).set("#PlayerMultiplier.Value", Integer.toString(config.playerMultiplier)).set("#EnemyType.Value", config.enemyType == null ? "undead" : config.enemyType).set("#Language.Value", HordeService.getLanguageDisplay(config.language)).set("#RewardEveryRounds.Value", Integer.toString(config.rewardEveryRounds)).set("#RewardCategory.Value", rewardCategory).set("#RewardItemId.Value", config.rewardItemId == null ? "" : config.rewardItemId).set("#RewardItemQuantity.Value", Integer.toString(config.rewardItemQuantity)).set("#FinalBossEnabled.Value", HordeConfigPage.finalBossDisplay(config.finalBossEnabled, english)).set("#EnemyLevelMin.Value", Integer.toString(config.enemyLevelMin)).set("#EnemyLevelMax.Value", Integer.toString(config.enemyLevelMax)).set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config, english)).set("#StatusLabel.Text", this.hordeService.getStatusLine()).set("#RoleHelpLabel.Text", HordeConfigPage.buildEnemyTypesHint(enemyTypeOptions, config.enemyType, english)).set("#RewardCommandsHelpLabel.Text", HordeConfigPage.buildRewardItemsHint(rewardCategoryOptions, rewardCategory, rewardItemSuggestions, config.rewardItemId, english)).set("#StartButton.Visible", !active).set("#StopButton.Visible", active).set("#SkipRoundButton.Visible", active);
        this.setLocalizedTexts(commandBuilder, english);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"action", (String)"close")).addEventBinding(CustomUIEventBindingType.Activating, "#SetSpawnButton", EventData.of((String)"action", (String)"set_spawn_here")).addEventBinding(CustomUIEventBindingType.Activating, "#RolesButton", EventData.of((String)"action", (String)"enemy_types")).addEventBinding(CustomUIEventBindingType.Activating, "#EnemyTypePrevButton", this.buildConfigSnapshotEvent("enemy_prev")).addEventBinding(CustomUIEventBindingType.Activating, "#EnemyTypeNextButton", this.buildConfigSnapshotEvent("enemy_next")).addEventBinding(CustomUIEventBindingType.Activating, "#LanguagePrevButton", this.buildConfigSnapshotEvent("language_prev")).addEventBinding(CustomUIEventBindingType.Activating, "#LanguageNextButton", this.buildConfigSnapshotEvent("language_next")).addEventBinding(CustomUIEventBindingType.Activating, "#RewardCategoryPrevButton", this.buildConfigSnapshotEvent("reward_category_prev")).addEventBinding(CustomUIEventBindingType.Activating, "#RewardCategoryNextButton", this.buildConfigSnapshotEvent("reward_category_next")).addEventBinding(CustomUIEventBindingType.Activating, "#RewardItemPrevButton", this.buildConfigSnapshotEvent("reward_prev")).addEventBinding(CustomUIEventBindingType.Activating, "#RewardItemNextButton", this.buildConfigSnapshotEvent("reward_next")).addEventBinding(CustomUIEventBindingType.Activating, "#FinalBossPrevButton", this.buildConfigSnapshotEvent("final_boss_prev")).addEventBinding(CustomUIEventBindingType.Activating, "#FinalBossNextButton", this.buildConfigSnapshotEvent("final_boss_next")).addEventBinding(CustomUIEventBindingType.Activating, "#RewardTypesButton", EventData.of((String)"action", (String)"reward_types")).addEventBinding(CustomUIEventBindingType.Activating, "#ReloadModButton", EventData.of((String)"action", (String)"reload_mod")).addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", this.buildConfigSnapshotEvent("save")).addEventBinding(CustomUIEventBindingType.Activating, "#StartButton", this.buildConfigSnapshotEvent("start")).addEventBinding(CustomUIEventBindingType.Activating, "#StopButton", EventData.of((String)"action", (String)"stop")).addEventBinding(CustomUIEventBindingType.Activating, "#SkipRoundButton", EventData.of((String)"action", (String)"skip_round"));
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        JsonObject payload;
        boolean english = this.isEnglish();
        try {
            payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)(english ? "Could not parse the UI event payload." : "No se pudo interpretar el evento de la UI.")));
            return;
        }
        try {
            String action = HordeConfigPage.read(payload, "action");
            EntityStore entityStore = (EntityStore)store.getExternalData();
            World world = entityStore == null ? null : entityStore.getWorld();
            if (world == null && HordeConfigPage.requiresWorld(action)) {
                this.playerRef.sendMessage(Message.raw((String)(english ? "Could not access the active world to process this UI action." : "No se pudo acceder al mundo actual para procesar la accion de UI.")));
                this.safeRebuild();
                return;
            }
            HordeService.OperationResult result = null;
            switch (action) {
                case "close": {
                    this.close();
                    return;
                }
                case "set_spawn_here": {
                    result = this.hordeService.setSpawnFromPlayer(this.playerRef, world);
                    break;
                }
                case "enemy_types":
                case "roles": {
                    this.sendEnemyTypesPreview();
                    break;
                }
                case "enemy_prev": {
                    result = this.cycleEnemyType(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "enemy_next": {
                    result = this.cycleEnemyType(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "reward_prev": {
                    result = this.cycleRewardItem(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "reward_next": {
                    result = this.cycleRewardItem(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "reward_category_prev": {
                    result = this.cycleRewardCategory(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "reward_category_next": {
                    result = this.cycleRewardCategory(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "final_boss_prev": {
                    result = this.cycleFinalBoss(HordeConfigPage.extractConfigValues(payload), world);
                    break;
                }
                case "final_boss_next": {
                    result = this.cycleFinalBoss(HordeConfigPage.extractConfigValues(payload), world);
                    break;
                }
                case "language_prev": {
                    result = this.cycleLanguage(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "language_next": {
                    result = this.cycleLanguage(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "reward_types": {
                    this.sendRewardTypesPreview();
                    break;
                }
                case "reload_mod": {
                    result = this.hordeService.reloadPlugin();
                    if (result != null) {
                        this.playerRef.sendMessage(Message.raw((String)result.getMessage()));
                        if (result.isSuccess()) {
                            this.close();
                            return;
                        }
                        result = null;
                    }
                    break;
                }
                case "save": {
                    result = this.hordeService.applyUiConfig(HordeConfigPage.extractConfigValues(payload), world);
                    break;
                }
                case "start": {
                    result = this.hordeService.applyUiConfig(HordeConfigPage.extractConfigValues(payload), world);
                    if (!result.isSuccess()) break;
                    result = this.hordeService.start(store, this.playerRef, world);
                    break;
                }
                case "stop": {
                    result = this.hordeService.stop(true);
                    break;
                }
                case "skip_round": {
                    result = this.hordeService.skipToNextRound(world);
                    break;
                }
                default: {
                    result = HordeService.OperationResult.fail(english ? "Unknown UI action: " + action : "Accion de UI desconocida: " + action);
                }
            }
            if (result != null) {
                this.playerRef.sendMessage(Message.raw((String)result.getMessage()));
            }
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)(english ? "Internal error while processing horde UI. Check server logs and try again." : "Error interno al procesar la UI de horda. Revisa logs e intenta de nuevo.")));
        }
        this.safeRebuild();
    }

    private static boolean requiresWorld(String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        switch (action) {
            case "set_spawn_here":
            case "enemy_prev":
            case "enemy_next":
            case "reward_prev":
            case "reward_next":
            case "reward_category_prev":
            case "reward_category_next":
            case "final_boss_prev":
            case "final_boss_next":
            case "language_prev":
            case "language_next":
            case "save":
            case "skip_round":
            case "start": {
                return true;
            }
        }
        return false;
    }

    private void safeRebuild() {
        try {
            this.rebuild();
        }
        catch (Exception ignored) {
            // avoid bubbling UI rebuild failures to the caller thread
        }
    }

    private EventData buildConfigSnapshotEvent(String action) {
        return EventData.of((String)"action", (String)action).append("@SpawnX", "#SpawnX.Value").append("@SpawnY", "#SpawnY.Value").append("@SpawnZ", "#SpawnZ.Value").append("@MinRadius", "#MinRadius.Value").append("@MaxRadius", "#MaxRadius.Value").append("@Rounds", "#Rounds.Value").append("@BaseEnemies", "#BaseEnemies.Value").append("@EnemiesPerRound", "#EnemiesPerRound.Value").append("@WaveDelay", "#WaveDelay.Value").append("@PlayerMultiplier", "#PlayerMultiplier.Value").append("@EnemyType", "#EnemyType.Value").append("@Language", "#Language.Value").append("@RewardEveryRounds", "#RewardEveryRounds.Value").append("@RewardCategory", "#RewardCategory.Value").append("@RewardItemId", "#RewardItemId.Value").append("@RewardItemQuantity", "#RewardItemQuantity.Value").append("@FinalBossEnabled", "#FinalBossEnabled.Value").append("@EnemyLevelMin", "#EnemyLevelMin.Value").append("@EnemyLevelMax", "#EnemyLevelMax.Value");
    }

    private void sendEnemyTypesPreview() {
        List<String> diagnostics = this.hordeService.getEnemyTypeDiagnostics();
        boolean english = this.isEnglish();
        this.playerRef.sendMessage(Message.raw((String)((english ? "Detected categories" : "Categorias detectadas") + " (" + diagnostics.size() + "):")));
        for (String line : diagnostics) {
            this.playerRef.sendMessage(Message.raw((String)(" - " + line)));
        }
    }

    private void sendRewardTypesPreview() {
        HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
        String rewardCategory = HordeConfigPage.firstNonEmpty(config.rewardCategory, this.hordeService.getRewardCategory());
        List<String> suggestions = this.hordeService.getRewardItemSuggestions(rewardCategory);
        boolean english = this.isEnglish();
        if (suggestions.isEmpty()) {
            this.playerRef.sendMessage(Message.raw((String)(english ? "No valid reward items detected in this modpack." : "No hay items recompensa validos detectados en este modpack.")));
            return;
        }
        int total = suggestions.size();
        int previewCount = Math.min(24, total);
        List<String> preview = suggestions.subList(0, previewCount);
        String safeTestItem = suggestions.get(0);
        for (String candidate : suggestions) {
            if ("random".equalsIgnoreCase(candidate) || "random_all".equalsIgnoreCase(candidate)) continue;
            safeTestItem = candidate;
            break;
        }
        this.playerRef.sendMessage(Message.raw((String)((english ? "Detected reward items for category '" : "Items recompensa detectados para categoria '") + rewardCategory + "': " + total + ".")));
        this.playerRef.sendMessage(Message.raw((String)((english ? "Recommended safe test item: " : "Item de test recomendado (seguro): ") + safeTestItem)));
        this.playerRef.sendMessage(Message.raw((String)("Preview (" + previewCount + "): " + String.join(", ", preview))));
        if (total > previewCount) {
            this.playerRef.sendMessage(Message.raw((String)(english ? "There are +" + (total - previewCount) + " IDs. Use < > buttons to browse more options." : "Hay +" + (total - previewCount) + " IDs. Usa los botones < > para recorrer mas opciones.")));
        }
        this.playerRef.sendMessage(Message.raw((String)(english ? "Tip: use 'random' (current category) or 'random_all' (all categories)." : "Tip extra: usa 'random' (categoria actual) o 'random_all' (todas las categorias).")));
        this.playerRef.sendMessage(Message.raw((String)(english ? "You can also paste a full ItemDumper line and the ID will be auto-extracted." : "Tambien puedes pegar una linea completa de ItemDumper y se intentara extraer el ID automaticamente.")));
    }

    private HordeService.OperationResult cycleEnemyType(Map<String, String> values, World world, int offset) {
        List<String> enemyTypes = this.hordeService.getEnemyTypeOptionsForCurrentRoles();
        boolean english = this.isEnglish();
        if (enemyTypes.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No horde categories available." : "No hay categorias de horda disponibles.");
        }
        String currentType = HordeConfigPage.normalizeEnemyTypeInput(HordeConfigPage.firstNonEmpty(values.get("enemyType"), this.hordeService.getConfigSnapshot().enemyType));
        int currentIndex = enemyTypes.indexOf(currentType);
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, enemyTypes.size());
        return this.hordeService.setEnemyType(enemyTypes.get(nextIndex));
    }

    private HordeService.OperationResult cycleRewardItem(Map<String, String> values, World world, int offset) {
        String rewardCategory = HordeConfigPage.normalizeRewardCategoryInput(HordeConfigPage.firstNonEmpty(values.get("rewardCategory"), this.hordeService.getConfigSnapshot().rewardCategory, this.hordeService.getRewardCategory()));
        List<String> suggestions = this.hordeService.getRewardItemSuggestions(rewardCategory);
        boolean english = this.isEnglish();
        if (suggestions.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No suggested reward items available." : "No hay items recompensa sugeridos.");
        }
        String currentItem = HordeConfigPage.firstNonEmpty(values.get("rewardItemId")).trim();
        int currentIndex = suggestions.indexOf(currentItem);
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, suggestions.size());
        values.put("rewardItemId", suggestions.get(nextIndex));
        values.put("rewardCategory", rewardCategory);
        return this.hordeService.applyUiConfig(values, world);
    }

    private HordeService.OperationResult cycleRewardCategory(Map<String, String> values, World world, int offset) {
        List<String> categories = this.hordeService.getRewardCategoryOptions();
        boolean english = this.isEnglish();
        if (categories.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No reward categories available." : "No hay categorias de recompensa disponibles.");
        }
        String currentCategory = HordeConfigPage.normalizeRewardCategoryInput(HordeConfigPage.firstNonEmpty(values.get("rewardCategory"), this.hordeService.getConfigSnapshot().rewardCategory, this.hordeService.getRewardCategory()));
        int currentIndex = categories.indexOf(currentCategory);
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, categories.size());
        String nextCategory = categories.get(nextIndex);
        List<String> nextItems = this.hordeService.getRewardItemSuggestions(nextCategory);
        if (!nextItems.isEmpty()) {
            String nextItem = nextItems.get(0);
            for (String candidate : nextItems) {
                if ("random".equalsIgnoreCase(candidate) || "random_all".equalsIgnoreCase(candidate)) continue;
                nextItem = candidate;
                break;
            }
            values.put("rewardItemId", nextItem);
        }
        values.put("rewardCategory", nextCategory);
        return this.hordeService.applyUiConfig(values, world);
    }

    private HordeService.OperationResult cycleLanguage(Map<String, String> values, World world, int offset) {
        List<String> options = this.hordeService.getLanguageOptions();
        boolean english = this.isEnglish();
        if (options.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No language options available." : "No hay idiomas disponibles.");
        }
        String current = HordeService.normalizeLanguage(HordeConfigPage.extractLanguage(HordeConfigPage.firstNonEmpty(values.get("language"), this.hordeService.getLanguage())));
        int currentIndex = options.indexOf(current);
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, options.size());
        return this.hordeService.setLanguage(options.get(nextIndex));
    }

    private HordeService.OperationResult cycleFinalBoss(Map<String, String> values, World world) {
        boolean english = this.isEnglish();
        boolean current = HordeConfigPage.parseFinalBoss(values.get("finalBossEnabled"), this.hordeService.getConfigSnapshot().finalBossEnabled);
        values.put("finalBossEnabled", Boolean.toString(!current));
        HordeService.OperationResult result = this.hordeService.applyUiConfig(values, world);
        if (!result.isSuccess()) {
            return result;
        }
        return HordeService.OperationResult.ok(english ? "Final boss: " + (!current ? "enabled" : "disabled") + "." : "Boss final: " + (!current ? "activado" : "desactivado") + ".");
    }

    private static Map<String, String> extractConfigValues(JsonObject payload) {
        HashMap<String, String> values = new HashMap<String, String>();
        values.put("spawnX", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "spawnX"), HordeConfigPage.read(payload, "@SpawnX"), HordeConfigPage.read(payload, "SpawnX")));
        values.put("spawnY", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "spawnY"), HordeConfigPage.read(payload, "@SpawnY"), HordeConfigPage.read(payload, "SpawnY")));
        values.put("spawnZ", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "spawnZ"), HordeConfigPage.read(payload, "@SpawnZ"), HordeConfigPage.read(payload, "SpawnZ")));
        values.put("minRadius", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "minRadius"), HordeConfigPage.read(payload, "@MinRadius"), HordeConfigPage.read(payload, "MinRadius")));
        values.put("maxRadius", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "maxRadius"), HordeConfigPage.read(payload, "@MaxRadius"), HordeConfigPage.read(payload, "MaxRadius")));
        values.put("rounds", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rounds"), HordeConfigPage.read(payload, "@Rounds"), HordeConfigPage.read(payload, "Rounds")));
        values.put("baseEnemies", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "baseEnemies"), HordeConfigPage.read(payload, "@BaseEnemies"), HordeConfigPage.read(payload, "BaseEnemies")));
        values.put("enemiesPerRound", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemiesPerRound"), HordeConfigPage.read(payload, "@EnemiesPerRound"), HordeConfigPage.read(payload, "EnemiesPerRound")));
        values.put("waveDelay", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "waveDelay"), HordeConfigPage.read(payload, "@WaveDelay"), HordeConfigPage.read(payload, "WaveDelay")));
        values.put("playerMultiplier", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "playerMultiplier"), HordeConfigPage.read(payload, "@PlayerMultiplier"), HordeConfigPage.read(payload, "PlayerMultiplier")));
        values.put("enemyType", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyType"), HordeConfigPage.read(payload, "@EnemyType"), HordeConfigPage.read(payload, "EnemyType"), HordeConfigPage.read(payload, "role"), HordeConfigPage.read(payload, "@Role"), HordeConfigPage.read(payload, "Role")));
        values.put("language", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "language"), HordeConfigPage.read(payload, "@Language"), HordeConfigPage.read(payload, "Language")));
        values.put("rewardEveryRounds", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardEveryRounds"), HordeConfigPage.read(payload, "@RewardEveryRounds"), HordeConfigPage.read(payload, "RewardEveryRounds")));
        values.put("rewardCategory", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardCategory"), HordeConfigPage.read(payload, "@RewardCategory"), HordeConfigPage.read(payload, "RewardCategory")));
        values.put("rewardItemId", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardItemId"), HordeConfigPage.read(payload, "@RewardItemId"), HordeConfigPage.read(payload, "RewardItemId")));
        values.put("rewardItemQuantity", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardItemQuantity"), HordeConfigPage.read(payload, "@RewardItemQuantity"), HordeConfigPage.read(payload, "RewardItemQuantity")));
        values.put("finalBossEnabled", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "finalBossEnabled"), HordeConfigPage.read(payload, "@FinalBossEnabled"), HordeConfigPage.read(payload, "FinalBossEnabled")));
        values.put("enemyLevelMin", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyLevelMin"), HordeConfigPage.read(payload, "@EnemyLevelMin"), HordeConfigPage.read(payload, "EnemyLevelMin")));
        values.put("enemyLevelMax", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyLevelMax"), HordeConfigPage.read(payload, "@EnemyLevelMax"), HordeConfigPage.read(payload, "EnemyLevelMax")));
        return values;
    }

    private static String buildSpawnLabel(HordeService.HordeConfig config, boolean english) {
        if (!config.spawnConfigured) {
            if (english) {
                return "Horde center not configured. You can use your current position.";
            }
            return "Centro de horda no configurado. Puedes usar tu posicion actual.";
        }
        if (english) {
            return String.format(Locale.ROOT, "Current center: %.2f %.2f %.2f | World: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName);
        }
        return String.format(Locale.ROOT, "Centro actual: %.2f %.2f %.2f | Mundo: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName);
    }

    private static String buildEnemyTypesHint(List<String> enemyTypeOptions, String selectedEnemyType, boolean english) {
        if (enemyTypeOptions == null || enemyTypeOptions.isEmpty()) {
            return english ? "No horde categories available in this modpack." : "No hay categorias de horda disponibles en este modpack.";
        }
        String current = HordeConfigPage.normalizeEnemyTypeInput(HordeConfigPage.firstNonEmpty(selectedEnemyType, enemyTypeOptions.get(0)));
        String currentLabel = HordeConfigPage.enemyTypeLabel(current, english);
        String currentIds = HordeConfigPage.enemyTypePreviewIds(current);
        int maxPreview = 6;
        int total = enemyTypeOptions.size();
        List<String> preview = total > maxPreview ? enemyTypeOptions.subList(0, maxPreview) : enemyTypeOptions;
        String available = String.join(", ", preview);
        String availableSuffix = total > maxPreview ? (english ? " ... (+" + (total - maxPreview) + " more)" : " ... (+" + (total - maxPreview) + " mas)") : "";
        if (english) {
            return "Use < > to change category | Current: " + currentLabel + " | IDs: " + currentIds + " | Available: " + available + availableSuffix;
        }
        return "Usa < > para cambiar categoria | Actual: " + currentLabel + " | IDs: " + currentIds + " | Disponibles: " + available + availableSuffix;
    }

    private static String buildRewardItemsHint(List<String> rewardCategoryOptions, String selectedCategory, List<String> rewardItems, String selectedItemId, boolean english) {
        if (rewardCategoryOptions == null || rewardCategoryOptions.isEmpty()) {
            return english ? "No reward categories available." : "No hay categorias de recompensa disponibles.";
        }
        String currentCategory = HordeConfigPage.normalizeRewardCategoryInput(HordeConfigPage.firstNonEmpty(selectedCategory, rewardCategoryOptions.get(0)));
        String currentCategoryLabel = HordeConfigPage.rewardCategoryLabel(currentCategory);
        int categoryPreviewCount = Math.min(4, rewardCategoryOptions.size());
        List<String> categoryPreview = rewardCategoryOptions.subList(0, categoryPreviewCount);
        String categorySuffix = rewardCategoryOptions.size() > categoryPreviewCount ? (english ? " ... (+" + (rewardCategoryOptions.size() - categoryPreviewCount) + " more)" : " ... (+" + (rewardCategoryOptions.size() - categoryPreviewCount) + " mas)") : "";

        if (rewardItems == null || rewardItems.isEmpty()) {
            if (english) {
                return "Use < > on category | Current: " + currentCategoryLabel + " | Categories: " + String.join(", ", categoryPreview) + categorySuffix + " | No reward items available.";
            }
            return "Usa < > en categoria | Actual: " + currentCategoryLabel + " | Categorias: " + String.join(", ", categoryPreview) + categorySuffix + " | No hay items recompensa disponibles.";
        }

        int itemPreviewCount = Math.min(4, rewardItems.size());
        List<String> itemPreview = rewardItems.subList(0, itemPreviewCount);
        String itemSuffix = rewardItems.size() > itemPreviewCount ? (english ? " ... (+" + (rewardItems.size() - itemPreviewCount) + " more)" : " ... (+" + (rewardItems.size() - itemPreviewCount) + " mas)") : "";
        String currentItem = HordeConfigPage.firstNonEmpty(selectedItemId, rewardItems.get(0));
        if (english) {
            return "Use < > on category/item | Category: " + currentCategoryLabel + " | Item: " + currentItem + " | Categories: " + String.join(", ", categoryPreview) + categorySuffix + " | Items: " + String.join(", ", itemPreview) + itemSuffix + " | Modes: random, random_all";
        }
        return "Usa < > en categoria/item | Categoria: " + currentCategoryLabel + " | Item: " + currentItem + " | Categorias: " + String.join(", ", categoryPreview) + categorySuffix + " | Items: " + String.join(", ", itemPreview) + itemSuffix + " | Modos: random, random_all";
    }

    private static String rewardCategoryLabel(String rewardCategory) {
        String normalized = HordeConfigPage.normalizeRewardCategoryInput(rewardCategory);
        switch (normalized) {
            case "mithril":
                return "MITHRIL";
            case "onyxium":
                return "ONYXIUM";
            case "gemas":
                return "GEMAS";
            case "metales":
                return "METALES";
            case "materiales_raros":
                return "MATERIALES_RAROS";
            case "armas_especiales":
                return "ARMAS_ESPECIALES";
            case "items_especiales":
                return "ITEMS_ESPECIALES";
        }
        return normalized;
    }

    private static String enemyTypeLabel(String enemyType, boolean english) {
        switch (HordeConfigPage.normalizeEnemyTypeInput(enemyType)) {
            case "random": {
                return english ? "Random category" : "Categoria random";
            }
            case "random-all": {
                return english ? "Random all roles" : "Random total de roles";
            }
            case "undead": {
                return english ? "Undead horde" : "Horda no-muertos";
            }
            case "goblins": {
                return english ? "Goblin horde" : "Horda goblins";
            }
            case "scarak": {
                return english ? "Scarak horde" : "Horda scarak";
            }
            case "void": {
                return english ? "Void horde" : "Horda del vacio";
            }
            case "wild": {
                return english ? "Wild creatures" : "Criaturas agresivas";
            }
            case "elementals": {
                return english ? "Elemental horde" : "Elementales";
            }
        }
        return enemyType;
    }

    private static String enemyTypePreviewIds(String enemyType) {
        switch (HordeConfigPage.normalizeEnemyTypeInput(enemyType)) {
            case "random": {
                return "Random by category each spawn";
            }
            case "random-all": {
                return "Random from all hostile roles";
            }
            case "undead": {
                return "Aberrant_Zombie, Burnt_Zombie, Burnt_Skeleton_Archer...";
            }
            case "goblins": {
                return "Goblin_Scavenger, Goblin_Lobber, Goblin_Duke...";
            }
            case "scarak": {
                return "Dungeon_Scarak_Fighter, Dungeon_Scarak_Seeker, Dungeon_Scarak_Broodmother_Young";
            }
            case "void": {
                return "Crawler_Void, VoidSpawn, VoidTaken";
            }
            case "wild": {
                return "Crocodile, Cave_Raptor, Feran_Longtooth...";
            }
            case "elementals": {
                return "Earthen_Golem, Ember_Golem, Frost_Elemental, Fire_Elemental";
            }
        }
        return "-";
    }

    private boolean isEnglish() {
        return HordeService.isEnglishLanguage(this.hordeService.getLanguage());
    }

    private void setLocalizedTexts(UICommandBuilder commandBuilder, boolean english) {
        commandBuilder.set("#TitleLabel.Text", english ? "Horde PVE Config" : "Horda PVE Config")
                .set("#SubTitleLabel.Text", english ? "Minimal setup: center, waves, enemies, rewards and language" : "Config minima: centro, oleadas, enemigos, recompensas e idioma")
                .set("#SpawnLabel.Text", english ? "Center (X Y Z)" : "Centro (X Y Z)")
                .set("#SetSpawnButton.Text", english ? "Use my current position" : "Usar mi posicion actual")
                .set("#RadiusLabel.Text", english ? "Min / max radius" : "Radio min / max")
                .set("#RoundLabel.Text", english ? "Rounds" : "Rondas")
                .set("#BaseEnemiesLabel.Text", english ? "Base / round" : "Base ronda")
                .set("#EnemiesPerRoundLabel.Text", english ? "Inc. per round" : "Inc. por ronda")
                .set("#WaveDelayLabel.Text", english ? "Delay (s)" : "Espera (s)")
                .set("#PlayerMultiplierLabel.Text", english ? "Players (x)" : "Jugadores (x)")
                .set("#RoleLabel.Text", english ? "Horde category" : "Categoria de horda")
                .set("#RolesButton.Text", english ? "View categories" : "Ver categorias")
                .set("#LanguageLabel.Text", english ? "Interface language" : "Idioma interfaz")
                .set("#RewardEveryRoundsLabel.Text", english ? "Reward every round(s)" : "Recompensa por ronda(s)")
                .set("#RewardCategoryLabel.Text", english ? "Reward category" : "Categoria recompensa")
                .set("#RewardCommandsLabel.Text", english ? "Reward item" : "Item recompensa")
                .set("#RewardTypesButton.Text", english ? "View loot" : "Ver loot")
                .set("#RewardItemQuantityLabel.Text", english ? "Qty." : "Cant.")
                .set("#FinalBossLabel.Text", english ? "Final boss" : "Boss final")
                .set("#EnemyLevelRangeLabel.Text", english ? "Enemy level range" : "Rango nivel enemigos")
                .set("#EnemyLevelRangeSeparator.Text", "-")
                .set("#StatusTitleLabel.Text", english ? "Current status" : "Estado actual")
                .set("#ReloadModButton.Text", english ? "Reload mod" : "Recargar mod")
                .set("#SaveButton.Text", english ? "Save config" : "Guardar config")
                .set("#StartButton.Text", english ? "Start horde" : "Iniciar horda")
                .set("#StopButton.Text", english ? "Stop horde" : "Detener horda")
                .set("#SkipRoundButton.Text", english ? "Skip round" : "Pasar ronda")
                .set("#CloseButton.Text", english ? "Close" : "Cerrar");
    }

    private static String finalBossDisplay(boolean enabled, boolean english) {
        if (english) {
            return enabled ? "On" : "Off";
        }
        return enabled ? "Si" : "No";
    }

    private static boolean parseFinalBoss(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u');
        switch (normalized) {
            case "1":
            case "true":
            case "yes":
            case "y":
            case "on":
            case "si":
            case "enabled":
            case "enable": {
                return true;
            }
            case "0":
            case "false":
            case "no":
            case "n":
            case "off":
            case "disabled":
            case "disable": {
                return false;
            }
        }
        return fallback;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String normalizeEnemyTypeInput(String value) {
        if (value == null || value.isBlank()) {
            return "undead";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u');
        normalized = normalized.replace('_', '-').replace(' ', '-');
        switch (normalized) {
            case "no-muerto":
            case "no-muertos":
            case "nomuerto":
            case "nomuertos":
            case "undead":
            case "zombie":
            case "zombies":
            case "skeleton":
            case "skeletons":
            case "auto":
            case "role":
                return "undead";
            case "random":
            case "aleatorio":
            case "aleatoria":
            case "rand":
            case "rnd":
            case "azar": {
                return "random";
            }
            case "random-all":
            case "randomall":
            case "random-total":
            case "aleatorio-total":
            case "aleatorio-totales": {
                return "random-all";
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
        }
        return normalized;
    }

    private static String normalizeRewardCategoryInput(String value) {
        if (value == null || value.isBlank()) {
            return "mithril";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u');
        normalized = normalized.replace(' ', '_').replace('-', '_');
        switch (normalized) {
            case "mithril":
                return "mithril";
            case "onyxium":
                return "onyxium";
            case "gemas":
            case "gems":
                return "gemas";
            case "metales":
            case "metals":
                return "metales";
            case "materiales_raros":
            case "material_raro":
            case "rare_materials":
                return "materiales_raros";
            case "armas_especiales":
            case "special_weapons":
                return "armas_especiales";
            case "items_especiales":
            case "trofeos":
            case "special_items":
                return "items_especiales";
        }
        return "mithril";
    }

    private static String read(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String firstNonEmpty(String ... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            return value;
        }
        return "";
    }

    private static String extractLanguage(String value) {
        if (value == null || value.isBlank()) {
            return "es";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("english") || normalized.contains("(en)")) {
            return "en";
        }
        if (normalized.contains("espanol") || normalized.contains("espa\u00f1ol") || normalized.contains("(es)")) {
            return "es";
        }
        return normalized;
    }
}



