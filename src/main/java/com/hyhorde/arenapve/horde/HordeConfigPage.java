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
import java.util.UUID;

public final class HordeConfigPage
extends CustomUIPage {
    private static final String LAYOUT = "Pages/HordeConfigPage.ui";
    private static final String TAB_GENERAL = "general";
    private static final String TAB_HORDE = "horde";
    private static final String TAB_PLAYERS = "players";
    private static final String TAB_SOUNDS = "sounds";
    private static final String TAB_REWARDS = "rewards";
    private static final int MAX_AUDIENCE_ROWS = 10;
    private final HordeService hordeService;
    private String activeTab;

    private HordeConfigPage(PlayerRef playerRef, HordeService hordeService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.hordeService = hordeService;
        this.activeTab = TAB_GENERAL;
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
        List<String> roundStartSoundOptions = this.hordeService.getRoundStartSoundOptions();
        List<String> roundVictorySoundOptions = this.hordeService.getRoundVictorySoundOptions();
        String rewardCategory = HordeConfigPage.firstNonEmpty(config.rewardCategory, this.hordeService.getRewardCategory());
        List<String> rewardItemSuggestions = this.hordeService.getRewardItemSuggestions(rewardCategory);
        String rewardHint = HordeConfigPage.buildRewardItemsHint(rewardCategoryOptions, rewardCategory, rewardItemSuggestions, config.rewardItemId, english);
        String tab = HordeConfigPage.normalizeTab(this.activeTab);
        this.activeTab = tab;
        EntityStore entityStore = (EntityStore)store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        List<HordeService.AudiencePlayerSnapshot> audienceRows = world == null ? List.of() : this.hordeService.getArenaAudiencePlayers(world);
        commandBuilder.append(LAYOUT)
                .set("#SpawnX.Value", HordeConfigPage.formatDouble(config.spawnX))
                .set("#SpawnY.Value", HordeConfigPage.formatDouble(config.spawnY))
                .set("#SpawnZ.Value", HordeConfigPage.formatDouble(config.spawnZ))
                .set("#MinRadius.Value", HordeConfigPage.formatDouble(config.minSpawnRadius))
                .set("#MaxRadius.Value", HordeConfigPage.formatDouble(config.maxSpawnRadius))
                .set("#ArenaJoinRadius.Value", HordeConfigPage.formatDouble(config.arenaJoinRadius))
                .set("#Rounds.Value", Integer.toString(config.rounds))
                .set("#BaseEnemies.Value", Integer.toString(config.baseEnemiesPerRound))
                .set("#EnemiesPerRound.Value", Integer.toString(config.enemiesPerRoundIncrement))
                .set("#WaveDelay.Value", Integer.toString(config.waveDelaySeconds))
                .set("#PlayerMultiplier.Value", Integer.toString(config.playerMultiplier))
                .set("#EnemyType.Value", config.enemyType == null ? "undead" : config.enemyType)
                .set("#Language.Value", HordeService.getLanguageDisplay(config.language))
                .set("#RewardEveryRounds.Value", Integer.toString(config.rewardEveryRounds))
                .set("#RewardCategory.Value", rewardCategory)
                .set("#RewardItemId.Value", config.rewardItemId == null ? "" : config.rewardItemId)
                .set("#RewardItemQuantity.Value", Integer.toString(config.rewardItemQuantity))
                .set("#FinalBossEnabled.Value", HordeConfigPage.finalBossDisplay(config.finalBossEnabled, english))
                .set("#RoundStartSoundId.Value", this.hordeService.getRoundStartSoundSelection())
                .set("#RoundVictorySoundId.Value", this.hordeService.getRoundVictorySoundSelection())
                .set("#EnemyLevelMin.Value", Integer.toString(config.enemyLevelMin))
                .set("#EnemyLevelMax.Value", Integer.toString(config.enemyLevelMax))
                .set("#AudienceInfoLabel.Text", HordeConfigPage.buildAudienceInfo(config.arenaJoinRadius, audienceRows.size(), english))
                .set("#PlayersCountValue.Text", Integer.toString(audienceRows.size()))
                .set("#PlayersListHint.Text", HordeConfigPage.buildAudienceRowsHint(audienceRows.size(), english))
                .set("#AudiencePlayersEmptyLabel.Text", audienceRows.isEmpty() ? (english ? "No players detected in the current arena radius." : "No hay jugadores detectados en el radio actual de arena.") : "")
                .set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config, english))
                .set("#StatusLabel.Text", this.hordeService.getStatusLine())
                .set("#RoleHelpLabel.Text", HordeConfigPage.buildEnemyTypesHint(enemyTypeOptions, config.enemyType, english))
                .set("#RoundSoundHelpLabel.Text", HordeConfigPage.buildRoundSoundHint(roundStartSoundOptions, this.hordeService.getRoundStartSoundSelection(), roundVictorySoundOptions, this.hordeService.getRoundVictorySoundSelection(), english))
                .set("#RewardCommandsHelpLabel.Text", rewardHint)
                .set("#ReloadModButton.Visible", true)
                .set("#StartButton.Visible", !active)
                .set("#StopButton.Visible", active)
                .set("#SkipRoundButton.Visible", active);
        this.setLocalizedTexts(commandBuilder, english, tab);
        this.applyTabVisibility(commandBuilder, tab);
        this.populateAudienceRows(commandBuilder, eventBuilder, audienceRows, english);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"action", (String)"close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabGeneralButton", EventData.of((String)"action", (String)"tab_general"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabHordeButton", EventData.of((String)"action", (String)"tab_horde"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabPlayersButton", EventData.of((String)"action", (String)"tab_players"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabSoundsButton", EventData.of((String)"action", (String)"tab_sounds"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabRewardsButton", EventData.of((String)"action", (String)"tab_rewards"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SetSpawnButton", EventData.of((String)"action", (String)"set_spawn_here"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RolesButton", EventData.of((String)"action", (String)"enemy_types"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyTypePrevButton", this.buildConfigSnapshotEvent("enemy_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyTypeNextButton", this.buildConfigSnapshotEvent("enemy_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#LanguagePrevButton", this.buildConfigSnapshotEvent("language_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#LanguageNextButton", this.buildConfigSnapshotEvent("language_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCategoryPrevButton", this.buildConfigSnapshotEvent("reward_category_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCategoryNextButton", this.buildConfigSnapshotEvent("reward_category_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardItemPrevButton", this.buildConfigSnapshotEvent("reward_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardItemNextButton", this.buildConfigSnapshotEvent("reward_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#FinalBossPrevButton", this.buildConfigSnapshotEvent("final_boss_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#FinalBossNextButton", this.buildConfigSnapshotEvent("final_boss_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RoundStartSoundPrevButton", this.buildConfigSnapshotEvent("round_start_sound_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RoundStartSoundNextButton", this.buildConfigSnapshotEvent("round_start_sound_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RoundVictorySoundPrevButton", this.buildConfigSnapshotEvent("round_victory_sound_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RoundVictorySoundNextButton", this.buildConfigSnapshotEvent("round_victory_sound_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardTypesButton", EventData.of((String)"action", (String)"reward_types"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersRefreshButton", EventData.of((String)"action", (String)"refresh_players"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ReloadModButton", EventData.of((String)"action", (String)"reload_config"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", this.buildConfigSnapshotEvent("save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StartButton", this.buildConfigSnapshotEvent("start"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StopButton", EventData.of((String)"action", (String)"stop"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SkipRoundButton", EventData.of((String)"action", (String)"skip_round"));
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
                case "tab_general": {
                    this.activeTab = TAB_GENERAL;
                    break;
                }
                case "tab_horde": {
                    this.activeTab = TAB_HORDE;
                    break;
                }
                case "tab_players": {
                    this.activeTab = TAB_PLAYERS;
                    break;
                }
                case "tab_sounds": {
                    this.activeTab = TAB_SOUNDS;
                    break;
                }
                case "tab_rewards": {
                    this.activeTab = TAB_REWARDS;
                    break;
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
                case "round_start_sound_prev": {
                    result = this.cycleRoundStartSound(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "round_start_sound_next": {
                    result = this.cycleRoundStartSound(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "round_victory_sound_prev": {
                    result = this.cycleRoundVictorySound(HordeConfigPage.extractConfigValues(payload), world, -1);
                    break;
                }
                case "round_victory_sound_next": {
                    result = this.cycleRoundVictorySound(HordeConfigPage.extractConfigValues(payload), world, 1);
                    break;
                }
                case "reward_types": {
                    this.sendRewardTypesPreview();
                    break;
                }
                case "refresh_players": {
                    break;
                }
                case "reload_config":
                case "reload_mod": {
                    result = this.hordeService.reloadConfigFromDisk();
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
                    result = this.handleAudienceAction(action, world, english);
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
        if (action.startsWith("audience_set:")) {
            return true;
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
            case "round_start_sound_prev":
            case "round_start_sound_next":
            case "round_victory_sound_prev":
            case "round_victory_sound_next":
            case "save":
            case "skip_round":
            case "start": {
                return true;
            }
        }
        return false;
    }

    private HordeService.OperationResult handleAudienceAction(String action, World world, boolean english) {
        if (action == null || !action.startsWith("audience_set:")) {
            return HordeService.OperationResult.fail(english ? "Unknown UI action: " + action : "Accion de UI desconocida: " + action);
        }
        String[] parts = action.split(":", 3);
        if (parts.length != 3) {
            return HordeService.OperationResult.fail(english ? "Invalid audience action payload." : "Accion de audiencia invalida.");
        }
        String mode = parts[1];
        String rawPlayerId = parts[2];
        UUID playerId;
        try {
            playerId = UUID.fromString(rawPlayerId);
        }
        catch (Exception ignored) {
            return HordeService.OperationResult.fail(english ? "Could not parse selected player UUID." : "No se pudo interpretar el UUID del jugador seleccionado.");
        }
        return this.hordeService.setArenaAudienceMode(playerId, mode, world);
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
        return EventData.of((String)"action", (String)action).append("@SpawnX", "#SpawnX.Value").append("@SpawnY", "#SpawnY.Value").append("@SpawnZ", "#SpawnZ.Value").append("@MinRadius", "#MinRadius.Value").append("@MaxRadius", "#MaxRadius.Value").append("@ArenaJoinRadius", "#ArenaJoinRadius.Value").append("@Rounds", "#Rounds.Value").append("@BaseEnemies", "#BaseEnemies.Value").append("@EnemiesPerRound", "#EnemiesPerRound.Value").append("@WaveDelay", "#WaveDelay.Value").append("@PlayerMultiplier", "#PlayerMultiplier.Value").append("@EnemyType", "#EnemyType.Value").append("@Language", "#Language.Value").append("@RewardEveryRounds", "#RewardEveryRounds.Value").append("@RewardCategory", "#RewardCategory.Value").append("@RewardItemId", "#RewardItemId.Value").append("@RewardItemQuantity", "#RewardItemQuantity.Value").append("@FinalBossEnabled", "#FinalBossEnabled.Value").append("@RoundStartSoundId", "#RoundStartSoundId.Value").append("@RoundVictorySoundId", "#RoundVictorySoundId.Value");
    }

    private void populateAudienceRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeService.AudiencePlayerSnapshot> rows, boolean english) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        int rowIndex = 0;
        for (HordeService.AudiencePlayerSnapshot row : rows) {
            if (row == null || row.playerId == null || rowIndex >= MAX_AUDIENCE_ROWS) continue;
            commandBuilder.append("#AudiencePlayersRows", "Pages/HordeAudiencePlayerRow.ui");
            String mode = HordeConfigPage.normalizeAudienceMode(row.mode);
            boolean playerMode = "player".equals(mode);
            boolean spectatorMode = "spectator".equals(mode);
            boolean exitMode = "exit".equals(mode);
            commandBuilder.set("#AudiencePlayersRows[" + rowIndex + "] #RowName.Text", HordeConfigPage.compactName(row.username, 30))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #RowMode.Text", HordeConfigPage.audienceModeDisplay(mode, english))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetPlayerButton.Text", playerMode ? (english ? "Player *" : "Jugador *") : (english ? "Player" : "Jugador"))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetSpectatorButton.Text", spectatorMode ? (english ? "Spectator *" : "Espectador *") : (english ? "Spectator" : "Espectador"))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetExitButton.Text", exitMode ? (english ? "Exit *" : "Salir *") : (english ? "Exit" : "Salir"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetPlayerButton", EventData.of((String)"action", (String)HordeConfigPage.buildAudienceAction("player", row.playerId)))
                    .addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetSpectatorButton", EventData.of((String)"action", (String)HordeConfigPage.buildAudienceAction("spectator", row.playerId)))
                    .addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetExitButton", EventData.of((String)"action", (String)HordeConfigPage.buildAudienceAction("exit", row.playerId)));
            ++rowIndex;
        }
        int hiddenRows = Math.max(0, rows.size() - MAX_AUDIENCE_ROWS);
        if (hiddenRows > 0) {
            commandBuilder.set("#PlayersListHint.Text", english ? "Showing first " + MAX_AUDIENCE_ROWS + " players (" + hiddenRows + " more not shown)." : "Mostrando primeros " + MAX_AUDIENCE_ROWS + " jugadores (" + hiddenRows + " mas sin mostrar).");
        }
    }

    private static String buildAudienceAction(String mode, UUID playerId) {
        return "audience_set:" + mode + ":" + playerId;
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

    private HordeService.OperationResult cycleRoundStartSound(Map<String, String> values, World world, int offset) {
        List<String> options = this.hordeService.getRoundStartSoundOptions();
        boolean english = this.isEnglish();
        if (options.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No round start sounds available." : "No hay sonidos de inicio de ronda disponibles.");
        }
        String current = HordeConfigPage.firstNonEmpty(values.get("roundStartSoundId"), this.hordeService.getRoundStartSoundSelection()).trim();
        int currentIndex = -1;
        for (int i = 0; i < options.size(); ++i) {
            if (!options.get(i).equalsIgnoreCase(current)) continue;
            currentIndex = i;
            break;
        }
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, options.size());
        values.put("roundStartSoundId", options.get(nextIndex));
        return this.hordeService.applyUiConfig(values, world);
    }

    private HordeService.OperationResult cycleRoundVictorySound(Map<String, String> values, World world, int offset) {
        List<String> options = this.hordeService.getRoundVictorySoundOptions();
        boolean english = this.isEnglish();
        if (options.isEmpty()) {
            return HordeService.OperationResult.fail(english ? "No round victory sounds available." : "No hay sonidos de victoria de ronda disponibles.");
        }
        String current = HordeConfigPage.firstNonEmpty(values.get("roundVictorySoundId"), this.hordeService.getRoundVictorySoundSelection()).trim();
        int currentIndex = -1;
        for (int i = 0; i < options.size(); ++i) {
            if (!options.get(i).equalsIgnoreCase(current)) continue;
            currentIndex = i;
            break;
        }
        if (currentIndex < 0) {
            currentIndex = offset > 0 ? -1 : 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, options.size());
        values.put("roundVictorySoundId", options.get(nextIndex));
        return this.hordeService.applyUiConfig(values, world);
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
        values.put("arenaJoinRadius", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "arenaJoinRadius"), HordeConfigPage.read(payload, "@ArenaJoinRadius"), HordeConfigPage.read(payload, "ArenaJoinRadius")));
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
        values.put("roundStartSoundId", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "roundStartSoundId"), HordeConfigPage.read(payload, "@RoundStartSoundId"), HordeConfigPage.read(payload, "RoundStartSoundId")));
        values.put("roundVictorySoundId", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "roundVictorySoundId"), HordeConfigPage.read(payload, "@RoundVictorySoundId"), HordeConfigPage.read(payload, "RoundVictorySoundId")));
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

    private static String buildRoundSoundHint(List<String> roundStartOptions, String selectedRoundStart, List<String> roundVictoryOptions, String selectedRoundVictory, boolean english) {
        String startCurrent = HordeConfigPage.firstNonEmpty(selectedRoundStart, roundStartOptions == null || roundStartOptions.isEmpty() ? "auto" : roundStartOptions.get(0));
        String victoryCurrent = HordeConfigPage.firstNonEmpty(selectedRoundVictory, roundVictoryOptions == null || roundVictoryOptions.isEmpty() ? "auto" : roundVictoryOptions.get(0));
        List<String> startPreview = roundStartOptions == null || roundStartOptions.isEmpty() ? List.of("auto", "none") : roundStartOptions.subList(0, Math.min(4, roundStartOptions.size()));
        List<String> victoryPreview = roundVictoryOptions == null || roundVictoryOptions.isEmpty() ? List.of("auto", "none") : roundVictoryOptions.subList(0, Math.min(4, roundVictoryOptions.size()));
        if (english) {
            return "Use < > to change round sounds | Start: " + startCurrent + " | Victory: " + victoryCurrent + " | Start options: " + String.join(", ", startPreview) + " | Victory options: " + String.join(", ", victoryPreview);
        }
        return "Usa < > para cambiar sonidos de ronda | Inicio: " + startCurrent + " | Victoria: " + victoryCurrent + " | Opciones inicio: " + String.join(", ", startPreview) + " | Opciones victoria: " + String.join(", ", victoryPreview);
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

    private void setLocalizedTexts(UICommandBuilder commandBuilder, boolean english, String tab) {
        boolean generalTab = TAB_GENERAL.equals(tab);
        boolean hordeTab = TAB_HORDE.equals(tab);
        boolean playersTab = TAB_PLAYERS.equals(tab);
        boolean soundsTab = TAB_SOUNDS.equals(tab);
        boolean rewardsTab = TAB_REWARDS.equals(tab);
        commandBuilder.set("#TitleLabel.Text", english ? "Horde PVE Config" : "Horda PVE Config")
                .set("#SubTitleLabel.Text", english ? "Split setup by tabs: general, horde, players, sounds and rewards" : "Configuracion en pestanas: general, horda, jugadores, sonidos y recompensas")
                .set("#TabGeneralButton.Text", english ? (generalTab ? "General *" : "General") : (generalTab ? "General *" : "General"))
                .set("#TabHordeButton.Text", english ? (hordeTab ? "Horde *" : "Horde") : (hordeTab ? "Horda *" : "Horda"))
                .set("#TabPlayersButton.Text", english ? (playersTab ? "Players *" : "Players") : (playersTab ? "Jugadores *" : "Jugadores"))
                .set("#TabSoundsButton.Text", english ? (soundsTab ? "Sounds *" : "Sounds") : (soundsTab ? "Sonidos *" : "Sonidos"))
                .set("#TabRewardsButton.Text", english ? (rewardsTab ? "Rewards *" : "Rewards") : (rewardsTab ? "Recompensas *" : "Recompensas"))
                .set("#TabHintLabel.Text", english ? "Tabs only change the editor view. Save applies the full config." : "Las pestanas solo cambian la vista. Guardar aplica toda la configuracion.")
                .set("#SpawnLabel.Text", english ? "Center (X Y Z)" : "Centro (X Y Z)")
                .set("#SetSpawnButton.Text", english ? "Use my current position" : "Usar mi posicion actual")
                .set("#RadiusLabel.Text", english ? "Min / max radius" : "Radio min / max")
                .set("#ArenaJoinRadiusLabel.Text", english ? "Players area radius" : "Radio de jugadores")
                .set("#PlayersListTitle.Text", english ? "Players inside current area" : "Jugadores dentro del area actual")
                .set("#PlayersCountLabel.Text", english ? "Detected" : "Detectados")
                .set("#PlayersHeaderName.Text", english ? "Player" : "Jugador")
                .set("#PlayersHeaderMode.Text", english ? "Mode" : "Modo")
                .set("#PlayersRefreshButton.Text", english ? "Refresh list" : "Actualizar lista")
                .set("#AudienceHelpLabel.Text", english ? "Changes apply to next start. If horde is active, they are applied to current lock immediately." : "Cambios aplican al siguiente inicio. Si la horda esta activa, se aplican al bloqueo actual.")
                .set("#RoundLabel.Text", english ? "Rounds" : "Rondas")
                .set("#BaseEnemiesLabel.Text", english ? "Base / round" : "Base ronda")
                .set("#EnemiesPerRoundLabel.Text", english ? "Inc. per round" : "Inc. por ronda")
                .set("#WaveDelayLabel.Text", english ? "Delay (s)" : "Espera (s)")
                .set("#PlayerMultiplierLabel.Text", english ? "Players (x)" : "Jugadores (x)")
                .set("#RoleLabel.Text", english ? "Horde category" : "Categoria de horda")
                .set("#RolesButton.Text", english ? "View categories" : "Ver categorias")
                .set("#LanguageLabel.Text", english ? "Interface language" : "Idioma interfaz")
                .set("#EnemyLevelRangeLabel.Text", english ? "Enemy level range" : "Rango nivel enemigos")
                .set("#EnemyLevelWipLabel.Text", english ? "WIP: this system is temporarily disabled." : "WIP: este sistema esta desactivado temporalmente.")
                .set("#RewardEveryRoundsLabel.Text", english ? "Reward every round(s)" : "Recompensa por ronda(s)")
                .set("#RewardCategoryLabel.Text", english ? "Reward category" : "Categoria recompensa")
                .set("#RewardCommandsLabel.Text", english ? "Reward item" : "Item recompensa")
                .set("#RewardTypesButton.Text", english ? "View loot" : "Ver loot")
                .set("#RewardItemQuantityLabel.Text", english ? "Qty." : "Cant.")
                .set("#FinalBossLabel.Text", english ? "Final boss" : "Boss final")
                .set("#RoundStartSoundLabel.Text", english ? "Round start sound" : "Sonido inicio ronda")
                .set("#RoundVictorySoundLabel.Text", english ? "Round victory sound" : "Sonido victoria ronda")
                .set("#StatusTitleLabel.Text", english ? "Current status" : "Estado actual")
                .set("#ReloadModButton.Text", english ? "Reload config" : "Recargar config")
                .set("#SaveButton.Text", english ? "Save config" : "Guardar config")
                .set("#StartButton.Text", english ? "Start horde" : "Iniciar horda")
                .set("#StopButton.Text", english ? "Stop horde" : "Detener horda")
                .set("#SkipRoundButton.Text", english ? "Skip round" : "Pasar ronda")
                .set("#CloseButton.Text", english ? "Close" : "Cerrar");
    }

    private void applyTabVisibility(UICommandBuilder commandBuilder, String tab) {
        boolean generalTab = TAB_GENERAL.equals(tab);
        boolean hordeTab = TAB_HORDE.equals(tab);
        boolean playersTab = TAB_PLAYERS.equals(tab);
        boolean soundsTab = TAB_SOUNDS.equals(tab);
        boolean rewardsTab = TAB_REWARDS.equals(tab);

        this.setVisible(commandBuilder, generalTab, "#SpawnStateLabel", "#SpawnLabel", "#SpawnX", "#SpawnY", "#SpawnZ", "#SetSpawnButton", "#RadiusLabel", "#MinRadius", "#MaxRadius", "#LanguageLabel", "#LanguagePrevButton", "#Language", "#LanguageNextButton");
        this.setVisible(commandBuilder, hordeTab, "#RoundLabel", "#Rounds", "#BaseEnemiesLabel", "#BaseEnemies", "#EnemiesPerRoundLabel", "#EnemiesPerRound", "#WaveDelayLabel", "#WaveDelay", "#PlayerMultiplierLabel", "#PlayerMultiplier", "#RoleLabel", "#EnemyTypePrevButton", "#EnemyType", "#EnemyTypeNextButton", "#RolesButton", "#RoleHelpLabel", "#FinalBossLabel", "#FinalBossPrevButton", "#FinalBossEnabled", "#FinalBossNextButton", "#EnemyLevelRangeLabel", "#EnemyLevelWipLabel");
        this.setVisible(commandBuilder, playersTab, "#ArenaJoinRadiusLabel", "#ArenaJoinRadius", "#AudienceInfoLabel", "#PlayersListTitle", "#PlayersCountLabel", "#PlayersCountValue", "#PlayersListHint", "#PlayersRefreshButton", "#PlayersHeaderName", "#PlayersHeaderMode", "#AudiencePlayersRows", "#AudiencePlayersEmptyLabel", "#AudienceHelpLabel");
        this.setVisible(commandBuilder, soundsTab, "#RoundStartSoundLabel", "#RoundStartSoundPrevButton", "#RoundStartSoundId", "#RoundStartSoundNextButton", "#RoundVictorySoundLabel", "#RoundVictorySoundPrevButton", "#RoundVictorySoundId", "#RoundVictorySoundNextButton", "#RoundSoundHelpLabel");
        this.setVisible(commandBuilder, rewardsTab, "#RewardEveryRoundsLabel", "#RewardEveryRounds", "#RewardCategoryLabel", "#RewardCategoryPrevButton", "#RewardCategory", "#RewardCategoryNextButton", "#RewardTypesButton", "#RewardCommandsLabel", "#RewardItemPrevButton", "#RewardItemId", "#RewardItemNextButton", "#RewardItemQuantityLabel", "#RewardItemQuantity", "#RewardCommandsHelpLabel");
        this.setVisible(commandBuilder, false, "#EnemyLevelMin", "#EnemyLevelRangeSeparator", "#EnemyLevelMax");
    }

    private void setVisible(UICommandBuilder commandBuilder, boolean visible, String ... elementIds) {
        if (elementIds == null) {
            return;
        }
        for (String elementId : elementIds) {
            if (elementId == null || elementId.isBlank()) continue;
            commandBuilder.set(elementId + ".Visible", visible);
        }
    }

    private static String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return TAB_GENERAL;
        }
        String normalized = tab.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case TAB_GENERAL:
            case TAB_HORDE:
            case TAB_PLAYERS:
            case TAB_SOUNDS:
            case TAB_REWARDS: {
                return normalized;
            }
        }
        return TAB_GENERAL;
    }

    private static String audienceModeDisplay(String mode, boolean english) {
        String normalized = HordeConfigPage.normalizeAudienceMode(mode);
        if ("spectator".equals(normalized)) {
            return english ? "Spectator" : "Espectador";
        }
        if ("exit".equals(normalized)) {
            return english ? "Exit area" : "Salir del area";
        }
        return english ? "Player" : "Jugador";
    }

    private static String normalizeAudienceMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "player";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u');
        normalized = normalized.replace('_', '-').replace(' ', '-');
        if ("spectator".equals(normalized) || "espectador".equals(normalized) || "observer".equals(normalized) || "observador".equals(normalized)) {
            return "spectator";
        }
        if ("exit".equals(normalized) || "salir".equals(normalized) || "out".equals(normalized) || "leave".equals(normalized)) {
            return "exit";
        }
        return "player";
    }

    private static String buildAudienceInfo(double arenaJoinRadius, int playersInArea, boolean english) {
        if (english) {
            return String.format(Locale.ROOT, "Current arena radius: %.2f blocks | Players inside area: %d", arenaJoinRadius, playersInArea);
        }
        return String.format(Locale.ROOT, "Radio actual de arena: %.2f bloques | Jugadores dentro del area: %d", arenaJoinRadius, playersInArea);
    }

    private static String buildAudienceRowsHint(int playersInArea, boolean english) {
        if (english) {
            return playersInArea > 0 ? "Use each row to set Player, Spectator or Exit mode." : "Move players inside the arena radius to manage them here.";
        }
        return playersInArea > 0 ? "Usa cada fila para poner modo Jugador, Espectador o Salir." : "Mueve jugadores dentro del radio de arena para gestionarlos aqui.";
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

    private static String compactName(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Jugador";
        }
        String safe = value.trim();
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, Math.max(0, maxLength - 1)) + ".";
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



