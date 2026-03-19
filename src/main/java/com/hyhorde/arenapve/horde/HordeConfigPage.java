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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.ArrayList;
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
    private static final String TAB_BOSSES = "bosses";
    private static final String TAB_ARENAS = "arenas";
    private static final String TAB_HELP = "help";
    private static final int MAX_AUDIENCE_ROWS = Integer.MAX_VALUE;
    private static final int MAX_BOSS_ROWS = 4;
    private static final int MAX_ARENA_ROWS = 10;
    private static final List<String> BOSS_SPAWN_TRIGGER_OPTIONS = List.of("before_boss", "on_spawn", "after_spawn_seconds", "since_last_wave", "boss_hp_percent");
    private static final UiFieldBinding[] SNAPSHOT_FIELDS = new UiFieldBinding[]{
            new UiFieldBinding("spawnX", "SpawnX", "#SpawnX.Value"),
            new UiFieldBinding("spawnY", "SpawnY", "#SpawnY.Value"),
            new UiFieldBinding("spawnZ", "SpawnZ", "#SpawnZ.Value"),
            new UiFieldBinding("minRadius", "MinRadius", "#MinRadius.Value"),
            new UiFieldBinding("maxRadius", "MaxRadius", "#MaxRadius.Value"),
            new UiFieldBinding("arenaJoinRadius", "ArenaJoinRadius", "#ArenaJoinRadius.Value"),
            new UiFieldBinding("rounds", "Rounds", "#Rounds.Value"),
            new UiFieldBinding("baseEnemies", "BaseEnemies", "#BaseEnemies.Value"),
            new UiFieldBinding("enemiesPerRound", "EnemiesPerRound", "#EnemiesPerRound.Value"),
            new UiFieldBinding("waveDelay", "WaveDelay", "#WaveDelay.Value"),
            // Known CustomUI runtime bug:
            // setting #RewardEveryRounds.Value from Java can crash with
            // "CustomUI Set command couldn't set value" (see server logs, 2026-03-17).
            // Keep auto-start on dedicated stable controls in General tab.
            new UiFieldBinding("autoStartEnabled", "AutoStartEnabled", "#AutoStartEnabled.Value"),
            new UiFieldBinding("autoStartIntervalMinutes", "AutoStartIntervalMinutes", "#AutoStartInterval.Value"),
            new UiFieldBinding("selectedArenaId", "GeneralArenaId", "#GeneralArenaId.Value"),
            new UiFieldBinding("enemyType", "EnemyType", "#EnemyType.Value", "role", "@Role", "Role"),
            new UiFieldBinding("language", "Language", "#Language.Value"),
            new UiFieldBinding("rewardCategory", "RewardCategory", "#RewardCategory.Value"),
            new UiFieldBinding("rewardItemId", "RewardItemId", "#RewardItemId.Value"),
            new UiFieldBinding("rewardItemQuantity", "RewardItemQuantity", "#RewardItemQuantity.Value"),
            new UiFieldBinding("finalBossEnabled", "FinalBossEnabled", "#FinalBossEnabled.Value"),
            new UiFieldBinding("roundStartSoundId", "RoundStartSoundId", "#RoundStartSoundId.Value"),
            new UiFieldBinding("roundStartVolume", "RoundStartVolume", "#RoundStartVolume.Value"),
            new UiFieldBinding("roundVictorySoundId", "RoundVictorySoundId", "#RoundVictorySoundId.Value"),
            new UiFieldBinding("roundVictoryVolume", "RoundVictoryVolume", "#RoundVictoryVolume.Value"),
            new UiFieldBinding("bossCreateId", "BossCreateId", "#BossCreateId.Value"),
            new UiFieldBinding("bossSelected", "BossSelected", "#BossSelected.Value"),
            new UiFieldBinding("bossEditName", "BossEditName", "#BossEditName.Value"),
            new UiFieldBinding("bossEditNpcId", "BossEditNpcId", "#BossEditNpcId.Value"),
            new UiFieldBinding("bossEditTier", "BossEditTier", "#BossEditTier.Value"),
            new UiFieldBinding("bossEditAmount", "BossEditAmount", "#BossEditAmount.Value"),
            new UiFieldBinding("bossEditLevelOverride", "BossEditLevelOverride", "#BossEditLevelOverride.Value"),
            new UiFieldBinding("bossEditLootRadius", "BossEditLootRadius", "#BossEditLootRadius.Value"),
            new UiFieldBinding("bossSpawnTrigger", "BossSpawnTrigger", "#BossSpawnTrigger.Value"),
            new UiFieldBinding("bossSpawnTriggerValue", "BossSpawnTriggerValue", "#BossSpawnTriggerValue.Value"),
            new UiFieldBinding("bossWaveRandomLocations", "BossWaveRandomLocations", "#BossWaveRandomLocations.Value"),
            new UiFieldBinding("bossWaveRandomRadius", "BossWaveRandomRadius", "#BossWaveRandomRadius.Value"),
            new UiFieldBinding("bossTimedProximityEnabled", "BossTimedProximityEnabled", "#BossTimedProximityEnabled.Value"),
            new UiFieldBinding("bossTimedProximityArena", "BossTimedProximityArena", "#BossTimedProximityArena.Value"),
            new UiFieldBinding("bossTimedProximityRadius", "BossTimedProximityRadius", "#BossTimedProximityRadius.Value"),
            new UiFieldBinding("bossTimedProximityCooldown", "BossTimedProximityCooldown", "#BossTimedProximityCooldown.Value"),
            new UiFieldBinding("arenaSelected", "ArenaSelected", "#ArenaSelected.Value"),
            new UiFieldBinding("arenaEditId", "ArenaEditId", "#ArenaEditId.Value"),
            new UiFieldBinding("arenaEditX", "ArenaEditX", "#ArenaEditX.Value"),
            new UiFieldBinding("arenaEditY", "ArenaEditY", "#ArenaEditY.Value"),
            new UiFieldBinding("arenaEditZ", "ArenaEditZ", "#ArenaEditZ.Value")
    };
    private final HordeService hordeService;
    private final Map<String, String> draftValues;
    private String activeTab;
    private int bossPage;
    private int arenaPage;
    private String bossStatusText;
    private String arenaStatusText;

    private HordeConfigPage(PlayerRef playerRef, HordeService hordeService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.hordeService = hordeService;
        this.draftValues = new HashMap<String, String>();
        this.activeTab = TAB_GENERAL;
        this.bossPage = 0;
        this.arenaPage = 0;
        this.bossStatusText = "";
        this.arenaStatusText = "";
    }

    public static void open(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, HordeService hordeService) {
        HordeConfigPage page = new HordeConfigPage(playerRef, hordeService);
        player.getPageManager().openCustomPage(playerEntityRef, store, (CustomUIPage)page);
    }

    public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
        HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
        this.ensureDraftDefaults(config);
        String language = HordeService.normalizeLanguage(this.getDraftValue("language", config.language));
        boolean english = HordeService.isEnglishLanguage(language);
        boolean active = this.hordeService.isActive();
        List<String> enemyTypeOptions = this.hordeService.getEnemyTypeOptionsForCurrentRoles();
        List<String> rewardCategoryOptions = this.hordeService.getRewardCategoryOptions();
        List<String> languageOptions = this.hordeService.getLanguageOptions();
        List<String> roundStartSoundOptions = this.hordeService.getRoundStartSoundOptions();
        List<String> roundVictorySoundOptions = this.hordeService.getRoundVictorySoundOptions();
        String rewardCategory = HordeConfigPage.normalizeRewardCategoryInput(this.getDraftValue("rewardCategory", HordeConfigPage.firstNonEmpty(config.rewardCategory, this.hordeService.getRewardCategory())));
        List<String> rewardItemSuggestions = this.hordeService.getRewardItemSuggestions(rewardCategory);
        String enemyTypeValue = HordeConfigPage.normalizeEnemyTypeInput(this.getDraftValue("enemyType", config.enemyType == null ? "undead" : config.enemyType));
        String rewardItemIdValue = this.getDraftValue("rewardItemId", config.rewardItemId == null ? "" : config.rewardItemId);
        String roundStartSoundValue = this.getDraftValue("roundStartSoundId", this.hordeService.getRoundStartSoundSelection());
        String roundVictorySoundValue = this.getDraftValue("roundVictorySoundId", this.hordeService.getRoundVictorySoundSelection());
        double minRadiusValue = HordeConfigPage.clamp(this.getDraftDouble("minRadius", config.minSpawnRadius), 1.0, 128.0);
        double maxRadiusValue = HordeConfigPage.clamp(this.getDraftDouble("maxRadius", config.maxSpawnRadius), 1.0, 128.0);
        double arenaJoinRadiusValue = HordeConfigPage.clamp(this.getDraftDouble("arenaJoinRadius", config.arenaJoinRadius), 4.0, 512.0);
        int roundsValue = HordeConfigPage.clamp(this.getDraftInt("rounds", config.rounds), 1, 200);
        int baseEnemiesValue = HordeConfigPage.clamp(this.getDraftInt("baseEnemies", config.baseEnemiesPerRound), 1, 400);
        int enemiesPerRoundValue = HordeConfigPage.clamp(this.getDraftInt("enemiesPerRound", config.enemiesPerRoundIncrement), 0, 400);
        int waveDelayValue = HordeConfigPage.clamp(this.getDraftInt("waveDelay", config.waveDelaySeconds), 0, 300);
        boolean autoStartEnabledValue = this.getDraftBoolean("autoStartEnabled", config.autoStartIntervalMinutes > 0);
        int autoStartIntervalDefaultValue = config.autoStartIntervalMinutes > 0 ? config.autoStartIntervalMinutes : 10;
        int autoStartIntervalMinutesValue = HordeConfigPage.clamp(this.getDraftInt("autoStartIntervalMinutes", autoStartIntervalDefaultValue), 1, 1440);
        int rewardItemQuantityValue = HordeConfigPage.clamp(this.getDraftInt("rewardItemQuantity", config.rewardItemQuantity), 1, 100);
        double roundStartVolumeValue = HordeConfigPage.clamp(this.getDraftDouble("roundStartVolume", HordeConfigPage.toUiVolumePercent(config.roundStartVolume)), 0.0, 100.0);
        double roundVictoryVolumeValue = HordeConfigPage.clamp(this.getDraftDouble("roundVictoryVolume", HordeConfigPage.toUiVolumePercent(config.roundVictoryVolume)), 0.0, 100.0);
        boolean finalBossEnabledValue = this.getDraftBoolean("finalBossEnabled", config.finalBossEnabled);
        List<DropdownEntryInfo> enemyTypeEntries = HordeConfigPage.buildEnemyTypeEntries(enemyTypeOptions, enemyTypeValue, language, english);
        List<DropdownEntryInfo> rewardCategoryEntries = HordeConfigPage.buildRewardCategoryEntries(rewardCategoryOptions, rewardCategory, language, english);
        List<DropdownEntryInfo> rewardItemEntries = HordeConfigPage.buildDropdownEntries(rewardItemSuggestions, rewardItemIdValue);
        List<DropdownEntryInfo> languageEntries = HordeConfigPage.buildLanguageEntries(languageOptions, language);
        List<DropdownEntryInfo> roundStartSoundEntries = HordeConfigPage.buildRoundSoundEntries(roundStartSoundOptions, roundStartSoundValue, language, english);
        List<DropdownEntryInfo> roundVictorySoundEntries = HordeConfigPage.buildRoundSoundEntries(roundVictorySoundOptions, roundVictorySoundValue, language, english);
        String tab = HordeConfigPage.normalizeTab(this.activeTab);
        this.activeTab = tab;
        EntityStore entityStore = (EntityStore)store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        List<HordeService.AudiencePlayerSnapshot> audienceRows = world == null ? List.of() : this.hordeService.getArenaAudiencePlayers(world);
        List<BossArenaCatalogService.BossDefinitionSnapshot> bossRows = this.hordeService.getBossDefinitionsSnapshot();
        List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows = this.hordeService.getArenaDefinitionsSnapshot();
        this.ensureBossDraftDefaults(bossRows, arenaRows);
        this.ensureArenaDraftDefaults(arenaRows);
        String selectedArenaForHordeValue = HordeConfigPage.firstNonEmpty(this.getDraftValue("selectedArenaId", ""), config.selectedArenaId, HordeConfigPage.firstArenaId(arenaRows));
        if (!selectedArenaForHordeValue.isBlank()) {
            this.draftValues.put("selectedArenaId", selectedArenaForHordeValue);
        }
        selectedArenaForHordeValue = this.getDraftValue("selectedArenaId", "");
        String bossSelectedValue = this.getDraftValue("bossSelected", "");
        String arenaSelectedValue = this.getDraftValue("arenaSelected", "");
        String bossTierValue = this.getDraftValue("bossEditTier", "common");
        String bossSpawnTriggerValue = this.getDraftValue("bossSpawnTrigger", "before_boss");
        String bossTimedProximityArenaValue = this.getDraftValue("bossTimedProximityArena", "");
        List<DropdownEntryInfo> generalArenaEntries = HordeConfigPage.buildDropdownEntries(HordeConfigPage.collectArenaIds(arenaRows), selectedArenaForHordeValue);
        List<DropdownEntryInfo> bossTierEntries = HordeConfigPage.buildDropdownEntries(this.hordeService.getBossTierOptions(), bossTierValue);
        List<DropdownEntryInfo> bossSpawnTriggerEntries = HordeConfigPage.buildBossSpawnTriggerEntries(BOSS_SPAWN_TRIGGER_OPTIONS, bossSpawnTriggerValue, language, english);
        List<DropdownEntryInfo> bossTimedProximityArenaEntries = HordeConfigPage.buildDropdownEntries(HordeConfigPage.collectArenaIds(arenaRows), bossTimedProximityArenaValue);
        this.bossPage = HordeConfigPage.clamp(this.bossPage, 0, HordeConfigPage.maxPageIndex(bossRows == null ? 0 : bossRows.size(), MAX_BOSS_ROWS));
        this.arenaPage = HordeConfigPage.clamp(this.arenaPage, 0, HordeConfigPage.maxPageIndex(arenaRows == null ? 0 : arenaRows.size(), MAX_ARENA_ROWS));
        // IMPORTANT (recurring crash pattern in Hytale CustomUI):
        // Do NOT push SliderNumberField `.Value` from Java on build/rebuild.
        // In this runtime it repeatedly crashes clients/servers with:
        // "Crash - CustomUI Set command couldn't set value. Selector: #<SliderId>.Value"
        // We only read slider values from payload on Save/Start.
        //
        // IMPORTANT (known client crash from logs):
        // We have seen singleplayer joins fail with "Crash - Failed to load CustomUI documents"
        // when the page document is changed aggressively (new controls/layout edits).
        // Also, this runtime may crash with:
        // "Crash - CustomUI Set command couldn't set value. Selector: #RewardEveryRounds.Value"
        // so we avoid writing to #RewardEveryRounds.Value and use dedicated stable controls instead.
        commandBuilder.append(LAYOUT)
                .set("#SpawnX.Value", this.getDraftValue("spawnX", HordeConfigPage.formatDouble(config.spawnX)))
                .set("#SpawnY.Value", this.getDraftValue("spawnY", HordeConfigPage.formatDouble(config.spawnY)))
                .set("#SpawnZ.Value", this.getDraftValue("spawnZ", HordeConfigPage.formatDouble(config.spawnZ)))
                .set("#GeneralArenaId.Value", selectedArenaForHordeValue)
                .set("#GeneralArenaId.Entries", generalArenaEntries)
                .set("#AutoStartEnabled.Value", autoStartEnabledValue)
                .set("#AutoStartInterval.Value", Integer.toString(autoStartIntervalMinutesValue))
                .set("#EnemyType.Value", enemyTypeValue)
                .set("#EnemyType.Entries", enemyTypeEntries)
                .set("#Language.Value", language)
                .set("#Language.Entries", languageEntries)
                .set("#RewardCategory.Value", rewardCategory)
                .set("#RewardCategory.Entries", rewardCategoryEntries)
                .set("#RewardItemId.Value", rewardItemIdValue)
                .set("#RewardItemId.Entries", rewardItemEntries)
                .set("#FinalBossEnabled.Value", finalBossEnabledValue)
                .set("#RoundStartSoundId.Value", roundStartSoundValue)
                .set("#RoundStartSoundId.Entries", roundStartSoundEntries)
                .set("#RoundVictorySoundId.Value", roundVictorySoundValue)
                .set("#RoundVictorySoundId.Entries", roundVictorySoundEntries)
                .set("#EnemyLevelMin.Value", this.getDraftValue("enemyLevelMin", Integer.toString(config.enemyLevelMin)))
                .set("#EnemyLevelMax.Value", this.getDraftValue("enemyLevelMax", Integer.toString(config.enemyLevelMax)))
                .set("#BossCreateId.Value", this.getDraftValue("bossCreateId", ""))
                .set("#BossSelected.Value", bossSelectedValue)
                .set("#BossEditName.Value", this.getDraftValue("bossEditName", bossSelectedValue))
                .set("#BossEditNpcId.Value", this.getDraftValue("bossEditNpcId", ""))
                .set("#BossEditTier.Value", bossTierValue)
                .set("#BossEditTier.Entries", bossTierEntries)
                .set("#BossEditAmount.Value", this.getDraftValue("bossEditAmount", "1"))
                .set("#BossEditLevelOverride.Value", this.getDraftValue("bossEditLevelOverride", "0"))
                .set("#BossEditLootRadius.Value", this.getDraftValue("bossEditLootRadius", "0"))
                .set("#BossSpawnTrigger.Value", bossSpawnTriggerValue)
                .set("#BossSpawnTrigger.Entries", bossSpawnTriggerEntries)
                .set("#BossSpawnTriggerValue.Value", this.getDraftValue("bossSpawnTriggerValue", "0"))
                .set("#BossWaveRandomLocations.Value", this.getDraftBoolean("bossWaveRandomLocations", false))
                .set("#BossWaveRandomRadius.Value", this.getDraftValue("bossWaveRandomRadius", "0"))
                .set("#BossTimedProximityEnabled.Value", this.getDraftBoolean("bossTimedProximityEnabled", false))
                .set("#BossTimedProximityArena.Value", bossTimedProximityArenaValue)
                .set("#BossTimedProximityArena.Entries", bossTimedProximityArenaEntries)
                .set("#BossTimedProximityRadius.Value", this.getDraftValue("bossTimedProximityRadius", "0"))
                .set("#BossTimedProximityCooldown.Value", this.getDraftValue("bossTimedProximityCooldown", "0"))
                .set("#BossStatusLabel.Text", this.bossStatusText == null ? "" : this.bossStatusText)
                .set("#ArenaSelected.Value", arenaSelectedValue)
                .set("#ArenaEditId.Value", this.getDraftValue("arenaEditId", arenaSelectedValue))
                .set("#ArenaEditX.Value", this.getDraftValue("arenaEditX", "0"))
                .set("#ArenaEditY.Value", this.getDraftValue("arenaEditY", "64"))
                .set("#ArenaEditZ.Value", this.getDraftValue("arenaEditZ", "0"))
                .set("#ArenaStatusLabel.Text", this.arenaStatusText == null ? "" : this.arenaStatusText)
                .set("#AudienceInfoLabel.Text", HordeConfigPage.buildAudienceInfo(config.arenaJoinRadius, audienceRows.size(), language))
                .set("#PlayersCountValue.Text", Integer.toString(audienceRows.size()))
                .set("#PlayersListHint.Text", HordeConfigPage.buildAudienceRowsHint(audienceRows.size(), language))
                .set("#AudiencePlayersEmptyLabel.Text", audienceRows.isEmpty() ? HordeConfigPage.t(language, english, "No players detected in the current arena radius.", "No hay jugadores detectados en el radio actual de arena.") : "")
                .set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config, language))
                .set("#ReloadModButton.Visible", true)
                .set("#StartButton.Visible", !active)
                .set("#StopButton.Visible", active)
                .set("#SkipRoundButton.Visible", active);
        this.setLocalizedTexts(commandBuilder, language, english);
        this.applyTabVisibility(commandBuilder, tab);
        if (TAB_PLAYERS.equals(tab)) {
            this.populateAudienceRows(commandBuilder, eventBuilder, audienceRows, language, english);
        }
        if (TAB_BOSSES.equals(tab)) {
            this.populateBossRows(commandBuilder, eventBuilder, bossRows, language, english);
        }
        if (TAB_ARENAS.equals(tab)) {
            this.populateArenaRows(commandBuilder, eventBuilder, arenaRows, language, english);
        }
        // IMPORTANT: Dropdowns like #Language must use ValueChanged.
        // Using Activating on dropdowns triggers client crash: "Failed to apply CustomUI event bindings".
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HordeCloseButton", EventData.of((String)"action", (String)"close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabGeneralButton", this.buildLanguageEvent("tab_general"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabHordeButton", this.buildLanguageEvent("tab_horde"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabPlayersButton", this.buildLanguageEvent("tab_players"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabSoundsButton", this.buildLanguageEvent("tab_sounds"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabRewardsButton", this.buildLanguageEvent("tab_rewards"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabBossesButton", this.buildLanguageEvent("tab_bosses"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabArenasButton", this.buildLanguageEvent("tab_arenas"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabHelpButton", this.buildLanguageEvent("tab_help"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersRefreshButton", this.buildLanguageEvent("refresh_players"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossAddButton", this.buildConfigSnapshotEvent("boss_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossSaveButton", this.buildConfigSnapshotEvent("boss_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossPagePrevButton", this.buildConfigSnapshotEvent("boss_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossPageNextButton", this.buildConfigSnapshotEvent("boss_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaAddButton", this.buildConfigSnapshotEvent("arena_add_from_player"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaSaveButton", this.buildConfigSnapshotEvent("arena_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaUseCurrentPositionButton", this.buildConfigSnapshotEvent("arena_use_current_position"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaPagePrevButton", this.buildConfigSnapshotEvent("arena_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaPageNextButton", this.buildConfigSnapshotEvent("arena_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ReloadModButton", this.buildLanguageEvent("reload_config"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#AutoStartApplyButton", this.buildConfigSnapshotEvent("apply_auto_start"))
                .addEventBinding(CustomUIEventBindingType.ValueChanged, "#Language", this.buildLanguageEvent("set_language"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", this.buildConfigSnapshotEvent("save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StartButton", this.buildConfigSnapshotEvent("start"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StopButton", this.buildActionEventWithLanguage("stop"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SkipRoundButton", this.buildActionEventWithLanguage("skip_round"));
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        JsonObject payload;
        String language = HordeService.normalizeLanguage(this.hordeService.getLanguage());
        boolean english = HordeService.isEnglishLanguage(language);
        try {
            payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateLegacy(language, english ? "Could not parse the UI event payload." : "No se pudo interpretar el evento de la UI.")));
            return;
        }
        try {
            String action = HordeConfigPage.read(payload, "action");
            this.captureDraftFromPayload(payload, action);
            this.applyLanguageFromPayload(payload, action);
            language = HordeService.normalizeLanguage(this.hordeService.getLanguage());
            english = HordeService.isEnglishLanguage(language);
            if ("set_language".equals(action)) {
                this.safeRebuild();
                return;
            }
            EntityStore entityStore = (EntityStore)store.getExternalData();
            World world = entityStore == null ? null : entityStore.getWorld();
            if (world == null && HordeConfigPage.requiresWorld(action)) {
                this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateLegacy(language, english ? "Could not access the active world to process this UI action." : "No se pudo acceder al mundo actual para procesar la accion de UI.")));
                this.safeRebuild();
                return;
            }
            HordeService.OperationResult result = null;
            boolean refreshDraftFromConfig = false;
            boolean tabSwitched = false;
            switch (action) {
                case "close": {
                    this.close();
                    return;
                }
                case "tab_general": {
                    this.activeTab = TAB_GENERAL;
                    tabSwitched = true;
                    break;
                }
                case "tab_horde": {
                    this.activeTab = TAB_HORDE;
                    tabSwitched = true;
                    break;
                }
                case "tab_players": {
                    this.activeTab = TAB_PLAYERS;
                    tabSwitched = true;
                    break;
                }
                case "tab_sounds": {
                    this.activeTab = TAB_SOUNDS;
                    tabSwitched = true;
                    break;
                }
                case "tab_rewards": {
                    this.activeTab = TAB_REWARDS;
                    tabSwitched = true;
                    break;
                }
                case "tab_bosses": {
                    this.activeTab = TAB_BOSSES;
                    tabSwitched = true;
                    break;
                }
                case "tab_arenas": {
                    this.activeTab = TAB_ARENAS;
                    tabSwitched = true;
                    break;
                }
                case "tab_help": {
                    this.activeTab = TAB_HELP;
                    tabSwitched = true;
                    break;
                }
                case "set_spawn_here": {
                    result = this.hordeService.setSpawnFromPlayer(this.playerRef, world);
                    refreshDraftFromConfig = result != null && result.isSuccess();
                    break;
                }
                case "refresh_players": {
                    break;
                }
                case "reload_config":
                case "reload_mod": {
                    result = this.hordeService.reloadConfigFromDisk();
                    refreshDraftFromConfig = result != null && result.isSuccess();
                    break;
                }
                case "apply_auto_start": {
                    result = this.hordeService.applyUiConfig(this.extractConfigValuesForApply(), world);
                    refreshDraftFromConfig = result != null && result.isSuccess();
                    break;
                }
                case "save": {
                    result = this.hordeService.applyUiConfig(this.extractConfigValuesForApply(), world);
                    refreshDraftFromConfig = result != null && result.isSuccess();
                    break;
                }
                case "start": {
                    result = this.hordeService.applyUiConfig(this.extractConfigValuesForApply(), world);
                    if (result.isSuccess()) {
                        refreshDraftFromConfig = true;
                    } else {
                        break;
                    }
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
                    if (action != null && action.startsWith("audience_set:")) {
                        result = this.handleAudienceAction(action, world, english);
                        break;
                    }
                    if (action != null && action.startsWith("boss_")) {
                        result = this.handleBossAction(action, english);
                        break;
                    }
                    if (action != null && action.startsWith("arena_")) {
                        result = this.handleArenaAction(action, world, fallbackWorldName(world), english);
                        break;
                    }
                    result = HordeService.OperationResult.fail(english ? "Unknown UI action: " + action : "Accion de UI desconocida: " + action);
                }
            }
            if (tabSwitched) {
                this.safeRebuild();
                return;
            }
            if (refreshDraftFromConfig) {
                this.resetDraftFromConfig();
            }
            if (result != null) {
                this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateLegacy(language, result.getMessage())));
            }
            if ("save".equals(action)) {
                return;
            }
            if ("start".equals(action) || "stop".equals(action) || "skip_round".equals(action)) {
                this.updateRuntimeControlsOnly(language);
                return;
            }
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateLegacy(language, english ? "Internal error while processing horde UI. Check server logs and try again." : "Error interno al procesar la UI de horda. Revisa logs e intenta de nuevo.")));
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
            case "apply_auto_start":
            case "save":
            case "skip_round":
            case "start":
            case "arena_add_from_player": {
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

    private HordeService.OperationResult handleBossAction(String action, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown boss action." : "Accion de bosses desconocida.");
        }
        if ("boss_page_prev".equals(action)) {
            this.bossPage = Math.max(0, this.bossPage - 1);
            return null;
        }
        if ("boss_page_next".equals(action)) {
            this.bossPage = this.bossPage + 1;
            return null;
        }
        if ("boss_add".equals(action)) {
            String requestedBossId = this.getDraftValue("bossCreateId", "");
            HordeService.OperationResult result = this.hordeService.createBossDraft(requestedBossId);
            if (result != null && result.isSuccess()) {
                this.draftValues.put("bossCreateId", "");
                this.selectBossForEditing(requestedBossId);
            }
            this.bossStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("boss_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveBossDefinitionFromUi(this.extractBossValuesForSave());
            if (result != null && result.isSuccess()) {
                this.selectBossForEditing(this.getDraftValue("bossEditName", this.getDraftValue("bossSelected", "")));
            }
            this.bossStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if (action.startsWith("boss_open:")) {
            String bossId = HordeConfigPage.extractActionArgument(action);
            this.selectBossForEditing(bossId);
            return null;
        }
        if (action.startsWith("boss_delete:")) {
            String bossId = HordeConfigPage.extractActionArgument(action);
            HordeService.OperationResult result = this.hordeService.deleteBossDefinition(bossId);
            this.bossStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectBossForEditing("");
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown boss action: " + action : "Accion de bosses desconocida: " + action);
    }

    private HordeService.OperationResult handleArenaAction(String action, World world, String fallbackWorldName, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown arenas action." : "Accion de arenas desconocida.");
        }
        if ("arena_page_prev".equals(action)) {
            this.arenaPage = Math.max(0, this.arenaPage - 1);
            return null;
        }
        if ("arena_page_next".equals(action)) {
            this.arenaPage = this.arenaPage + 1;
            return null;
        }
        if ("arena_add_from_player".equals(action)) {
            if (world == null) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Could not resolve active world for arena creation." : "No se pudo resolver el mundo activo para crear arena.");
                this.arenaStatusText = result.getMessage();
                return result;
            }
            HordeService.OperationResult result = this.hordeService.createArenaFromPlayerDraft(this.playerRef, world, "");
            if (result != null && result.isSuccess()) {
                this.selectArenaForEditing("");
            }
            this.arenaStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("arena_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveArenaDefinitionFromUi(this.extractArenaValuesForSave(), fallbackWorldName);
            if (result != null && result.isSuccess()) {
                this.selectArenaForEditing(this.getDraftValue("arenaEditId", this.getDraftValue("arenaSelected", "")));
            }
            this.arenaStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("arena_use_current_position".equals(action)) {
            String selectedArenaId = this.getDraftValue("arenaSelected", "");
            if (selectedArenaId == null || selectedArenaId.isBlank()) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Select an arena first." : "Primero selecciona una arena.");
                this.arenaStatusText = result.getMessage();
                return result;
            }
            Transform transform = this.playerRef.getTransform();
            Vector3d position = transform == null ? null : transform.getPosition();
            if (position == null) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Could not resolve player position." : "No se pudo resolver la posicion del jugador.");
                this.arenaStatusText = result.getMessage();
                return result;
            }
            this.draftValues.put("arenaEditId", this.getDraftValue("arenaEditId", selectedArenaId));
            this.draftValues.put("arenaEditX", HordeConfigPage.formatDouble(position.x));
            this.draftValues.put("arenaEditY", HordeConfigPage.formatDouble(position.y));
            this.draftValues.put("arenaEditZ", HordeConfigPage.formatDouble(position.z));
            HordeService.OperationResult result = this.hordeService.saveArenaDefinitionFromUi(this.extractArenaValuesForSave(), fallbackWorldName);
            if (result != null && result.isSuccess()) {
                this.selectArenaForEditing(this.getDraftValue("arenaEditId", selectedArenaId));
            }
            this.arenaStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if (action.startsWith("arena_open:")) {
            String arenaId = HordeConfigPage.extractActionArgument(action);
            this.selectArenaForEditing(arenaId);
            return null;
        }
        if (action.startsWith("arena_delete:")) {
            String arenaId = HordeConfigPage.extractActionArgument(action);
            HordeService.OperationResult result = this.hordeService.deleteArenaDefinition(arenaId);
            this.arenaStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectArenaForEditing("");
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown arenas action: " + action : "Accion de arenas desconocida: " + action);
    }

    private static String fallbackWorldName(World world) {
        if (world == null || world.getName() == null || world.getName().isBlank()) {
            return "default";
        }
        return world.getName();
    }

    private void safeRebuild() {
        try {
            this.rebuild();
        }
        catch (Exception ignored) {
            // avoid bubbling UI rebuild failures to the caller thread
        }
    }

    private void updateCurrentTabVisibilityOnly() {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        this.applyTabVisibility(commandBuilder, HordeConfigPage.normalizeTab(this.activeTab));
        this.safeSendUpdate(commandBuilder);
    }

    private void safeSendUpdate(UICommandBuilder commandBuilder) {
        if (commandBuilder == null) {
            return;
        }
        try {
            this.sendUpdate(commandBuilder, false);
        }
        catch (Exception ignored) {
            this.safeRebuild();
        }
    }

    private void updateRuntimeControlsOnly(String language) {
        HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
        boolean active = this.hordeService.isActive();
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder
                .set("#StartButton.Visible", !active)
                .set("#StopButton.Visible", active)
                .set("#SkipRoundButton.Visible", active)
                .set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config, language));
        this.safeSendUpdate(commandBuilder);
    }

    private EventData buildConfigSnapshotEvent(String action) {
        EventData eventData = EventData.of((String)"action", (String)action);
        for (UiFieldBinding field : SNAPSHOT_FIELDS) {
            eventData = eventData.append("@" + field.payloadAlias, field.uiValueSelector);
        }
        return eventData;
    }

    private EventData buildLanguageEvent(String action) {
        return this.buildConfigSnapshotEvent(action);
    }

    private EventData buildActionEventWithLanguage(String action) {
        return this.buildConfigSnapshotEvent(action);
    }

    private void applyLanguageFromPayload(JsonObject payload, String action) {
        boolean languageEvent = "set_language".equals(action);
        boolean generalTab = TAB_GENERAL.equals(HordeConfigPage.normalizeTab(this.activeTab));
        if (!languageEvent && !generalTab) {
            return;
        }
        String requestedLanguage = HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "language"), HordeConfigPage.read(payload, "@Language"), HordeConfigPage.read(payload, "Language"));
        if (requestedLanguage == null || requestedLanguage.isBlank()) {
            return;
        }
        String nextLanguage = HordeService.normalizeLanguage(requestedLanguage);
        String currentLanguage = HordeService.normalizeLanguage(this.hordeService.getLanguage());
        if (currentLanguage.equals(nextLanguage)) {
            return;
        }
        this.hordeService.setLanguage(nextLanguage);
    }

    private void captureDraftFromPayload(JsonObject payload, String action) {
        if (payload == null) {
            return;
        }
        for (UiFieldBinding field : SNAPSHOT_FIELDS) {
            if (!this.shouldCaptureFieldFromPayload(field, action)) {
                continue;
            }
            HordeConfigPage.putIfNotBlank(this.draftValues, field.configKey, HordeConfigPage.extractFieldValue(payload, field));
        }
        if (TAB_HORDE.equals(HordeConfigPage.normalizeTab(this.activeTab))) {
            HordeConfigPage.putIfNotBlank(this.draftValues, "enemyLevelMin", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyLevelMin"), HordeConfigPage.read(payload, "@EnemyLevelMin"), HordeConfigPage.read(payload, "EnemyLevelMin")));
            HordeConfigPage.putIfNotBlank(this.draftValues, "enemyLevelMax", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyLevelMax"), HordeConfigPage.read(payload, "@EnemyLevelMax"), HordeConfigPage.read(payload, "EnemyLevelMax")));
        }
    }

    private void ensureDraftDefaults(HordeService.HordeConfig config) {
        if (config == null) {
            return;
        }
        this.putDraftIfMissing("spawnX", HordeConfigPage.formatDouble(config.spawnX));
        this.putDraftIfMissing("spawnY", HordeConfigPage.formatDouble(config.spawnY));
        this.putDraftIfMissing("spawnZ", HordeConfigPage.formatDouble(config.spawnZ));
        this.putDraftIfMissing("minRadius", HordeConfigPage.formatDouble(config.minSpawnRadius));
        this.putDraftIfMissing("maxRadius", HordeConfigPage.formatDouble(config.maxSpawnRadius));
        this.putDraftIfMissing("arenaJoinRadius", HordeConfigPage.formatDouble(config.arenaJoinRadius));
        this.putDraftIfMissing("rounds", Integer.toString(config.rounds));
        this.putDraftIfMissing("baseEnemies", Integer.toString(config.baseEnemiesPerRound));
        this.putDraftIfMissing("enemiesPerRound", Integer.toString(config.enemiesPerRoundIncrement));
        this.putDraftIfMissing("waveDelay", Integer.toString(config.waveDelaySeconds));
        this.putDraftIfMissing("autoStartEnabled", Boolean.toString(config.autoStartIntervalMinutes > 0));
        this.putDraftIfMissing("autoStartIntervalMinutes", Integer.toString(config.autoStartIntervalMinutes > 0 ? config.autoStartIntervalMinutes : 10));
        this.putDraftIfMissing("selectedArenaId", config.selectedArenaId == null ? "" : config.selectedArenaId.trim());
        this.putDraftIfMissing("enemyType", config.enemyType == null ? "undead" : config.enemyType);
        this.putDraftIfMissing("language", HordeService.normalizeLanguage(config.language));
        this.putDraftIfMissing("rewardCategory", HordeConfigPage.firstNonEmpty(config.rewardCategory, this.hordeService.getRewardCategory()));
        this.putDraftIfMissing("rewardItemId", config.rewardItemId == null ? "" : config.rewardItemId);
        this.putDraftIfMissing("rewardItemQuantity", Integer.toString(config.rewardItemQuantity));
        this.putDraftIfMissing("finalBossEnabled", Boolean.toString(config.finalBossEnabled));
        this.putDraftIfMissing("roundStartSoundId", this.hordeService.getRoundStartSoundSelection());
        this.putDraftIfMissing("roundStartVolume", HordeConfigPage.formatDouble(HordeConfigPage.toUiVolumePercent(config.roundStartVolume)));
        this.putDraftIfMissing("roundVictorySoundId", this.hordeService.getRoundVictorySoundSelection());
        this.putDraftIfMissing("roundVictoryVolume", HordeConfigPage.formatDouble(HordeConfigPage.toUiVolumePercent(config.roundVictoryVolume)));
        this.putDraftIfMissing("enemyLevelMin", Integer.toString(config.enemyLevelMin));
        this.putDraftIfMissing("enemyLevelMax", Integer.toString(config.enemyLevelMax));
    }

    private void putDraftIfMissing(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank() || this.draftValues.containsKey(key)) {
            return;
        }
        this.draftValues.put(key, value);
    }

    private String getDraftValue(String key, String fallback) {
        String value = this.draftValues.get(key);
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private double getDraftDouble(String key, double fallback) {
        String raw = this.draftValues.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().replace(',', '.');
        try {
            return Double.parseDouble(normalized);
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private int getDraftInt(String key, int fallback) {
        String raw = this.draftValues.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().replace(',', '.');
        try {
            double parsed = Double.parseDouble(normalized);
            return (int)Math.round(parsed);
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean getDraftBoolean(String key, boolean fallback) {
        String raw = this.draftValues.get(key);
        if (raw == null || raw.isBlank()) {
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

    private void resetDraftFromConfig() {
        this.draftValues.clear();
        this.ensureDraftDefaults(this.hordeService.getConfigSnapshot());
    }

    private void ensureBossDraftDefaults(List<BossArenaCatalogService.BossDefinitionSnapshot> bossRows, List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows) {
        BossArenaCatalogService.BossDefinitionSnapshot selectedBoss = HordeConfigPage.findBossSnapshot(bossRows, this.getDraftValue("bossSelected", ""));
        if (selectedBoss == null && bossRows != null && !bossRows.isEmpty()) {
            selectedBoss = bossRows.get(0);
        }
        if (selectedBoss != null) {
            this.putDraftIfMissing("bossSelected", selectedBoss.bossId);
            this.putDraftIfMissing("bossEditName", selectedBoss.bossId);
            this.putDraftIfMissing("bossEditNpcId", selectedBoss.npcId);
            this.putDraftIfMissing("bossEditTier", selectedBoss.tier);
            this.putDraftIfMissing("bossEditAmount", Integer.toString(selectedBoss.amount));
            this.putDraftIfMissing("bossEditLevelOverride", Integer.toString(selectedBoss.levelOverride));
            this.putDraftIfMissing("bossEditLootRadius", HordeConfigPage.formatDouble(selectedBoss.lootRadius));
            this.putDraftIfMissing("bossSpawnTrigger", selectedBoss.bossSpawnTrigger);
            this.putDraftIfMissing("bossSpawnTriggerValue", HordeConfigPage.formatDouble(selectedBoss.bossSpawnTriggerValue));
            this.putDraftIfMissing("bossWaveRandomLocations", Boolean.toString(selectedBoss.useRandomSpawnLocations));
            this.putDraftIfMissing("bossWaveRandomRadius", HordeConfigPage.formatDouble(selectedBoss.randomSpawnRadius));
            this.putDraftIfMissing("bossTimedProximityEnabled", Boolean.toString(selectedBoss.timedProximityEnabled));
            this.putDraftIfMissing("bossTimedProximityArena", selectedBoss.timedProximityArenaId);
            this.putDraftIfMissing("bossTimedProximityRadius", HordeConfigPage.formatDouble(selectedBoss.timedProximityRadius));
            this.putDraftIfMissing("bossTimedProximityCooldown", Integer.toString(selectedBoss.timedProximityCooldownSeconds));
        }
        this.putDraftIfMissing("bossCreateId", "");
        this.putDraftIfMissing("bossEditTier", "common");
        this.putDraftIfMissing("bossEditAmount", "1");
        this.putDraftIfMissing("bossEditLevelOverride", "0");
        this.putDraftIfMissing("bossEditLootRadius", "0");
        this.putDraftIfMissing("bossSpawnTrigger", "before_boss");
        this.putDraftIfMissing("bossSpawnTriggerValue", "0");
        this.putDraftIfMissing("bossWaveRandomLocations", "false");
        this.putDraftIfMissing("bossWaveRandomRadius", "0");
        this.putDraftIfMissing("bossTimedProximityEnabled", "false");
        this.putDraftIfMissing("bossTimedProximityRadius", "0");
        this.putDraftIfMissing("bossTimedProximityCooldown", "0");
        this.putDraftIfMissing("bossTimedProximityArena", HordeConfigPage.firstArenaId(arenaRows));
    }

    private void ensureArenaDraftDefaults(List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows) {
        BossArenaCatalogService.ArenaDefinitionSnapshot selectedArena = HordeConfigPage.findArenaSnapshot(arenaRows, this.getDraftValue("arenaSelected", ""));
        if (selectedArena == null && arenaRows != null && !arenaRows.isEmpty()) {
            selectedArena = arenaRows.get(0);
        }
        if (selectedArena != null) {
            this.putDraftIfMissing("arenaSelected", selectedArena.arenaId);
            this.putDraftIfMissing("arenaEditId", selectedArena.arenaId);
            this.putDraftIfMissing("arenaEditX", HordeConfigPage.formatDouble(selectedArena.x));
            this.putDraftIfMissing("arenaEditY", HordeConfigPage.formatDouble(selectedArena.y));
            this.putDraftIfMissing("arenaEditZ", HordeConfigPage.formatDouble(selectedArena.z));
        }
        this.putDraftIfMissing("arenaEditX", "0");
        this.putDraftIfMissing("arenaEditY", "64");
        this.putDraftIfMissing("arenaEditZ", "0");
    }

    private void selectBossForEditing(String requestedBossId) {
        List<BossArenaCatalogService.BossDefinitionSnapshot> bossRows = this.hordeService.getBossDefinitionsSnapshot();
        BossArenaCatalogService.BossDefinitionSnapshot selected = HordeConfigPage.findBossSnapshot(bossRows, requestedBossId);
        if (selected == null && bossRows != null && !bossRows.isEmpty()) {
            selected = bossRows.get(bossRows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("bossSelected");
            this.draftValues.remove("bossEditName");
            this.draftValues.remove("bossEditNpcId");
            this.draftValues.remove("bossEditTier");
            this.draftValues.remove("bossEditAmount");
            this.draftValues.remove("bossEditLevelOverride");
            this.draftValues.remove("bossEditLootRadius");
            this.draftValues.remove("bossSpawnTrigger");
            this.draftValues.remove("bossSpawnTriggerValue");
            this.draftValues.remove("bossWaveRandomLocations");
            this.draftValues.remove("bossWaveRandomRadius");
            this.draftValues.remove("bossTimedProximityEnabled");
            this.draftValues.remove("bossTimedProximityArena");
            this.draftValues.remove("bossTimedProximityRadius");
            this.draftValues.remove("bossTimedProximityCooldown");
            return;
        }
        this.applyBossDraftFromSnapshot(selected);
        int selectedIndex = HordeConfigPage.findBossIndex(bossRows, selected.bossId);
        if (selectedIndex >= 0) {
            this.bossPage = selectedIndex / MAX_BOSS_ROWS;
        }
    }

    private void selectArenaForEditing(String requestedArenaId) {
        List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows = this.hordeService.getArenaDefinitionsSnapshot();
        BossArenaCatalogService.ArenaDefinitionSnapshot selected = HordeConfigPage.findArenaSnapshot(arenaRows, requestedArenaId);
        if (selected == null && arenaRows != null && !arenaRows.isEmpty()) {
            selected = arenaRows.get(arenaRows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("arenaSelected");
            this.draftValues.remove("arenaEditId");
            this.draftValues.remove("arenaEditX");
            this.draftValues.remove("arenaEditY");
            this.draftValues.remove("arenaEditZ");
            return;
        }
        this.applyArenaDraftFromSnapshot(selected);
        int selectedIndex = HordeConfigPage.findArenaIndex(arenaRows, selected.arenaId);
        if (selectedIndex >= 0) {
            this.arenaPage = selectedIndex / MAX_ARENA_ROWS;
        }
    }

    private void applyBossDraftFromSnapshot(BossArenaCatalogService.BossDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("bossSelected", snapshot.bossId);
        this.draftValues.put("bossEditName", snapshot.bossId);
        this.draftValues.put("bossEditNpcId", snapshot.npcId == null ? "" : snapshot.npcId);
        this.draftValues.put("bossEditTier", snapshot.tier == null ? "common" : snapshot.tier);
        this.draftValues.put("bossEditAmount", Integer.toString(snapshot.amount));
        this.draftValues.put("bossEditLevelOverride", Integer.toString(snapshot.levelOverride));
        this.draftValues.put("bossEditLootRadius", HordeConfigPage.formatDouble(snapshot.lootRadius));
        this.draftValues.put("bossSpawnTrigger", snapshot.bossSpawnTrigger == null ? "before_boss" : snapshot.bossSpawnTrigger);
        this.draftValues.put("bossSpawnTriggerValue", HordeConfigPage.formatDouble(snapshot.bossSpawnTriggerValue));
        this.draftValues.put("bossWaveRandomLocations", Boolean.toString(snapshot.useRandomSpawnLocations));
        this.draftValues.put("bossWaveRandomRadius", HordeConfigPage.formatDouble(snapshot.randomSpawnRadius));
        this.draftValues.put("bossTimedProximityEnabled", Boolean.toString(snapshot.timedProximityEnabled));
        this.draftValues.put("bossTimedProximityArena", snapshot.timedProximityArenaId == null ? "" : snapshot.timedProximityArenaId);
        this.draftValues.put("bossTimedProximityRadius", HordeConfigPage.formatDouble(snapshot.timedProximityRadius));
        this.draftValues.put("bossTimedProximityCooldown", Integer.toString(snapshot.timedProximityCooldownSeconds));
    }

    private void applyArenaDraftFromSnapshot(BossArenaCatalogService.ArenaDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("arenaSelected", snapshot.arenaId);
        this.draftValues.put("arenaEditId", snapshot.arenaId);
        this.draftValues.put("arenaEditX", HordeConfigPage.formatDouble(snapshot.x));
        this.draftValues.put("arenaEditY", HordeConfigPage.formatDouble(snapshot.y));
        this.draftValues.put("arenaEditZ", HordeConfigPage.formatDouble(snapshot.z));
    }

    private Map<String, String> extractBossValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "bossSelected", this.getDraftValue("bossSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditName", this.getDraftValue("bossEditName", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditNpcId", this.getDraftValue("bossEditNpcId", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditTier", this.getDraftValue("bossEditTier", "common"));
        HordeConfigPage.putIfNotBlank(values, "bossEditAmount", this.getDraftValue("bossEditAmount", "1"));
        HordeConfigPage.putIfNotBlank(values, "bossEditLevelOverride", this.getDraftValue("bossEditLevelOverride", "0"));
        HordeConfigPage.putIfNotBlank(values, "bossEditLootRadius", this.getDraftValue("bossEditLootRadius", "0"));
        HordeConfigPage.putIfNotBlank(values, "bossSpawnTrigger", this.getDraftValue("bossSpawnTrigger", "before_boss"));
        HordeConfigPage.putIfNotBlank(values, "bossSpawnTriggerValue", this.getDraftValue("bossSpawnTriggerValue", "0"));
        values.put("bossWaveRandomLocations", Boolean.toString(this.getDraftBoolean("bossWaveRandomLocations", false)));
        HordeConfigPage.putIfNotBlank(values, "bossWaveRandomRadius", this.getDraftValue("bossWaveRandomRadius", "0"));
        values.put("bossTimedProximityEnabled", Boolean.toString(this.getDraftBoolean("bossTimedProximityEnabled", false)));
        HordeConfigPage.putIfNotBlank(values, "bossTimedProximityArena", this.getDraftValue("bossTimedProximityArena", ""));
        HordeConfigPage.putIfNotBlank(values, "bossTimedProximityRadius", this.getDraftValue("bossTimedProximityRadius", "0"));
        HordeConfigPage.putIfNotBlank(values, "bossTimedProximityCooldown", this.getDraftValue("bossTimedProximityCooldown", "0"));
        return values;
    }

    private Map<String, String> extractArenaValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "arenaSelected", this.getDraftValue("arenaSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "arenaEditId", this.getDraftValue("arenaEditId", ""));
        HordeConfigPage.putIfNotBlank(values, "arenaEditX", this.getDraftValue("arenaEditX", "0"));
        HordeConfigPage.putIfNotBlank(values, "arenaEditY", this.getDraftValue("arenaEditY", "64"));
        HordeConfigPage.putIfNotBlank(values, "arenaEditZ", this.getDraftValue("arenaEditZ", "0"));
        return values;
    }

    private void populateBossRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<BossArenaCatalogService.BossDefinitionSnapshot> rows, String language, boolean english) {
        int total = rows == null ? 0 : rows.size();
        int pageCount = Math.max(1, (int)Math.ceil(total / (double)MAX_BOSS_ROWS));
        this.bossPage = HordeConfigPage.clamp(this.bossPage, 0, pageCount - 1);
        int start = this.bossPage * MAX_BOSS_ROWS;
        String selectedBossId = this.getDraftValue("bossSelected", "");
        for (int slot = 0; slot < MAX_BOSS_ROWS; ++slot) {
            int index = start + slot;
            int rowNumber = slot + 1;
            String rowSelector = "#BossRow" + rowNumber;
            if (index < total) {
                BossArenaCatalogService.BossDefinitionSnapshot row = rows.get(index);
                String tierText = HordeConfigPage.t(language, english, "Tier", "Tier") + ": " + HordeConfigPage.compactName(row.tier, 12);
                commandBuilder.set(rowSelector + ".Visible", true)
                        .set("#BossName" + rowNumber + ".Text", row.bossId)
                        .set("#BossNpc" + rowNumber + ".Text", row.npcId == null ? "" : row.npcId)
                        .set("#BossTier" + rowNumber + ".Text", tierText)
                        .set("#BossAmount" + rowNumber + ".Text", Integer.toString(row.amount))
                        .set(rowSelector + ".Background", selectedBossId.equalsIgnoreCase(row.bossId) ? "#2b4a70" : "#16283f");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BossOpen" + rowNumber, this.buildConfigSnapshotEvent(HordeConfigPage.buildBossAction("open", row.bossId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, "#BossDelete" + rowNumber, this.buildConfigSnapshotEvent(HordeConfigPage.buildBossAction("delete", row.bossId)));
            } else {
                commandBuilder.set(rowSelector + ".Visible", false)
                        .set("#BossName" + rowNumber + ".Text", "")
                        .set("#BossNpc" + rowNumber + ".Text", "")
                        .set("#BossTier" + rowNumber + ".Text", "")
                        .set("#BossAmount" + rowNumber + ".Text", "0");
            }
        }
        String pageText = total <= 0 ? "0/0" : (this.bossPage + 1) + "/" + pageCount;
        commandBuilder.set("#BossPageLabel.Text", pageText)
                .set("#BossPageLabel.Visible", pageCount > 1)
                .set("#BossPagePrevButton.Visible", pageCount > 1)
                .set("#BossPageNextButton.Visible", pageCount > 1)
                .set("#BossEmptyLabel.Visible", total == 0)
                .set("#BossEmptyLabel.Text", total == 0 ? HordeConfigPage.t(language, english, "No bosses yet. Press Add Boss to create one.", "Aun no hay bosses. Pulsa Anadir Boss para crear uno.") : "")
                .set("#BossOverflowLabel.Visible", total > MAX_BOSS_ROWS)
                .set("#BossOverflowLabel.Text", total > MAX_BOSS_ROWS ? HordeConfigPage.t(language, english, "More bosses available. Use page controls.", "Hay mas bosses disponibles. Usa el paginado.") : "");
    }

    private void populateArenaRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows, String language, boolean english) {
        int total = rows == null ? 0 : rows.size();
        int pageCount = Math.max(1, (int)Math.ceil(total / (double)MAX_ARENA_ROWS));
        this.arenaPage = HordeConfigPage.clamp(this.arenaPage, 0, pageCount - 1);
        int start = this.arenaPage * MAX_ARENA_ROWS;
        String selectedArenaId = this.getDraftValue("arenaSelected", "");
        for (int slot = 0; slot < MAX_ARENA_ROWS; ++slot) {
            int index = start + slot;
            int rowNumber = slot + 1;
            String rowSelector = "#ArenaRow" + rowNumber;
            if (index < total) {
                BossArenaCatalogService.ArenaDefinitionSnapshot row = rows.get(index);
                String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", row.x, row.y, row.z);
                commandBuilder.set(rowSelector + ".Visible", true)
                        .set("#ArenaName" + rowNumber + ".Text", row.arenaId)
                        .set("#ArenaCoords" + rowNumber + ".Text", coords)
                        .set(rowSelector + ".Background", selectedArenaId.equalsIgnoreCase(row.arenaId) ? "#2b4a70" : "#16283f");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ArenaOpen" + rowNumber, this.buildConfigSnapshotEvent(HordeConfigPage.buildArenaAction("open", row.arenaId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaDelete" + rowNumber, this.buildConfigSnapshotEvent(HordeConfigPage.buildArenaAction("delete", row.arenaId)));
            } else {
                commandBuilder.set(rowSelector + ".Visible", false)
                        .set("#ArenaName" + rowNumber + ".Text", "")
                        .set("#ArenaCoords" + rowNumber + ".Text", "");
            }
        }
        String pageText = total <= 0 ? "0/0" : (this.arenaPage + 1) + "/" + pageCount;
        commandBuilder.set("#ArenaPageLabel.Text", pageText)
                .set("#ArenaPageLabel.Visible", pageCount > 1)
                .set("#ArenaPagePrevButton.Visible", pageCount > 1)
                .set("#ArenaPageNextButton.Visible", pageCount > 1)
                .set("#ArenaEmptyLabel.Visible", total == 0)
                .set("#ArenaEmptyLabel.Text", total == 0 ? HordeConfigPage.t(language, english, "No arenas yet. Use Add Arena here to create one from your position.", "Aun no hay arenas. Usa Anadir Arena aqui para crear una desde tu posicion.") : "")
                .set("#ArenaOverflowLabel.Visible", total > MAX_ARENA_ROWS)
                .set("#ArenaOverflowLabel.Text", total > MAX_ARENA_ROWS ? HordeConfigPage.t(language, english, "More arenas available. Use page controls.", "Hay mas arenas disponibles. Usa el paginado.") : "");
    }

    private void populateAudienceRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeService.AudiencePlayerSnapshot> rows, String language, boolean english) {
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
                    .set("#AudiencePlayersRows[" + rowIndex + "] #RowMode.Text", HordeConfigPage.audienceModeDisplay(mode, language))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetPlayerButton.Text", playerMode ? HordeConfigPage.t(language, english, "Player *", "Jugador *") : HordeConfigPage.t(language, english, "Player", "Jugador"))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetSpectatorButton.Text", spectatorMode ? HordeConfigPage.t(language, english, "Spectator *", "Espectador *") : HordeConfigPage.t(language, english, "Spectator", "Espectador"))
                    .set("#AudiencePlayersRows[" + rowIndex + "] #SetExitButton.Text", exitMode ? HordeConfigPage.t(language, english, "Exit *", "Salir *") : HordeConfigPage.t(language, english, "Exit", "Salir"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetPlayerButton", this.buildActionEventWithLanguage(HordeConfigPage.buildAudienceAction("player", row.playerId)))
                    .addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetSpectatorButton", this.buildActionEventWithLanguage(HordeConfigPage.buildAudienceAction("spectator", row.playerId)))
                    .addEventBinding(CustomUIEventBindingType.Activating, "#AudiencePlayersRows[" + rowIndex + "] #SetExitButton", this.buildActionEventWithLanguage(HordeConfigPage.buildAudienceAction("exit", row.playerId)));
            ++rowIndex;
        }
        int hiddenRows = Math.max(0, rows.size() - MAX_AUDIENCE_ROWS);
        if (hiddenRows > 0) {
            commandBuilder.set("#PlayersListHint.Text", HordeConfigPage.t(language, english, "Showing first " + MAX_AUDIENCE_ROWS + " players (" + hiddenRows + " more not shown).", "Mostrando primeros " + MAX_AUDIENCE_ROWS + " jugadores (" + hiddenRows + " mas sin mostrar)."));
        }
    }

    private static String buildAudienceAction(String mode, UUID playerId) {
        return "audience_set:" + mode + ":" + playerId;
    }

    private Map<String, String> extractConfigValuesForApply() {
        this.ensureDraftDefaults(this.hordeService.getConfigSnapshot());
        HashMap<String, String> values = new HashMap<String, String>();
        for (UiFieldBinding field : SNAPSHOT_FIELDS) {
            HordeConfigPage.putIfNotBlank(values, field.configKey, this.draftValues.get(field.configKey));
        }
        boolean autoStartEnabled = this.getDraftBoolean("autoStartEnabled", false);
        int autoStartIntervalMinutes = HordeConfigPage.clamp(this.getDraftInt("autoStartIntervalMinutes", 10), 1, 1440);
        values.put("autoStartIntervalMinutes", Integer.toString(autoStartEnabled ? autoStartIntervalMinutes : 0));
        HordeConfigPage.putIfNotBlank(values, "enemyLevelMin", this.draftValues.get("enemyLevelMin"));
        HordeConfigPage.putIfNotBlank(values, "enemyLevelMax", this.draftValues.get("enemyLevelMax"));
        return values;
    }

    private boolean shouldCaptureFieldFromPayload(UiFieldBinding field, String action) {
        if (field == null || field.configKey == null || field.configKey.isBlank()) {
            return false;
        }
        if ("set_language".equals(action)) {
            return "language".equals(field.configKey);
        }
        String tab = HordeConfigPage.normalizeTab(this.activeTab);
        switch (tab) {
            case TAB_GENERAL:
                return "spawnX".equals(field.configKey)
                        || "spawnY".equals(field.configKey)
                        || "spawnZ".equals(field.configKey)
                        || "autoStartEnabled".equals(field.configKey)
                        || "autoStartIntervalMinutes".equals(field.configKey)
                        || "selectedArenaId".equals(field.configKey)
                        || "language".equals(field.configKey);
            case TAB_HORDE:
                return "minRadius".equals(field.configKey)
                        || "maxRadius".equals(field.configKey)
                        || "rounds".equals(field.configKey)
                        || "baseEnemies".equals(field.configKey)
                        || "enemiesPerRound".equals(field.configKey)
                        || "waveDelay".equals(field.configKey)
                        || "enemyType".equals(field.configKey)
                        || "finalBossEnabled".equals(field.configKey);
            case TAB_PLAYERS:
                return "arenaJoinRadius".equals(field.configKey);
            case TAB_SOUNDS:
                return "roundStartSoundId".equals(field.configKey)
                        || "roundStartVolume".equals(field.configKey)
                        || "roundVictorySoundId".equals(field.configKey)
                        || "roundVictoryVolume".equals(field.configKey);
            case TAB_REWARDS:
                return "rewardCategory".equals(field.configKey)
                        || "rewardItemId".equals(field.configKey)
                        || "rewardItemQuantity".equals(field.configKey);
            case TAB_BOSSES:
                return "bossCreateId".equals(field.configKey)
                        || "bossSelected".equals(field.configKey)
                        || "bossEditName".equals(field.configKey)
                        || "bossEditNpcId".equals(field.configKey)
                        || "bossEditTier".equals(field.configKey)
                        || "bossEditAmount".equals(field.configKey)
                        || "bossEditLevelOverride".equals(field.configKey)
                        || "bossEditLootRadius".equals(field.configKey)
                        || "bossSpawnTrigger".equals(field.configKey)
                        || "bossSpawnTriggerValue".equals(field.configKey)
                        || "bossWaveRandomLocations".equals(field.configKey)
                        || "bossWaveRandomRadius".equals(field.configKey)
                        || "bossTimedProximityEnabled".equals(field.configKey)
                        || "bossTimedProximityArena".equals(field.configKey)
                        || "bossTimedProximityRadius".equals(field.configKey)
                        || "bossTimedProximityCooldown".equals(field.configKey);
            case TAB_ARENAS:
                return "arenaSelected".equals(field.configKey)
                        || "arenaEditId".equals(field.configKey)
                        || "arenaEditX".equals(field.configKey)
                        || "arenaEditY".equals(field.configKey)
                        || "arenaEditZ".equals(field.configKey);
            default:
                return false;
        }
    }

    private static void putIfNotBlank(Map<String, String> values, String key, String value) {
        if (values == null || key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }
        values.put(key, value);
    }

    private static String extractFieldValue(JsonObject payload, UiFieldBinding field) {
        String value = HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, field.configKey), HordeConfigPage.read(payload, "@" + field.payloadAlias), HordeConfigPage.read(payload, field.payloadAlias));
        if (field.extraPayloadKeys != null) {
            for (String key : field.extraPayloadKeys) {
                value = HordeConfigPage.firstNonEmpty(value, HordeConfigPage.read(payload, key));
            }
        }
        return value;
    }

    private static String buildSpawnLabel(HordeService.HordeConfig config, String language) {
        boolean english = HordeService.isEnglishLanguage(language);
        if (!config.spawnConfigured) {
            return HordeConfigPage.t(language, english, "Horde center not configured. You can use your current position.", "Centro de horda no configurado. Puedes usar tu posicion actual.");
        }
        if (english) {
            return HordeConfigPage.t(language, true, String.format(Locale.ROOT, "Current center: %.2f %.2f %.2f | World: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName), String.format(Locale.ROOT, "Centro actual: %.2f %.2f %.2f | Mundo: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName));
        }
        return HordeConfigPage.t(language, false, String.format(Locale.ROOT, "Current center: %.2f %.2f %.2f | World: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName), String.format(Locale.ROOT, "Centro actual: %.2f %.2f %.2f | Mundo: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName));
    }

    private static List<DropdownEntryInfo> buildDropdownEntries(List<String> options, String selectedValue) {
        ArrayList<String> values = new ArrayList<String>();
        if (options != null) {
            for (String option : options) {
                if (option == null) continue;
                String trimmed = option.trim();
                if (trimmed.isBlank()) continue;
                if (HordeConfigPage.containsIgnoreCase(values, trimmed)) continue;
                values.add(trimmed);
            }
        }
        String selected = selectedValue == null ? "" : selectedValue.trim();
        if (!selected.isBlank() && !HordeConfigPage.containsIgnoreCase(values, selected)) {
            values.add(0, selected);
        }
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(value), value));
        }
        return entries;
    }

    private static List<DropdownEntryInfo> buildEnemyTypeEntries(List<String> options, String selectedValue, String language, boolean english) {
        List<String> values = HordeConfigPage.collectDropdownValues(options, selectedValue);
        ArrayList<DropdownEntryInfo> localizedEntries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            String text = HordeConfigPage.enemyTypeDisplay(value, language, english);
            localizedEntries.add(new DropdownEntryInfo(LocalizableString.fromString(text), value));
        }
        return localizedEntries;
    }

    private static List<DropdownEntryInfo> buildRewardCategoryEntries(List<String> options, String selectedValue, String language, boolean english) {
        List<String> values = HordeConfigPage.collectDropdownValues(options, selectedValue);
        ArrayList<DropdownEntryInfo> localizedEntries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            String text = HordeConfigPage.rewardCategoryDisplay(value, language, english);
            localizedEntries.add(new DropdownEntryInfo(LocalizableString.fromString(text), value));
        }
        return localizedEntries;
    }

    private static List<DropdownEntryInfo> buildRoundSoundEntries(List<String> options, String selectedValue, String language, boolean english) {
        List<String> values = HordeConfigPage.collectDropdownValues(options, selectedValue);
        ArrayList<DropdownEntryInfo> localizedEntries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            String text = value;
            if ("auto".equals(normalized)) {
                text = HordeConfigPage.t(language, english, "Auto (recommended)", "Auto (recomendado)");
            } else if ("none".equals(normalized)) {
                text = HordeConfigPage.t(language, english, "None", "Ninguno");
            }
            localizedEntries.add(new DropdownEntryInfo(LocalizableString.fromString(text), value));
        }
        return localizedEntries;
    }

    private static List<DropdownEntryInfo> buildBossSpawnTriggerEntries(List<String> options, String selectedValue, String language, boolean english) {
        List<String> values = HordeConfigPage.collectDropdownValues(options, selectedValue);
        ArrayList<DropdownEntryInfo> localizedEntries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            localizedEntries.add(new DropdownEntryInfo(LocalizableString.fromString(HordeConfigPage.bossSpawnTriggerDisplay(value, language, english)), value));
        }
        return localizedEntries;
    }

    private static List<String> collectDropdownValues(List<String> options, String selectedValue) {
        ArrayList<String> values = new ArrayList<String>();
        if (options != null) {
            for (String option : options) {
                if (option == null) continue;
                String trimmed = option.trim();
                if (trimmed.isBlank() || HordeConfigPage.containsIgnoreCase(values, trimmed)) continue;
                values.add(trimmed);
            }
        }
        String selected = selectedValue == null ? "" : selectedValue.trim();
        if (!selected.isBlank() && !HordeConfigPage.containsIgnoreCase(values, selected)) {
            values.add(0, selected);
        }
        return values;
    }

    private static String bossSpawnTriggerDisplay(String value, String language, boolean english) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "before_boss":
                return HordeConfigPage.t(language, english, "Before boss", "Antes del boss");
            case "on_spawn":
                return HordeConfigPage.t(language, english, "On spawn", "Al aparecer");
            case "after_spawn_seconds":
                return HordeConfigPage.t(language, english, "After spawn (s)", "Tras spawn (s)");
            case "since_last_wave":
                return HordeConfigPage.t(language, english, "Since last wave (s)", "Desde ultima wave (s)");
            case "boss_hp_percent":
                return HordeConfigPage.t(language, english, "Boss HP %", "HP del boss %");
            default:
                return value == null ? "" : value;
        }
    }

    private static String enemyTypeDisplay(String value, String language, boolean english) {
        if (value == null || value.isBlank()) {
            return HordeConfigPage.t(language, english, "Undead", "No muertos");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        switch (normalized) {
            case "undead":
                return HordeConfigPage.t(language, english, "Undead", "No muertos");
            case "goblins":
                return HordeConfigPage.t(language, english, "Goblins", "Goblins");
            case "scarak":
                return HordeConfigPage.t(language, english, "Scarak", "Scarak");
            case "void":
                return HordeConfigPage.t(language, english, "Void", "Vacio");
            case "wild":
                return HordeConfigPage.t(language, english, "Wild creatures", "Criaturas agresivas");
            case "elementals":
                return HordeConfigPage.t(language, english, "Elementals", "Elementales");
            case "random":
                return HordeConfigPage.t(language, english, "Random by category", "Aleatorio por categoria");
            case "random-all":
                return HordeConfigPage.t(language, english, "Random from all", "Aleatorio total");
            default:
                return value;
        }
    }

    private static String rewardCategoryDisplay(String value, String language, boolean english) {
        if (value == null || value.isBlank()) {
            return HordeConfigPage.t(language, english, "Mithril", "Mithril");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        switch (normalized) {
            case "mithril":
                return "Mithril";
            case "onyxium":
                return "Onyxium";
            case "gemas":
                return HordeConfigPage.t(language, english, "Gems", "Gemas");
            case "metales":
                return HordeConfigPage.t(language, english, "Metals", "Metales");
            case "materiales_raros":
                return HordeConfigPage.t(language, english, "Rare materials", "Materiales raros");
            case "armas_especiales":
                return HordeConfigPage.t(language, english, "Special weapons", "Armas especiales");
            case "items_especiales":
                return HordeConfigPage.t(language, english, "Special items", "Items especiales");
            case "random":
                return HordeConfigPage.t(language, english, "Random by category", "Aleatorio por categoria");
            case "random_all":
                return HordeConfigPage.t(language, english, "Random from all", "Aleatorio total");
            default:
                return value;
        }
    }

    private static List<DropdownEntryInfo> buildLanguageEntries(List<String> languageOptions, String selectedLanguage) {
        ArrayList<String> values = new ArrayList<String>();
        if (languageOptions != null) {
            values.addAll(languageOptions);
        }
        if (values.isEmpty()) {
            values.addAll(HordeI18n.LANGUAGE_OPTIONS);
        }
        String selected = HordeService.normalizeLanguage(selectedLanguage);
        if (!values.contains(selected)) {
            values.add(0, selected);
        }
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String code : values) {
            String normalizedCode = HordeService.normalizeLanguage(code);
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(HordeService.getLanguageDisplay(normalizedCode)), normalizedCode));
        }
        return entries;
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || values.isEmpty() || value == null) {
            return false;
        }
        for (String candidate : values) {
            if (candidate == null || !candidate.equalsIgnoreCase(value)) continue;
            return true;
        }
        return false;
    }

    private static String t(String language, boolean english, String englishText, String spanishText) {
        String normalizedLanguage = HordeService.normalizeLanguage(language);
        if (HordeI18n.LANGUAGE_ENGLISH.equals(normalizedLanguage)) {
            return englishText;
        }
        if (HordeI18n.LANGUAGE_SPANISH.equals(normalizedLanguage)) {
            return spanishText;
        }
        String translatedFromEnglish = HordeI18n.translateLegacy(normalizedLanguage, englishText);
        if (!translatedFromEnglish.equals(englishText)) {
            return translatedFromEnglish;
        }
        String translatedFromSpanish = HordeI18n.translateLegacy(normalizedLanguage, spanishText);
        if (!translatedFromSpanish.equals(spanishText)) {
            return translatedFromSpanish;
        }
        return translatedFromEnglish;
    }

    private void setLocalizedTexts(UICommandBuilder commandBuilder, String language, boolean english) {
        commandBuilder.set("#TitleLabel.Text", HordeConfigPage.t(language, english, "Horde PVE Config", "Horda PVE Config"))
                .set("#SubTitleLabel.Text", "")
                .set("#TabGeneralButton.Text", HordeConfigPage.t(language, english, "General", "General"))
                .set("#TabHordeButton.Text", HordeConfigPage.t(language, english, "Horde", "Horda"))
                .set("#TabPlayersButton.Text", HordeConfigPage.t(language, english, "Players", "Jugadores"))
                .set("#TabSoundsButton.Text", HordeConfigPage.t(language, english, "Sounds", "Sonidos"))
                .set("#TabRewardsButton.Text", HordeConfigPage.t(language, english, "Rewards", "Recompensas"))
                .set("#TabBossesButton.Text", HordeConfigPage.t(language, english, "Bosses", "Bosses"))
                .set("#TabArenasButton.Text", HordeConfigPage.t(language, english, "Arenas", "Arenas"))
                .set("#TabHelpButton.Text", HordeConfigPage.t(language, english, "Help", "Ayuda"))
                .set("#TabHintLabel.Text", "")
                .set("#GeneralArenaLabel.Text", HordeConfigPage.t(language, english, "Current horde arena", "Arena actual de la horda"))
                .set("#SpawnLabel.Text", HordeConfigPage.t(language, english, "Center (X Y Z)", "Centro (X Y Z)"))
                .set("#SetSpawnButton.Text", HordeConfigPage.t(language, english, "Use my current position", "Usar mi posicion actual"))
                .set("#RadiusLabel.Text", HordeConfigPage.t(language, english, "Enemy spawn radius setup", "Configuracion del radio de aparicion de enemigos"))
                .set("#RoundConfigLabel.Text", HordeConfigPage.t(language, english, "Round setup", "Configuracion de ronda"))
                .set("#MinRadiusLabel.Text", HordeConfigPage.t(language, english, "Minimum radius", "Radio minimo"))
                .set("#MaxRadiusLabel.Text", HordeConfigPage.t(language, english, "Maximum radius", "Radio maximo"))
                .set("#ArenaJoinRadiusLabel.Text", HordeConfigPage.t(language, english, "Arena players radius", "Radio de jugadores de arena"))
                .set("#PlayersListTitle.Text", HordeConfigPage.t(language, english, "Players inside current area", "Jugadores dentro del area actual"))
                .set("#PlayersCountLabel.Text", HordeConfigPage.t(language, english, "Detected", "Detectados"))
                .set("#PlayersHeaderName.Text", HordeConfigPage.t(language, english, "Player", "Jugador"))
                .set("#PlayersHeaderMode.Text", HordeConfigPage.t(language, english, "Mode", "Modo"))
                .set("#PlayersRefreshButton.Text", HordeConfigPage.t(language, english, "Refresh list", "Actualizar lista"))
                .set("#AudienceHelpLabel.Text", HordeConfigPage.t(language, english, "Changes apply to next start. If horde is active, they are applied to current lock immediately.", "Cambios aplican al siguiente inicio. Si la horda esta activa, se aplican al bloqueo actual."))
                .set("#RoundLabel.Text", HordeConfigPage.t(language, english, "Number of rounds", "Cantidad de rondas"))
                .set("#BaseEnemiesLabel.Text", HordeConfigPage.t(language, english, "Base enemies per round", "Cantidad base de enemigos por ronda"))
                .set("#EnemiesPerRoundLabel.Text", HordeConfigPage.t(language, english, "Enemy increment per round", "Incremento de enemigos por ronda"))
                .set("#WaveDelayLabel.Text", HordeConfigPage.t(language, english, "Delay between rounds (s)", "Tiempo de espera entre rondas (s)"))
                .set("#RoleLabel.Text", HordeConfigPage.t(language, english, "Horde category", "Categoria de horda"))
                .set("#LanguageLabel.Text", HordeConfigPage.t(language, english, "Interface language", "Idioma interfaz"))
                .set("#EnemyLevelRangeLabel.Text", "")
                .set("#EnemyLevelWipLabel.Text", "")
                .set("#AutoStartEnabledLabel.Text", HordeConfigPage.t(language, english, "Automatic horde mode", "Modo horda automatica"))
                .set("#AutoStartIntervalLabel.Text", HordeConfigPage.t(language, english, "Start every (minutes)", "Iniciar cada (minutos)"))
                .set("#AutoStartApplyButton.Text", HordeConfigPage.t(language, english, "Apply auto mode", "Aplicar modo auto"))
                .set("#RewardEveryRoundsLabel.Text", HordeConfigPage.t(language, english, "Reward every round(s)", "Recompensa por ronda(s)"))
                .set("#RewardCategoryLabel.Text", HordeConfigPage.t(language, english, "Reward category", "Categoria recompensa"))
                .set("#RewardCommandsLabel.Text", HordeConfigPage.t(language, english, "Reward item", "Item recompensa"))
                .set("#RewardItemQuantityLabel.Text", HordeConfigPage.t(language, english, "Qty.", "Cant."))
                .set("#FinalBossLabel.Text", HordeConfigPage.t(language, english, "Final boss", "Boss final"))
                .set("#RoundStartSoundLabel.Text", HordeConfigPage.t(language, english, "Round start sound", "Sonido inicio ronda"))
                .set("#RoundVictorySoundLabel.Text", HordeConfigPage.t(language, english, "Round victory sound", "Sonido victoria ronda"))
                .set("#RoundStartVolumeLabel.Text", HordeConfigPage.t(language, english, "Start volume (%)", "Volumen inicio (%)"))
                .set("#RoundVictoryVolumeLabel.Text", HordeConfigPage.t(language, english, "Victory volume (%)", "Volumen victoria (%)"))
                .set("#BossesTitleLabel.Text", HordeConfigPage.t(language, english, "Boss definitions", "Definiciones de bosses"))
                .set("#BossCreateIdLabel.Text", HordeConfigPage.t(language, english, "New boss ID", "Nuevo ID de boss"))
                .set("#BossAddButton.Text", HordeConfigPage.t(language, english, "Add boss", "Anadir boss"))
                .set("#BossHeaderName.Text", HordeConfigPage.t(language, english, "Boss ID", "Boss ID"))
                .set("#BossHeaderNpc.Text", HordeConfigPage.t(language, english, "NPC ID", "NPC ID"))
                .set("#BossHeaderTier.Text", HordeConfigPage.t(language, english, "Tier", "Tier"))
                .set("#BossHeaderAmount.Text", HordeConfigPage.t(language, english, "Amt", "Cant"))
                .set("#BossHeaderActions.Text", HordeConfigPage.t(language, english, "Actions", "Acciones"))
                .set("#BossEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Boss editor", "Editor de boss"))
                .set("#BossSelectedLabel.Text", HordeConfigPage.t(language, english, "Selected boss", "Boss seleccionado"))
                .set("#BossEditNameLabel.Text", HordeConfigPage.t(language, english, "Boss ID", "Boss ID"))
                .set("#BossEditNpcIdLabel.Text", HordeConfigPage.t(language, english, "NPC ID", "NPC ID"))
                .set("#BossEditTierLabel.Text", HordeConfigPage.t(language, english, "Tier", "Tier"))
                .set("#BossEditAmountLabel.Text", HordeConfigPage.t(language, english, "Amount", "Cantidad"))
                .set("#BossEditLevelOverrideLabel.Text", HordeConfigPage.t(language, english, "Level override", "Nivel fijo"))
                .set("#BossEditLootRadiusLabel.Text", HordeConfigPage.t(language, english, "Loot radius", "Radio de loot"))
                .set("#BossSpawnTriggerLabel.Text", HordeConfigPage.t(language, english, "Spawn trigger", "Disparador de spawn"))
                .set("#BossSpawnTriggerValueLabel.Text", HordeConfigPage.t(language, english, "Trigger value", "Valor del disparador"))
                .set("#BossWaveRandomLocationsLabel.Text", HordeConfigPage.t(language, english, "Random locations", "Ubicaciones aleatorias"))
                .set("#BossWaveRandomRadiusLabel.Text", HordeConfigPage.t(language, english, "Random radius", "Radio aleatorio"))
                .set("#BossTimedProximityEnabledLabel.Text", HordeConfigPage.t(language, english, "Timed proximity", "Proximidad temporizada"))
                .set("#BossTimedProximityArenaLabel.Text", HordeConfigPage.t(language, english, "Timed arena", "Arena temporizada"))
                .set("#BossTimedProximityRadiusLabel.Text", HordeConfigPage.t(language, english, "Proximity radius", "Radio de proximidad"))
                .set("#BossTimedProximityCooldownLabel.Text", HordeConfigPage.t(language, english, "Cooldown (s)", "Cooldown (s)"))
                .set("#BossPagePrevButton.Text", "<")
                .set("#BossPageNextButton.Text", ">")
                .set("#BossSaveButton.Text", HordeConfigPage.t(language, english, "Save boss", "Guardar boss"))
                .set("#ArenasTitleLabel.Text", HordeConfigPage.t(language, english, "Arena definitions", "Definiciones de arenas"))
                .set("#ArenaAddButton.Text", HordeConfigPage.t(language, english, "Add arena here", "Anadir arena aqui"))
                .set("#ArenaHeaderName.Text", HordeConfigPage.t(language, english, "Arena ID", "Arena ID"))
                .set("#ArenaHeaderCoords.Text", HordeConfigPage.t(language, english, "Coordinates", "Coordenadas"))
                .set("#ArenaHeaderActions.Text", HordeConfigPage.t(language, english, "Actions", "Acciones"))
                .set("#ArenaEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Arena editor", "Editor de arena"))
                .set("#ArenaSelectedLabel.Text", HordeConfigPage.t(language, english, "Selected arena", "Arena seleccionada"))
                .set("#ArenaEditIdLabel.Text", HordeConfigPage.t(language, english, "Arena ID", "Arena ID"))
                .set("#ArenaEditXLabel.Text", "X")
                .set("#ArenaEditYLabel.Text", "Y")
                .set("#ArenaEditZLabel.Text", "Z")
                .set("#ArenaUseCurrentPositionButton.Text", HordeConfigPage.t(language, english, "Use my current position", "Usar mi posicion actual"))
                .set("#ArenaPagePrevButton.Text", "<")
                .set("#ArenaPageNextButton.Text", ">")
                .set("#ArenaSaveButton.Text", HordeConfigPage.t(language, english, "Save arena", "Guardar arena"))
                .set("#StatusTitleLabel.Text", "")
                .set("#ReloadModButton.Text", HordeConfigPage.t(language, english, "Reload config", "Recargar config"))
                .set("#SaveButton.Text", HordeConfigPage.t(language, english, "Save config", "Guardar config"))
                .set("#StartButton.Text", HordeConfigPage.t(language, english, "Start horde", "Iniciar horda"))
                .set("#StopButton.Text", HordeConfigPage.t(language, english, "Stop horde", "Detener horda"))
                .set("#SkipRoundButton.Text", HordeConfigPage.t(language, english, "Skip round", "Pasar ronda"))
                .set("#HelpIntroLabel.Text", HordeConfigPage.t(language, english, "Quick guide for Horde PVE Config (v1.2.x)", "Guia rapida para Horde PVE Config (v1.2.x)"))
                .set("#HelpCommandsLabel.Text", HordeConfigPage.t(language, english, "Main player commands", "Comandos principales de jugador"))
                .set("#HelpCommandsLine1.Text", HordeConfigPage.t(language, english, "/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): open config UI.", "/hordeconfig (alias: /hconfig /hordecfg /hordepve /spawnve /spawnpve): abre la UI de configuracion."))
                .set("#HelpCommandsLine2.Text", HordeConfigPage.t(language, english, "/hordeconfig start | stop | status | logs | setspawn | reload.", "/hordeconfig start | stop | status | logs | setspawn | reload."))
                .set("#HelpCommandsLine3.Text", HordeConfigPage.t(language, english, "/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.", "/hordeconfig enemy <tipo> | tipos | role <rolNpc|auto> | roles | reward <rondas> | spectator <on|off> | player | arearadius <bloques>."))
                .set("#HelpConfigLabel.Text", HordeConfigPage.t(language, english, "What Save Config stores", "Que guarda Guardar config"))
                .set("#HelpConfigLine1.Text", HordeConfigPage.t(language, english, "Center/world, min-max spawn radius, players area radius and interface language.", "Centro/mundo, radio minimo-maximo de aparicion, radio de area de jugadores e idioma interfaz."))
                .set("#HelpConfigLine2.Text", HordeConfigPage.t(language, english, "Rounds, base enemies, increment per round, delay between rounds and final boss toggle.", "Rondas, enemigos base, incremento por ronda, espera entre rondas y activar/desactivar boss final."))
                .set("#HelpConfigLine3.Text", HordeConfigPage.t(language, english, "Rewards (category, item, quantity) and sounds (start/victory id plus volume).", "Recompensas (categoria, item, cantidad) y sonidos (id de inicio/victoria mas volumen)."))
                .set("#HelpExternalLabel.Text", HordeConfigPage.t(language, english, "External JSON files (plugin data folder)", "JSON externos (carpeta de datos del plugin)"))
                .set("#HelpExternalLine1.Text", HordeConfigPage.t(language, english, "enemy-categories.json: categories, final boss roles and blocked role hints.", "enemy-categories.json: categorias, roles de boss final y pistas de roles bloqueados."))
                .set("#HelpExternalLine2.Text", HordeConfigPage.t(language, english, "reward-items.json: reward categories and selectable item IDs.", "reward-items.json: categorias de recompensa e IDs de item seleccionables."))
                .set("#HelpExternalLine3.Text", HordeConfigPage.t(language, english, "horde-sounds.json: auto-sound hints and filters for round start/victory.", "horde-sounds.json: pistas y filtros de sonido automatico para inicio/victoria de ronda."))
                .set("#HelpReloadLabel.Text", HordeConfigPage.t(language, english, "Reload and deployment", "Recarga y despliegue"))
                .set("#HelpReloadLine1.Text", HordeConfigPage.t(language, english, "'Reload config' or /hordareload config reloads all JSON config files without restart.", "'Recargar config' o /hordareload config recarga todos los JSON de configuracion sin reiniciar."))
                .set("#HelpReloadLine2.Text", HordeConfigPage.t(language, english, "Replacing the mod .jar still requires a full server restart.", "Reemplazar el .jar del mod sigue requiriendo reinicio completo del servidor."))
                .set("#HordeCloseButton.Text", HordeConfigPage.t(language, english, "Close", "Cerrar"));
    }

    private void applyTabVisibility(UICommandBuilder commandBuilder, String tab) {
        boolean generalTab = TAB_GENERAL.equals(tab);
        boolean hordeTab = TAB_HORDE.equals(tab);
        boolean playersTab = TAB_PLAYERS.equals(tab);
        boolean soundsTab = TAB_SOUNDS.equals(tab);
        boolean rewardsTab = TAB_REWARDS.equals(tab);
        boolean bossesTab = TAB_BOSSES.equals(tab);
        boolean arenasTab = TAB_ARENAS.equals(tab);
        boolean helpTab = TAB_HELP.equals(tab);

        this.setVisible(commandBuilder, generalTab, "#GeneralArenaLabel", "#GeneralArenaId", "#LanguageLabel", "#Language", "#AutoStartEnabledLabel", "#AutoStartEnabled", "#AutoStartIntervalLabel", "#AutoStartInterval", "#AutoStartApplyButton");
        this.setVisible(commandBuilder, hordeTab, "#RoleLabel", "#EnemyType", "#FinalBossLabel", "#FinalBossEnabled", "#RadiusLabel", "#MinRadiusLabel", "#MinRadius", "#MaxRadiusLabel", "#MaxRadius", "#RoundConfigLabel", "#RoundLabel", "#Rounds", "#WaveDelayLabel", "#WaveDelay", "#BaseEnemiesLabel", "#BaseEnemies", "#EnemiesPerRoundLabel", "#EnemiesPerRound");
        this.setVisible(commandBuilder, playersTab, "#AudienceInfoLabel", "#PlayersListTitle", "#PlayersCountLabel", "#PlayersCountValue", "#PlayersListHint", "#PlayersRefreshButton", "#PlayersHeaderName", "#PlayersHeaderMode", "#AudiencePlayersRows", "#AudiencePlayersEmptyLabel", "#AudienceHelpLabel", "#ArenaJoinRadiusLabel", "#ArenaJoinRadius");
        this.setVisible(commandBuilder, soundsTab, "#RoundStartSoundLabel", "#RoundStartSoundId", "#RoundStartVolumeLabel", "#RoundStartVolume", "#RoundVictorySoundLabel", "#RoundVictorySoundId", "#RoundVictoryVolumeLabel", "#RoundVictoryVolume");
        this.setVisible(commandBuilder, rewardsTab, "#RewardCategoryLabel", "#RewardCategory", "#RewardCommandsLabel", "#RewardItemId", "#RewardItemQuantityLabel", "#RewardItemQuantity");
        this.setVisible(commandBuilder, bossesTab, "#BossesTitleLabel", "#BossCreateIdLabel", "#BossCreateId", "#BossAddButton", "#BossHeaderName", "#BossHeaderNpc", "#BossHeaderTier", "#BossHeaderAmount", "#BossHeaderActions", "#BossPagePrevButton", "#BossPageNextButton", "#BossPageLabel", "#BossEmptyLabel", "#BossOverflowLabel", "#BossEditorTitleLabel", "#BossSelectedLabel", "#BossEditNameLabel", "#BossEditName", "#BossEditNpcIdLabel", "#BossEditNpcId", "#BossEditTierLabel", "#BossEditTier", "#BossEditAmountLabel", "#BossEditAmount", "#BossEditLevelOverrideLabel", "#BossEditLevelOverride", "#BossEditLootRadiusLabel", "#BossEditLootRadius", "#BossSpawnTriggerLabel", "#BossSpawnTrigger", "#BossSpawnTriggerValueLabel", "#BossSpawnTriggerValue", "#BossWaveRandomLocationsLabel", "#BossWaveRandomLocations", "#BossWaveRandomRadiusLabel", "#BossWaveRandomRadius", "#BossTimedProximityEnabledLabel", "#BossTimedProximityEnabled", "#BossTimedProximityArenaLabel", "#BossTimedProximityArena", "#BossTimedProximityRadiusLabel", "#BossTimedProximityRadius", "#BossTimedProximityCooldownLabel", "#BossTimedProximityCooldown", "#BossSaveButton", "#BossStatusLabel", "#BossRow1", "#BossRow2", "#BossRow3", "#BossRow4");
        this.setVisible(commandBuilder, arenasTab, "#ArenasTitleLabel", "#ArenaAddButton", "#ArenaHeaderName", "#ArenaHeaderCoords", "#ArenaHeaderActions", "#ArenaPagePrevButton", "#ArenaPageNextButton", "#ArenaPageLabel", "#ArenaEmptyLabel", "#ArenaOverflowLabel", "#ArenaEditorTitleLabel", "#ArenaSelectedLabel", "#ArenaSelected", "#ArenaEditIdLabel", "#ArenaEditId", "#ArenaEditXLabel", "#ArenaEditX", "#ArenaEditYLabel", "#ArenaEditY", "#ArenaEditZLabel", "#ArenaEditZ", "#ArenaUseCurrentPositionButton", "#ArenaSaveButton", "#ArenaRow1", "#ArenaRow2", "#ArenaRow3", "#ArenaRow4", "#ArenaRow5", "#ArenaRow6", "#ArenaRow7", "#ArenaRow8", "#ArenaRow9", "#ArenaRow10");
        this.setVisible(commandBuilder, helpTab, "#HelpIntroLabel", "#HelpCommandsLabel", "#HelpCommandsLine1", "#HelpCommandsLine2", "#HelpCommandsLine3", "#HelpConfigLabel", "#HelpConfigLine1", "#HelpConfigLine2", "#HelpConfigLine3", "#HelpExternalLabel", "#HelpExternalLine1", "#HelpExternalLine2", "#HelpExternalLine3", "#HelpReloadLabel", "#HelpReloadLine1", "#HelpReloadLine2");
        this.setVisible(commandBuilder, generalTab, "#TabGeneralActiveBack", "#TabGeneralActiveTop", "#TabGeneralActiveNotch");
        this.setVisible(commandBuilder, hordeTab, "#TabHordeActiveBack", "#TabHordeActiveTop", "#TabHordeActiveNotch");
        this.setVisible(commandBuilder, playersTab, "#TabPlayersActiveBack", "#TabPlayersActiveTop", "#TabPlayersActiveNotch");
        this.setVisible(commandBuilder, soundsTab, "#TabSoundsActiveBack", "#TabSoundsActiveTop", "#TabSoundsActiveNotch");
        this.setVisible(commandBuilder, rewardsTab, "#TabRewardsActiveBack", "#TabRewardsActiveTop", "#TabRewardsActiveNotch");
        this.setVisible(commandBuilder, bossesTab, "#TabBossesActiveBack", "#TabBossesActiveTop", "#TabBossesActiveNotch");
        this.setVisible(commandBuilder, arenasTab, "#TabArenasActiveBack", "#TabArenasActiveTop", "#TabArenasActiveNotch");
        this.setVisible(commandBuilder, helpTab, "#TabHelpActiveBack", "#TabHelpActiveTop", "#TabHelpActiveNotch");
        this.setVisible(commandBuilder, false, "#SubTitleLabel", "#TabHintLabel", "#StatusTitleLabel", "#StatusPanel", "#StatusLabel", "#SpawnStateLabel", "#SpawnLabel", "#SpawnX", "#SpawnY", "#SpawnZ", "#SetSpawnButton", "#ArenaStatusLabel", "#RoleHelpLabel", "#RoundSoundHelpLabel", "#RewardCommandsHelpLabel", "#PlayerMultiplierLabel", "#PlayerMultiplier", "#EnemyLevelRangeLabel", "#EnemyLevelWipLabel", "#EnemyLevelMin", "#EnemyLevelRangeSeparator", "#EnemyLevelMax", "#LanguagePrevButton", "#LanguageNextButton", "#FinalBossPrevButton", "#FinalBossNextButton", "#RoundStartSoundPrevButton", "#RoundStartSoundNextButton", "#RoundVictorySoundPrevButton", "#RoundVictorySoundNextButton", "#RewardCategoryPrevButton", "#RewardCategoryNextButton", "#RewardItemPrevButton", "#RewardItemNextButton", "#RewardEveryRoundsLabel", "#RewardEveryRounds", "#HelpDiscordButton", "#HelpCurseForgeButton");
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
            case TAB_REWARDS:
            case TAB_BOSSES:
            case TAB_ARENAS:
            case TAB_HELP: {
                return normalized;
            }
        }
        return TAB_GENERAL;
    }

    private static String audienceModeDisplay(String mode, String language) {
        boolean english = HordeService.isEnglishLanguage(language);
        String normalized = HordeConfigPage.normalizeAudienceMode(mode);
        if ("spectator".equals(normalized)) {
            return HordeConfigPage.t(language, english, "Spectator", "Espectador");
        }
        if ("exit".equals(normalized)) {
            return HordeConfigPage.t(language, english, "Exit area", "Salir del area");
        }
        return HordeConfigPage.t(language, english, "Player", "Jugador");
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

    private static String buildAudienceInfo(double arenaJoinRadius, int playersInArea, String language) {
        boolean english = HordeService.isEnglishLanguage(language);
        String englishText = String.format(Locale.ROOT, "Arena players radius: %.2f blocks", arenaJoinRadius);
        String spanishText = String.format(Locale.ROOT, "Radio de jugadores de arena: %.2f bloques", arenaJoinRadius);
        return HordeConfigPage.t(language, english, englishText, spanishText);
    }

    private static String buildAudienceRowsHint(int playersInArea, String language) {
        return "";
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static double toUiVolumePercent(double normalizedVolume) {
        if (Double.isNaN(normalizedVolume) || Double.isInfinite(normalizedVolume)) {
            return 100.0;
        }
        if (normalizedVolume <= 1.0) {
            return normalizedVolume * 100.0;
        }
        return normalizedVolume;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private static String extractActionArgument(String action) {
        if (action == null || action.isBlank()) {
            return "";
        }
        int separator = action.indexOf(':');
        if (separator < 0 || separator >= action.length() - 1) {
            return "";
        }
        return action.substring(separator + 1).trim();
    }

    private static String buildBossAction(String action, String bossId) {
        return "boss_" + action + ":" + HordeConfigPage.firstNonEmpty(bossId, "");
    }

    private static String buildArenaAction(String action, String arenaId) {
        return "arena_" + action + ":" + HordeConfigPage.firstNonEmpty(arenaId, "");
    }

    private static int maxPageIndex(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) {
            return 0;
        }
        return Math.max(0, (int)Math.ceil(totalItems / (double)pageSize) - 1);
    }

    private static List<String> collectArenaIds(List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows) {
        ArrayList<String> ids = new ArrayList<String>();
        if (rows == null) {
            return ids;
        }
        for (BossArenaCatalogService.ArenaDefinitionSnapshot row : rows) {
            if (row == null || row.arenaId == null || row.arenaId.isBlank() || HordeConfigPage.containsIgnoreCase(ids, row.arenaId)) {
                continue;
            }
            ids.add(row.arenaId);
        }
        return ids;
    }

    private static String firstArenaId(List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        for (BossArenaCatalogService.ArenaDefinitionSnapshot row : rows) {
            if (row != null && row.arenaId != null && !row.arenaId.isBlank()) {
                return row.arenaId;
            }
        }
        return "";
    }

    private static BossArenaCatalogService.BossDefinitionSnapshot findBossSnapshot(List<BossArenaCatalogService.BossDefinitionSnapshot> rows, String bossId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String requested = bossId == null ? "" : bossId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (BossArenaCatalogService.BossDefinitionSnapshot row : rows) {
            if (row == null || row.bossId == null) {
                continue;
            }
            if (row.bossId.equalsIgnoreCase(requested)) {
                return row;
            }
        }
        return null;
    }

    private static BossArenaCatalogService.ArenaDefinitionSnapshot findArenaSnapshot(List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows, String arenaId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String requested = arenaId == null ? "" : arenaId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (BossArenaCatalogService.ArenaDefinitionSnapshot row : rows) {
            if (row == null || row.arenaId == null) {
                continue;
            }
            if (row.arenaId.equalsIgnoreCase(requested)) {
                return row;
            }
        }
        return null;
    }

    private static int findBossIndex(List<BossArenaCatalogService.BossDefinitionSnapshot> rows, String bossId) {
        if (rows == null || rows.isEmpty() || bossId == null || bossId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); ++i) {
            BossArenaCatalogService.BossDefinitionSnapshot row = rows.get(i);
            if (row == null || row.bossId == null) {
                continue;
            }
            if (row.bossId.equalsIgnoreCase(bossId)) {
                return i;
            }
        }
        return -1;
    }

    private static int findArenaIndex(List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows, String arenaId) {
        if (rows == null || rows.isEmpty() || arenaId == null || arenaId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); ++i) {
            BossArenaCatalogService.ArenaDefinitionSnapshot row = rows.get(i);
            if (row == null || row.arenaId == null) {
                continue;
            }
            if (row.arenaId.equalsIgnoreCase(arenaId)) {
                return i;
            }
        }
        return -1;
    }

    private static String compactName(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Player";
        }
        String safe = value.trim();
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, Math.max(0, maxLength - 1)) + ".";
    }

    private static final class UiFieldBinding {
        private final String configKey;
        private final String payloadAlias;
        private final String uiValueSelector;
        private final String[] extraPayloadKeys;

        private UiFieldBinding(String configKey, String payloadAlias, String uiValueSelector, String ... extraPayloadKeys) {
            this.configKey = configKey;
            this.payloadAlias = payloadAlias;
            this.uiValueSelector = uiValueSelector;
            this.extraPayloadKeys = extraPayloadKeys;
        }
    }
}



