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
    // Keep in sync with src/main/resources/Common/UI/Custom/Pages/HordeConfigPage.ui.
    // If this path drifts, client can crash with:
    // "Could not find document ... for Custom UI Append command".
    // See docs/CUSTOM_UI_GUARDRAILS.md
    private static final String LAYOUT = "Pages/HordeConfigPage.ui";
    private static final String TAB_GENERAL = "general";
    private static final String TAB_HORDE = "horde";
    private static final String TAB_ENEMIES = "enemies";
    private static final String TAB_PLAYERS = "players";
    private static final String TAB_SOUNDS = "sounds";
    private static final String TAB_REWARDS = "rewards";
    private static final String TAB_BOSSES = "bosses";
    private static final String TAB_ARENAS = "arenas";
    private static final String TAB_HELP = "help";
    private static final int MAX_AUDIENCE_ROWS = Integer.MAX_VALUE;
    private static final int MAX_PLAYER_ROWS = 11;
    private static final int MAX_HORDE_ROWS = 11;
    private static final int MAX_ENEMY_CATEGORY_ROWS = 11;
    private static final int MAX_ENEMY_CATEGORY_EDITOR_ROLE_ROWS = 10;
    private static final int MAX_REWARD_CATEGORY_ROWS = 11;
    private static final int MAX_REWARD_CATEGORY_EDITOR_ITEM_ROWS = 10;
    private static final int MAX_BOSS_ROWS = 11;
    private static final String COMMON_LIST_ROW_LAYOUT = "Pages/HordeArenaRow.ui";
    private static final String ENEMY_ROLE_ROW_LAYOUT = "Pages/HordeEnemyRoleRow.ui";
    private static final String ENEMY_PICKER_ROW_LAYOUT = "Pages/HordeEnemyPickerRow.ui";
    private static final int ENEMY_PICKER_COLUMNS = 4;
    private static final int ARENA_ICON_PICKER_COLUMNS = 7;
    private static final String ARENA_ICON_PICKER_ROW_LAYOUT = "Pages/HordeArenaIconPickerRow.ui";
    private static final String DEFAULT_ARENA_ITEM_ICON_ID = "Ingredient_Bar_Gold";
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
            new UiFieldBinding("selectedBossId", "GeneralBossId", "#GeneralBossId.Value"),
            new UiFieldBinding("selectedHordeId", "GeneralHordeId", "#GeneralHordeId.Value"),
            new UiFieldBinding("hordeSelected", "HordeSelected", "#HordeSelected.Value"),
            new UiFieldBinding("hordeEditId", "HordeEditId", "#HordeEditId.Value"),
            new UiFieldBinding("enemyType", "EnemyType", "#EnemyType.Value", "role", "@Role", "Role"),
            new UiFieldBinding("enemyCategorySelected", "EnemyCatSelected", "#EnemyCatSelected.Value"),
            new UiFieldBinding("enemyCategoryEditId", "EnemyCatEditId", "#EnemyCatEditId.Value"),
            new UiFieldBinding("enemyCategoryEditRoles", "EnemyCatEditRoles", "#EnemyCatEditRoles.Value"),
            new UiFieldBinding("enemyCategoryRolePicker", "EnemyCatRolePicker", "#EnemyCatRolePicker.Value"),
            new UiFieldBinding("enemyCategoryEditIconItemId", "EnemyCatEditIconItemId", "#EnemyCatEditIconItemId.Value"),
            new UiFieldBinding("playerSelected", "PlayerSelected", "#PlayerSelected.Value"),
            new UiFieldBinding("playerEditMode", "PlayerEditMode", "#PlayerEditMode.Value"),
            new UiFieldBinding("rewardCatSelected", "RewardCatSelected", "#RewardCatSelected.Value"),
            new UiFieldBinding("rewardCatEditId", "RewardCatEditId", "#RewardCatEditId.Value"),
            new UiFieldBinding("rewardCatEditItems", "RewardCatEditItems", "#RewardCatEditItems.Value"),
            new UiFieldBinding("rewardCatItemPicker", "RewardCatItemPicker", "#RewardCatItemPicker.Value"),
            new UiFieldBinding("language", "Language", "#Language.Value"),
            new UiFieldBinding("rewardCategory", "RewardCategory", "#GeneralRewardId.Value"),
            new UiFieldBinding("rewardItemId", "RewardItemId", "#RewardItemId.Value"),
            new UiFieldBinding("rewardItemQuantity", "RewardItemQuantity", "#RewardItemQuantity.Value"),
            new UiFieldBinding("finalBossEnabled", "FinalBossEnabled", "#FinalBossEnabled.Value"),
            new UiFieldBinding("roundStartSoundId", "RoundStartSoundId", "#RoundStartSoundId.Value"),
            new UiFieldBinding("roundStartVolume", "RoundStartVolume", "#RoundStartVolume.Value"),
            new UiFieldBinding("roundVictorySoundId", "RoundVictorySoundId", "#RoundVictorySoundId.Value"),
            new UiFieldBinding("roundVictoryVolume", "RoundVictoryVolume", "#RoundVictoryVolume.Value"),
            new UiFieldBinding("bossSelected", "BossSelected", "#BossSelected.Value"),
            new UiFieldBinding("bossEditName", "BossEditName", "#BossEditName.Value"),
            new UiFieldBinding("bossEditNpcId", "BossEditNpcId", "#BossEditNpcId.Value"),
            new UiFieldBinding("bossEditTier", "BossEditTier", "#BossEditTier.Value"),
            new UiFieldBinding("bossEditAmount", "BossEditAmount", "#BossEditAmount.Value"),
            new UiFieldBinding("bossEditHp", "BossEditHp", "#BossEditHp.Value"),
            new UiFieldBinding("bossEditDamage", "BossEditDamage", "#BossEditDamage.Value"),
            new UiFieldBinding("bossEditSize", "BossEditSize", "#BossEditSize.Value"),
            new UiFieldBinding("bossEditAttackRate", "BossEditAttackRate", "#BossEditAttackRate.Value"),
            new UiFieldBinding("arenaSelected", "ArenaSelected", "#ArenaSelected.Value"),
            new UiFieldBinding("arenaEditId", "ArenaEditId", "#ArenaEditId.Value"),
            new UiFieldBinding("arenaEditIconItemId", "ArenaEditIconItemId", "#ArenaEditIconItemId.Value"),
            new UiFieldBinding("arenaEditX", "ArenaEditX", "#ArenaEditX.Value"),
            new UiFieldBinding("arenaEditY", "ArenaEditY", "#ArenaEditY.Value"),
            new UiFieldBinding("arenaEditZ", "ArenaEditZ", "#ArenaEditZ.Value")
    };
    private final HordeService hordeService;
    private final Map<String, String> draftValues;
    private String activeTab;
    private int playerPage;
    private int hordePage;
    private int enemyCategoryPage;
    private int enemyCategoryRolePage;
    private int rewardCategoryPage;
    private int rewardCategoryItemPage;
    private int bossPage;
    private String playerStatusText;
    private String hordeStatusText;
    private String enemyCategoryStatusText;
    private String rewardCategoryStatusText;
    private String bossStatusText;
    private String arenaStatusText;
    private boolean playerEditorModalVisible;
    private boolean enemyCategoryEditorModalVisible;
    private boolean rewardCategoryEditorModalVisible;
    private boolean hordeEditorModalVisible;
    private boolean bossEditorModalVisible;
    private boolean soundsEditorModalVisible;
    private boolean arenaEditorModalVisible;
    private boolean enemyCategoryEnemyPickerModalVisible;
    private boolean enemyCategoryIconPickerModalVisible;
    private boolean arenaIconPickerModalVisible;

    private HordeConfigPage(PlayerRef playerRef, HordeService hordeService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.hordeService = hordeService;
        this.draftValues = new HashMap<String, String>();
        this.activeTab = TAB_GENERAL;
        this.playerPage = 0;
        this.hordePage = 0;
        this.enemyCategoryPage = 0;
        this.enemyCategoryRolePage = 0;
        this.rewardCategoryPage = 0;
        this.rewardCategoryItemPage = 0;
        this.bossPage = 0;
        this.playerStatusText = "";
        this.hordeStatusText = "";
        this.enemyCategoryStatusText = "";
        this.rewardCategoryStatusText = "";
        this.bossStatusText = "";
        this.arenaStatusText = "";
        this.playerEditorModalVisible = false;
        this.enemyCategoryEditorModalVisible = false;
        this.rewardCategoryEditorModalVisible = false;
        this.hordeEditorModalVisible = false;
        this.bossEditorModalVisible = false;
        this.soundsEditorModalVisible = false;
        this.arenaEditorModalVisible = false;
        this.enemyCategoryEnemyPickerModalVisible = false;
        this.enemyCategoryIconPickerModalVisible = false;
        this.arenaIconPickerModalVisible = false;
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
        List<String> enemyRoleOptions = this.hordeService.getBossNpcIdOptions();
        List<String> languageOptions = this.hordeService.getLanguageOptions();
        List<String> roundStartSoundOptions = this.hordeService.getRoundStartSoundOptions();
        List<String> roundVictorySoundOptions = this.hordeService.getRoundVictorySoundOptions();
        List<String> rewardCategoryOptions = this.hordeService.getRewardCategoryOptions();
        List<String> rewardItemCatalogOptions = HordeConfigPage.filterRewardItemPickerOptions(this.hordeService.getRewardItemSuggestions((String)null));
        String enemyTypeValue = HordeConfigPage.normalizeEnemyTypeInput(this.getDraftValue("enemyType", config.enemyType == null ? "undead" : config.enemyType));
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
        double roundStartVolumeValue = HordeConfigPage.clamp(this.getDraftDouble("roundStartVolume", HordeConfigPage.toUiVolumePercent(config.roundStartVolume)), 0.0, 100.0);
        double roundVictoryVolumeValue = HordeConfigPage.clamp(this.getDraftDouble("roundVictoryVolume", HordeConfigPage.toUiVolumePercent(config.roundVictoryVolume)), 0.0, 100.0);
        boolean finalBossEnabledValue = this.getDraftBoolean("finalBossEnabled", config.finalBossEnabled);
        List<DropdownEntryInfo> enemyTypeEntries = HordeConfigPage.buildEnemyTypeEntries(enemyTypeOptions, enemyTypeValue, language, english);
        List<DropdownEntryInfo> languageEntries = HordeConfigPage.buildLanguageEntries(languageOptions, language);
        List<DropdownEntryInfo> roundStartSoundEntries = HordeConfigPage.buildRoundSoundEntries(roundStartSoundOptions, roundStartSoundValue, language, english);
        List<DropdownEntryInfo> roundVictorySoundEntries = HordeConfigPage.buildRoundSoundEntries(roundVictorySoundOptions, roundVictorySoundValue, language, english);
        String tab = HordeConfigPage.normalizeTab(this.activeTab);
        this.activeTab = tab;
        EntityStore entityStore = (EntityStore)store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        List<HordeService.AudiencePlayerSnapshot> audienceRows = world == null ? List.of() : this.hordeService.getArenaAudiencePlayers(world);
        List<HordeService.EnemyCategorySnapshot> enemyCategoryRows = this.hordeService.getEnemyCategoryDefinitionsSnapshot();
        List<HordeService.RewardCategorySnapshot> rewardCategoryRows = this.hordeService.getRewardCategoryDefinitionsSnapshot();
        List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> hordeRows = this.hordeService.getHordeDefinitionsSnapshot();
        List<BossArenaCatalogService.BossDefinitionSnapshot> bossRows = this.hordeService.getBossDefinitionsSnapshot();
        List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows = this.hordeService.getArenaDefinitionsSnapshot();
        this.ensurePlayerDraftDefaults(audienceRows);
        this.ensureEnemyCategoryDraftDefaults(enemyCategoryRows);
        this.ensureRewardCategoryDraftDefaults(rewardCategoryRows, rewardItemCatalogOptions);
        this.ensureHordeDraftDefaults(hordeRows, config);
        this.ensureBossDraftDefaults(bossRows);
        this.ensureArenaDraftDefaults(arenaRows);
        String selectedArenaForHordeValue = HordeConfigPage.firstNonEmpty(this.getDraftValue("selectedArenaId", ""), config.selectedArenaId, HordeConfigPage.firstArenaId(arenaRows));
        if (!selectedArenaForHordeValue.isBlank()) {
            this.draftValues.put("selectedArenaId", selectedArenaForHordeValue);
        }
        selectedArenaForHordeValue = this.getDraftValue("selectedArenaId", "");
        String selectedBossForHordeValue = HordeConfigPage.firstNonEmpty(this.getDraftValue("selectedBossId", ""), config.selectedBossId, HordeConfigPage.firstBossId(bossRows));
        if (!selectedBossForHordeValue.isBlank()) {
            this.draftValues.put("selectedBossId", selectedBossForHordeValue);
        }
        selectedBossForHordeValue = this.getDraftValue("selectedBossId", "");
        String selectedHordeForConfigValue = HordeConfigPage.firstNonEmpty(this.getDraftValue("selectedHordeId", ""), config.selectedHordeId, HordeConfigPage.firstHordeId(hordeRows));
        if (!selectedHordeForConfigValue.isBlank()) {
            this.draftValues.put("selectedHordeId", selectedHordeForConfigValue);
        }
        selectedHordeForConfigValue = this.getDraftValue("selectedHordeId", "");
        String selectedRewardCategoryForConfigValue = HordeConfigPage.firstNonEmpty(this.getDraftValue("rewardCategory", ""), config.rewardCategory, this.hordeService.getRewardCategory(), HordeConfigPage.firstEnemyRoleOption(rewardCategoryOptions), "");
        if (!selectedRewardCategoryForConfigValue.isBlank()) {
            this.draftValues.put("rewardCategory", selectedRewardCategoryForConfigValue);
        }
        selectedRewardCategoryForConfigValue = this.getDraftValue("rewardCategory", "");
        String bossSelectedValue = this.getDraftValue("bossSelected", "");
        String bossNpcIdValue = this.getDraftValue("bossEditNpcId", "");
        String arenaSelectedValue = this.getDraftValue("arenaSelected", "");
        String playerSelectedValue = this.getDraftValue("playerSelected", "");
        String playerEditModeValue = HordeConfigPage.normalizeAudienceMode(this.getDraftValue("playerEditMode", "player"));
        String hordeSelectedValue = this.getDraftValue("hordeSelected", "");
        String enemyCategorySelectedValue = this.getDraftValue("enemyCategorySelected", "");
        String enemyCategoryEditIdValue = this.getDraftValue("enemyCategoryEditId", enemyCategorySelectedValue);
        String enemyCategoryEditRolesValue = this.getDraftValue("enemyCategoryEditRoles", "");
        String enemyCategoryEditIconItemIdValue = this.getDraftValue("enemyCategoryEditIconItemId", HordeConfigPage.resolveEnemyCategoryIcon(enemyCategoryEditIdValue));
        String enemyCategoryRolePickerValue = this.getDraftValue("enemyCategoryRolePicker", HordeConfigPage.firstNonEmpty(HordeConfigPage.firstEnemyRoleOption(enemyRoleOptions), "enemy"));
        if (!enemyCategoryRolePickerValue.isBlank()) {
            this.draftValues.put("enemyCategoryRolePicker", enemyCategoryRolePickerValue);
        }
        String rewardCategorySelectedValue = this.getDraftValue("rewardCatSelected", "");
        String rewardCategoryEditIdValue = this.getDraftValue("rewardCatEditId", rewardCategorySelectedValue);
        String rewardCategoryEditItemsValue = this.getDraftValue("rewardCatEditItems", "");
        String rewardCategoryItemPickerValue = this.getDraftValue("rewardCatItemPicker", HordeConfigPage.firstNonEmpty(HordeConfigPage.firstEnemyRoleOption(rewardItemCatalogOptions), ""));
        if (!rewardCategoryItemPickerValue.isBlank()) {
            this.draftValues.put("rewardCatItemPicker", rewardCategoryItemPickerValue);
        }
        String bossTierValue = this.getDraftValue("bossEditTier", "common");
        List<DropdownEntryInfo> generalArenaEntries = HordeConfigPage.buildDropdownEntries(HordeConfigPage.collectArenaIds(arenaRows), selectedArenaForHordeValue);
        List<DropdownEntryInfo> generalBossEntries = HordeConfigPage.buildDropdownEntries(HordeConfigPage.collectBossIds(bossRows), selectedBossForHordeValue);
        List<DropdownEntryInfo> generalHordeEntries = HordeConfigPage.buildDropdownEntries(HordeConfigPage.collectHordeIds(hordeRows), selectedHordeForConfigValue);
        List<DropdownEntryInfo> generalRewardEntries = HordeConfigPage.buildRewardCategoryEntries(rewardCategoryOptions, selectedRewardCategoryForConfigValue, language, english);
        List<DropdownEntryInfo> playerModeEntries = HordeConfigPage.buildPlayerModeEntries(language, english, playerEditModeValue);
        List<DropdownEntryInfo> enemyCategoryRolePickerEntries = HordeConfigPage.buildDropdownEntries(enemyRoleOptions, enemyCategoryRolePickerValue);
        List<DropdownEntryInfo> rewardCategoryItemPickerEntries = HordeConfigPage.buildDropdownEntries(rewardItemCatalogOptions, rewardCategoryItemPickerValue);
        List<DropdownEntryInfo> bossNpcIdEntries = HordeConfigPage.buildDropdownEntries(this.hordeService.getBossNpcIdOptions(), bossNpcIdValue);
        List<DropdownEntryInfo> bossTierEntries = HordeConfigPage.buildDropdownEntries(this.hordeService.getBossTierOptions(), bossTierValue);
        this.playerPage = HordeConfigPage.clamp(this.playerPage, 0, HordeConfigPage.maxPageIndex(audienceRows == null ? 0 : audienceRows.size(), MAX_PLAYER_ROWS));
        this.enemyCategoryPage = HordeConfigPage.clamp(this.enemyCategoryPage, 0, HordeConfigPage.maxPageIndex(enemyCategoryRows == null ? 0 : enemyCategoryRows.size(), MAX_ENEMY_CATEGORY_ROWS));
        this.rewardCategoryPage = HordeConfigPage.clamp(this.rewardCategoryPage, 0, HordeConfigPage.maxPageIndex(rewardCategoryRows == null ? 0 : rewardCategoryRows.size(), MAX_REWARD_CATEGORY_ROWS));
        this.hordePage = HordeConfigPage.clamp(this.hordePage, 0, HordeConfigPage.maxPageIndex(hordeRows == null ? 0 : hordeRows.size(), MAX_HORDE_ROWS));
        this.bossPage = HordeConfigPage.clamp(this.bossPage, 0, HordeConfigPage.maxPageIndex(bossRows == null ? 0 : bossRows.size(), MAX_BOSS_ROWS));
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
                .set("#GeneralBossId.Value", selectedBossForHordeValue)
                .set("#GeneralBossId.Entries", generalBossEntries)
                .set("#GeneralHordeId.Value", selectedHordeForConfigValue)
                .set("#GeneralHordeId.Entries", generalHordeEntries)
                .set("#GeneralRewardId.Value", selectedRewardCategoryForConfigValue)
                .set("#GeneralRewardId.Entries", generalRewardEntries)
                .set("#HordeSelected.Value", hordeSelectedValue)
                .set("#HordeEditId.Value", this.getDraftValue("hordeEditId", hordeSelectedValue))
                .set("#MaxRadius.Value", this.getDraftValue("maxRadius", HordeConfigPage.formatDouble(maxRadiusValue)))
                .set("#Rounds.Value", this.getDraftValue("rounds", Integer.toString(roundsValue)))
                .set("#BaseEnemies.Value", this.getDraftValue("baseEnemies", Integer.toString(baseEnemiesValue)))
                .set("#EnemiesPerRound.Value", this.getDraftValue("enemiesPerRound", Integer.toString(enemiesPerRoundValue)))
                .set("#WaveDelay.Value", this.getDraftValue("waveDelay", Integer.toString(waveDelayValue)))
                .set("#HordeStatusLabel.Text", this.hordeStatusText == null ? "" : this.hordeStatusText)
                .set("#PlayerSelected.Value", playerSelectedValue)
                .set("#PlayerEditMode.Value", playerEditModeValue)
                .set("#PlayerEditMode.Entries", playerModeEntries)
                .set("#PlayerStatusLabel.Text", this.playerStatusText == null ? "" : this.playerStatusText)
                .set("#EnemyCatSelected.Value", enemyCategorySelectedValue)
                .set("#EnemyCatEditId.Value", enemyCategoryEditIdValue)
                .set("#EnemyCatEditRoles.Value", enemyCategoryEditRolesValue)
                .set("#EnemyCatEditIconItemId.Value", enemyCategoryEditIconItemIdValue)
                .set("#EnemyCatEditIconPreview.ItemId", enemyCategoryEditIconItemIdValue)
                .set("#EnemyCatEditIconCurrentLabel.Text", enemyCategoryEditIconItemIdValue)
                .set("#EnemyCatRolePicker.Value", enemyCategoryRolePickerValue)
                .set("#EnemyCatRolePicker.Entries", enemyCategoryRolePickerEntries)
                .set("#EnemyCatStatusLabel.Text", this.enemyCategoryStatusText == null ? "" : this.enemyCategoryStatusText)
                .set("#RewardCatSelected.Value", rewardCategorySelectedValue)
                .set("#RewardCatEditId.Value", rewardCategoryEditIdValue)
                .set("#RewardCatEditItems.Value", rewardCategoryEditItemsValue)
                .set("#RewardCatItemPicker.Value", rewardCategoryItemPickerValue)
                .set("#RewardCatItemPicker.Entries", rewardCategoryItemPickerEntries)
                .set("#AutoStartEnabled.Value", autoStartEnabledValue)
                .set("#AutoStartInterval.Value", Integer.toString(autoStartIntervalMinutesValue))
                .set("#EnemyType.Value", enemyTypeValue)
                .set("#EnemyType.Entries", enemyTypeEntries)
                .set("#Language.Value", language)
                .set("#Language.Entries", languageEntries)
                .set("#FinalBossEnabled.Value", finalBossEnabledValue)
                .set("#RoundStartSoundId.Value", roundStartSoundValue)
                .set("#RoundStartSoundId.Entries", roundStartSoundEntries)
                .set("#RoundVictorySoundId.Value", roundVictorySoundValue)
                .set("#RoundVictorySoundId.Entries", roundVictorySoundEntries)
                .set("#SoundRowStartId.Text", HordeConfigPage.firstNonEmpty(roundStartSoundValue, "-"))
                .set("#SoundRowStartVolume.Text", String.format(Locale.ROOT, "%.0f%%", roundStartVolumeValue))
                .set("#SoundRowVictoryId.Text", HordeConfigPage.firstNonEmpty(roundVictorySoundValue, "-"))
                .set("#SoundRowVictoryVolume.Text", String.format(Locale.ROOT, "%.0f%%", roundVictoryVolumeValue))
                .set("#EnemyLevelMin.Value", this.getDraftValue("enemyLevelMin", Integer.toString(config.enemyLevelMin)))
                .set("#EnemyLevelMax.Value", this.getDraftValue("enemyLevelMax", Integer.toString(config.enemyLevelMax)))
                .set("#BossSelected.Value", bossSelectedValue)
                .set("#BossEditName.Value", this.getDraftValue("bossEditName", bossSelectedValue))
                .set("#BossEditNpcId.Value", bossNpcIdValue)
                .set("#BossEditNpcId.Entries", bossNpcIdEntries)
                .set("#BossEditTier.Value", bossTierValue)
                .set("#BossEditTier.Entries", bossTierEntries)
                .set("#BossEditAmount.Value", this.getDraftValue("bossEditAmount", "1"))
                .set("#BossEditHp.Value", this.getDraftValue("bossEditHp", "1"))
                .set("#BossEditDamage.Value", this.getDraftValue("bossEditDamage", "1"))
                .set("#BossEditSize.Value", this.getDraftValue("bossEditSize", "1"))
                .set("#BossEditAttackRate.Value", this.getDraftValue("bossEditAttackRate", "1"))
                .set("#BossStatusLabel.Text", this.bossStatusText == null ? "" : this.bossStatusText)
                .set("#ArenaSelected.Value", arenaSelectedValue)
                .set("#ArenaEditId.Value", this.getDraftValue("arenaEditId", arenaSelectedValue))
                .set("#ArenaEditIconItemId.Value", this.getDraftValue("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID))
                .set("#ArenaEditIconPreview.ItemId", this.getDraftValue("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID))
                .set("#ArenaEditIconCurrentLabel.Text", this.getDraftValue("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID))
                .set("#ArenaEditX.Value", this.getDraftValue("arenaEditX", "0"))
                .set("#ArenaEditY.Value", this.getDraftValue("arenaEditY", "64"))
                .set("#ArenaEditZ.Value", this.getDraftValue("arenaEditZ", "0"))
                .set("#ArenaStatusLabel.Text", this.arenaStatusText == null ? "" : this.arenaStatusText)
                .set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config, language))
                .set("#ReloadModButton.Visible", true)
                .set("#StartButton.Visible", !active)
                .set("#StopButton.Visible", active)
                .set("#SkipRoundButton.Visible", active);
        this.setLocalizedTexts(commandBuilder, language, english);
        this.applyTabVisibility(commandBuilder, tab);
        if (TAB_PLAYERS.equals(tab)) {
            this.populatePlayerRows(commandBuilder, eventBuilder, audienceRows, language, english);
        }
        if (TAB_ENEMIES.equals(tab)) {
            this.populateEnemyCategoryRows(commandBuilder, eventBuilder, enemyCategoryRows, language, english, rewardItemCatalogOptions);
            this.populateEnemyCategoryEnemyPicker(commandBuilder, eventBuilder, enemyRoleOptions, language, english, rewardItemCatalogOptions);
            this.populateEnemyCategoryIconPicker(commandBuilder, eventBuilder, rewardItemCatalogOptions);
        }
        if (TAB_REWARDS.equals(tab)) {
            this.populateRewardCategoryRows(commandBuilder, eventBuilder, rewardCategoryRows, language, english);
        }
        if (TAB_HORDE.equals(tab)) {
            this.populateHordeRows(commandBuilder, eventBuilder, hordeRows, language, english);
        }
        if (TAB_BOSSES.equals(tab)) {
            this.populateBossRows(commandBuilder, eventBuilder, bossRows, language, english);
        }
        if (TAB_SOUNDS.equals(tab)) {
            this.populateSoundRows(commandBuilder, eventBuilder, roundStartSoundValue, roundStartVolumeValue, roundVictorySoundValue, roundVictoryVolumeValue, language, english);
        }
        if (TAB_ARENAS.equals(tab)) {
            this.populateArenaRows(commandBuilder, eventBuilder, arenaRows, language, english);
            this.populateArenaIconPicker(commandBuilder, eventBuilder, rewardItemCatalogOptions);
        }
        // IMPORTANT: Dropdowns like #Language must use ValueChanged.
        // Using Activating on dropdowns triggers client crash: "Failed to apply CustomUI event bindings".
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HordeCloseButton", EventData.of((String)"action", (String)"close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabGeneralButton", this.buildLanguageEvent("tab_general"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabArenasButton", this.buildLanguageEvent("tab_arenas"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabEnemiesButton", this.buildLanguageEvent("tab_enemies"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabHordeButton", this.buildLanguageEvent("tab_horde"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabBossesButton", this.buildLanguageEvent("tab_bosses"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabPlayersButton", this.buildLanguageEvent("tab_players"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabRewardsButton", this.buildLanguageEvent("tab_rewards"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabSoundsButton", this.buildLanguageEvent("tab_sounds"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#TabHelpButton", this.buildLanguageEvent("tab_help"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersAddButton", this.buildConfigSnapshotEvent("playerdef_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersSaveButton", this.buildConfigSnapshotEvent("playerdef_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersPagePrevButton", this.buildConfigSnapshotEvent("playerdef_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersPageNextButton", this.buildConfigSnapshotEvent("playerdef_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatAddButton", this.buildConfigSnapshotEvent("enemycat_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatSaveButton", this.buildConfigSnapshotEvent("enemycat_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatRoleAddButton", this.buildConfigSnapshotEvent("enemycat_role_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatEnemyPickerOpenButton", this.buildConfigSnapshotEvent("enemycat_enemy_picker_open"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatEnemyPickerCloseButton", this.buildConfigSnapshotEvent("enemycat_enemy_picker_close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatIconPickerOpenButton", this.buildConfigSnapshotEvent("enemycat_icon_picker_open"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatIconPickerCloseButton", this.buildConfigSnapshotEvent("enemycat_icon_picker_close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatAddButton", this.buildConfigSnapshotEvent("rewardcat_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatSaveButton", this.buildConfigSnapshotEvent("rewardcat_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatItemAddButton", this.buildConfigSnapshotEvent("rewardcat_item_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatPagePrevButton", this.buildConfigSnapshotEvent("rewardcat_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatPageNextButton", this.buildConfigSnapshotEvent("rewardcat_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatItemsPagePrevButton", this.buildConfigSnapshotEvent("rewardcat_items_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatItemsPageNextButton", this.buildConfigSnapshotEvent("rewardcat_items_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#HordeAddButton", this.buildConfigSnapshotEvent("hordedef_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#HordeSaveButton", this.buildConfigSnapshotEvent("hordedef_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#HordePagePrevButton", this.buildConfigSnapshotEvent("hordedef_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#HordePageNextButton", this.buildConfigSnapshotEvent("hordedef_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossAddButton", this.buildConfigSnapshotEvent("boss_add"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossSaveButton", this.buildConfigSnapshotEvent("boss_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossPagePrevButton", this.buildConfigSnapshotEvent("boss_page_prev"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossPageNextButton", this.buildConfigSnapshotEvent("boss_page_next"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaAddButton", this.buildConfigSnapshotEvent("arena_add_from_player"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaSaveButton", this.buildConfigSnapshotEvent("arena_save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaUseCurrentPositionButton", this.buildConfigSnapshotEvent("arena_use_current_position"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaEditorCloseButton", this.buildConfigSnapshotEvent("arena_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaIconPickerOpenButton", this.buildConfigSnapshotEvent("arena_icon_picker_open"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ArenaIconPickerCloseButton", this.buildConfigSnapshotEvent("arena_icon_picker_close"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#PlayersEditorCloseButton", this.buildConfigSnapshotEvent("playerdef_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#EnemyCatEditorCloseButton", this.buildConfigSnapshotEvent("enemycat_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatEditorCloseButton", this.buildConfigSnapshotEvent("rewardcat_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#HordeEditorCloseButton", this.buildConfigSnapshotEvent("hordedef_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#BossEditorCloseButton", this.buildConfigSnapshotEvent("boss_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SoundsEditorCloseButton", this.buildConfigSnapshotEvent("sounds_close_editor"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SoundRowStartOpen", this.buildConfigSnapshotEvent("sounds_open_start"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SoundRowVictoryOpen", this.buildConfigSnapshotEvent("sounds_open_victory"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#ReloadModButton", this.buildLanguageEvent("reload_config"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#AutoStartApplyButton", this.buildConfigSnapshotEvent("apply_auto_start"))
                .addEventBinding(CustomUIEventBindingType.ValueChanged, "#Language", this.buildLanguageEvent("set_language"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", this.buildConfigSnapshotEvent("save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SoundsSaveButton", this.buildConfigSnapshotEvent("save"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StartButton", this.buildConfigSnapshotEvent("start"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#StopButton", this.buildActionEventWithLanguage("stop"))
                .addEventBinding(CustomUIEventBindingType.Activating, "#SkipRoundButton", this.buildActionEventWithLanguage("skip_round"));
    }

    private void closeAllEditorModals() {
        this.playerEditorModalVisible = false;
        this.enemyCategoryEditorModalVisible = false;
        this.rewardCategoryEditorModalVisible = false;
        this.hordeEditorModalVisible = false;
        this.bossEditorModalVisible = false;
        this.soundsEditorModalVisible = false;
        this.arenaEditorModalVisible = false;
        this.enemyCategoryEnemyPickerModalVisible = false;
        this.enemyCategoryIconPickerModalVisible = false;
        this.arenaIconPickerModalVisible = false;
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        JsonObject payload;
        String language = HordeService.normalizeLanguage(this.hordeService.getLanguage());
        boolean english = HordeService.isEnglishLanguage(language);
        try {
            payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateUi(language, "Could not parse the UI event payload.", "No se pudo interpretar el evento de la UI.")));
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
                this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateUi(language, "Could not access the active world to process this UI action.", "No se pudo acceder al mundo actual para procesar la accion de UI.")));
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
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_horde": {
                    this.activeTab = TAB_HORDE;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_enemies": {
                    this.activeTab = TAB_ENEMIES;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_players": {
                    this.activeTab = TAB_PLAYERS;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_sounds": {
                    this.activeTab = TAB_SOUNDS;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_rewards": {
                    this.activeTab = TAB_REWARDS;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_bosses": {
                    this.activeTab = TAB_BOSSES;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_arenas": {
                    this.activeTab = TAB_ARENAS;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "tab_help": {
                    this.activeTab = TAB_HELP;
                    this.closeAllEditorModals();
                    tabSwitched = true;
                    break;
                }
                case "sounds_open_start":
                case "sounds_open_victory": {
                    this.soundsEditorModalVisible = true;
                    break;
                }
                case "sounds_close_editor": {
                    this.soundsEditorModalVisible = false;
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
                    if (action != null && action.startsWith("playerdef_")) {
                        result = this.handlePlayerDefinitionAction(action, world, english);
                        break;
                    }
                    if (action != null && action.startsWith("enemycat_")) {
                        result = this.handleEnemyCategoryAction(action, english);
                        break;
                    }
                    if (action != null && action.startsWith("rewardcat_")) {
                        result = this.handleRewardCategoryAction(action, english);
                        break;
                    }
                    if (action != null && action.startsWith("hordedef_")) {
                        result = this.handleHordeDefinitionAction(action, english);
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
                this.safeRebuild();
                return;
            }
            if ("start".equals(action) || "stop".equals(action) || "skip_round".equals(action)) {
                this.updateRuntimeControlsOnly(language);
                return;
            }
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)HordeI18n.translateUi(language, "Internal error while processing horde UI. Check server logs and try again.", "Error interno al procesar la UI de horda. Revisa logs e intenta de nuevo.")));
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
        if (action.startsWith("playerdef_save") || action.startsWith("playerdef_delete")) {
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

    private HordeService.OperationResult handlePlayerDefinitionAction(String action, World world, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown players action." : "Accion de jugadores desconocida.");
        }
        if ("playerdef_page_prev".equals(action)) {
            this.playerPage = Math.max(0, this.playerPage - 1);
            return null;
        }
        if ("playerdef_page_next".equals(action)) {
            this.playerPage = this.playerPage + 1;
            return null;
        }
        if ("playerdef_close_editor".equals(action)) {
            this.playerEditorModalVisible = false;
            return null;
        }
        List<HordeService.AudiencePlayerSnapshot> rows = world == null ? List.of() : this.hordeService.getArenaAudiencePlayers(world);
        if ("playerdef_add".equals(action)) {
            this.selectAudiencePlayerForEditing(rows, "");
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Players list refreshed." : "Lista de jugadores actualizada.");
            this.playerStatusText = result.getMessage();
            return result;
        }
        if ("playerdef_save".equals(action)) {
            String selected = this.getDraftValue("playerSelected", "");
            UUID playerId = HordeConfigPage.parseUuid(selected);
            if (playerId == null) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Select a player first." : "Selecciona primero un jugador.");
                this.playerStatusText = result.getMessage();
                return result;
            }
            String mode = HordeConfigPage.normalizeAudienceMode(this.getDraftValue("playerEditMode", "player"));
            HordeService.OperationResult result = this.hordeService.setArenaAudienceMode(playerId, mode, world);
            this.playerStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectAudiencePlayerForEditing(this.hordeService.getArenaAudiencePlayers(world), playerId.toString());
            }
            return result;
        }
        if (action.startsWith("playerdef_open:")) {
            String playerId = HordeConfigPage.extractActionArgument(action);
            this.selectAudiencePlayerForEditing(rows, playerId);
            this.playerEditorModalVisible = true;
            return null;
        }
        if (action.startsWith("playerdef_delete:")) {
            String playerIdRaw = HordeConfigPage.extractActionArgument(action);
            UUID playerId = HordeConfigPage.parseUuid(playerIdRaw);
            if (playerId == null) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Invalid player ID." : "ID de jugador invalido.");
                this.playerStatusText = result.getMessage();
                return result;
            }
            HordeService.OperationResult result = this.hordeService.setArenaAudienceMode(playerId, "exit", world);
            this.playerStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectAudiencePlayerForEditing(this.hordeService.getArenaAudiencePlayers(world), playerId.toString());
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown players action: " + action : "Accion de jugadores desconocida: " + action);
    }

    private HordeService.OperationResult handleEnemyCategoryAction(String action, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown enemy categories action." : "Accion de categorias de enemigos desconocida.");
        }
        if ("enemycat_page_prev".equals(action)) {
            this.enemyCategoryPage = Math.max(0, this.enemyCategoryPage - 1);
            return null;
        }
        if ("enemycat_page_next".equals(action)) {
            this.enemyCategoryPage = this.enemyCategoryPage + 1;
            return null;
        }
        if ("enemycat_close_editor".equals(action)) {
            this.enemyCategoryEditorModalVisible = false;
            this.enemyCategoryEnemyPickerModalVisible = false;
            this.enemyCategoryIconPickerModalVisible = false;
            return null;
        }
        if ("enemycat_roles_page_prev".equals(action)) {
            this.enemyCategoryRolePage = Math.max(0, this.enemyCategoryRolePage - 1);
            return null;
        }
        if ("enemycat_roles_page_next".equals(action)) {
            this.enemyCategoryRolePage = this.enemyCategoryRolePage + 1;
            return null;
        }
        if ("enemycat_icon_picker_open".equals(action)) {
            this.enemyCategoryIconPickerModalVisible = true;
            this.enemyCategoryEnemyPickerModalVisible = false;
            return null;
        }
        if ("enemycat_icon_picker_close".equals(action)) {
            this.enemyCategoryIconPickerModalVisible = false;
            return null;
        }
        if ("enemycat_enemy_picker_open".equals(action)) {
            this.enemyCategoryEnemyPickerModalVisible = true;
            this.enemyCategoryIconPickerModalVisible = false;
            return null;
        }
        if ("enemycat_enemy_picker_close".equals(action)) {
            this.enemyCategoryEnemyPickerModalVisible = false;
            return null;
        }
        if ("enemycat_add".equals(action)) {
            HordeService.OperationResult result = this.hordeService.createEnemyCategoryDraft("");
            if (result != null && result.isSuccess()) {
                this.selectEnemyCategoryForEditing("");
                this.enemyCategoryEditorModalVisible = true;
                this.enemyCategoryEnemyPickerModalVisible = false;
                this.enemyCategoryIconPickerModalVisible = false;
            }
            this.enemyCategoryStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("enemycat_role_add".equals(action)) {
            String candidateRole = this.getDraftValue("enemyCategoryRolePicker", "");
            if (candidateRole == null || candidateRole.isBlank()) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Select an Enemy ID first." : "Selecciona primero un Enemy ID.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            List<String> roles = HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("enemyCategoryEditRoles", ""));
            if (HordeConfigPage.containsIgnoreCase(roles, candidateRole)) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "That Enemy ID is already in the category." : "Ese Enemy ID ya esta en la categoria.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            roles.add(candidateRole.trim());
            this.draftValues.put("enemyCategoryEditRoles", HordeConfigPage.buildRolesCsv(roles));
            this.enemyCategoryRolePage = Math.max(0, (roles.size() - 1) / MAX_ENEMY_CATEGORY_EDITOR_ROLE_ROWS);
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Enemy ID added to category." : "Enemy ID anadido a la categoria.");
            this.enemyCategoryStatusText = result.getMessage();
            return result;
        }
        if (action.startsWith("enemycat_enemy_pick:")) {
            String pickedRoleId = HordeConfigPage.extractActionArgument(action);
            if (pickedRoleId == null || pickedRoleId.isBlank()) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Invalid Enemy ID." : "Enemy ID invalido.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            List<String> roles = HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("enemyCategoryEditRoles", ""));
            if (HordeConfigPage.containsIgnoreCase(roles, pickedRoleId)) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "That Enemy ID is already in the category." : "Ese Enemy ID ya esta en la categoria.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            roles.add(pickedRoleId.trim());
            this.draftValues.put("enemyCategoryEditRoles", HordeConfigPage.buildRolesCsv(roles));
            this.draftValues.put("enemyCategoryRolePicker", pickedRoleId.trim());
            this.enemyCategoryEnemyPickerModalVisible = false;
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Enemy ID added to category." : "Enemy ID anadido a la categoria.");
            this.enemyCategoryStatusText = result.getMessage();
            return result;
        }
        if (action.startsWith("enemycat_icon_pick:")) {
            String pickedItemId = HordeConfigPage.extractActionArgument(action);
            if (pickedItemId == null || pickedItemId.isBlank()) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Invalid icon item ID." : "Item ID de icono invalido.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            this.draftValues.put("enemyCategoryEditIconItemId", pickedItemId.trim());
            this.enemyCategoryIconPickerModalVisible = false;
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Category icon updated." : "Icono de categoria actualizado.");
            this.enemyCategoryStatusText = result.getMessage();
            return result;
        }
        if ("enemycat_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveEnemyCategoryFromUi(this.extractEnemyCategoryValuesForSave());
            if (result != null && result.isSuccess()) {
                this.selectEnemyCategoryForEditing(this.getDraftValue("enemyCategoryEditId", this.getDraftValue("enemyCategorySelected", "")));
                this.enemyCategoryEnemyPickerModalVisible = false;
                this.enemyCategoryIconPickerModalVisible = false;
            }
            this.enemyCategoryStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if (action.startsWith("enemycat_role_remove:")) {
            String roleId = HordeConfigPage.extractActionArgument(action);
            List<String> roles = HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("enemyCategoryEditRoles", ""));
            if (!HordeConfigPage.removeIgnoreCase(roles, roleId)) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Enemy ID not found in category." : "Enemy ID no encontrado en la categoria.");
                this.enemyCategoryStatusText = result.getMessage();
                return result;
            }
            this.draftValues.put("enemyCategoryEditRoles", HordeConfigPage.buildRolesCsv(roles));
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Enemy ID removed from category." : "Enemy ID eliminado de la categoria.");
            this.enemyCategoryStatusText = result.getMessage();
            return result;
        }
        if (action.startsWith("enemycat_open:")) {
            String categoryId = HordeConfigPage.extractActionArgument(action);
            this.selectEnemyCategoryForEditing(categoryId);
            this.enemyCategoryEditorModalVisible = true;
            this.enemyCategoryEnemyPickerModalVisible = false;
            this.enemyCategoryIconPickerModalVisible = false;
            return null;
        }
        if (action.startsWith("enemycat_delete:")) {
            String categoryId = HordeConfigPage.extractActionArgument(action);
            HordeService.OperationResult result = this.hordeService.deleteEnemyCategoryDefinition(categoryId);
            this.enemyCategoryStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectEnemyCategoryForEditing("");
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown enemy categories action: " + action : "Accion de categorias de enemigos desconocida: " + action);
    }

    private HordeService.OperationResult handleRewardCategoryAction(String action, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown rewards action." : "Accion de recompensas desconocida.");
        }
        if ("rewardcat_page_prev".equals(action)) {
            this.rewardCategoryPage = Math.max(0, this.rewardCategoryPage - 1);
            return null;
        }
        if ("rewardcat_page_next".equals(action)) {
            this.rewardCategoryPage = this.rewardCategoryPage + 1;
            return null;
        }
        if ("rewardcat_close_editor".equals(action)) {
            this.rewardCategoryEditorModalVisible = false;
            return null;
        }
        if ("rewardcat_items_page_prev".equals(action)) {
            this.rewardCategoryItemPage = Math.max(0, this.rewardCategoryItemPage - 1);
            return null;
        }
        if ("rewardcat_items_page_next".equals(action)) {
            this.rewardCategoryItemPage = this.rewardCategoryItemPage + 1;
            return null;
        }
        if ("rewardcat_add".equals(action)) {
            HordeService.OperationResult result = this.hordeService.createRewardCategoryDraft("");
            if (result != null && result.isSuccess()) {
                this.selectRewardCategoryForEditing("");
                this.rewardCategoryEditorModalVisible = true;
            }
            this.rewardCategoryStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("rewardcat_item_add".equals(action)) {
            String candidateItem = this.getDraftValue("rewardCatItemPicker", "");
            if (candidateItem == null || candidateItem.isBlank()) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Select a reward item first." : "Selecciona primero un item de recompensa.");
                this.rewardCategoryStatusText = result.getMessage();
                return result;
            }
            List<String> items = HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("rewardCatEditItems", ""));
            if (HordeConfigPage.containsIgnoreCase(items, candidateItem)) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "That item is already in the category." : "Ese item ya esta en la categoria.");
                this.rewardCategoryStatusText = result.getMessage();
                return result;
            }
            items.add(candidateItem.trim());
            this.draftValues.put("rewardCatEditItems", HordeConfigPage.buildRolesCsv(items));
            this.rewardCategoryItemPage = Math.max(0, (items.size() - 1) / MAX_REWARD_CATEGORY_EDITOR_ITEM_ROWS);
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Reward item added to category." : "Item de recompensa anadido a la categoria.");
            this.rewardCategoryStatusText = result.getMessage();
            return result;
        }
        if ("rewardcat_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveRewardCategoryFromUi(this.extractRewardCategoryValuesForSave());
            if (result != null && result.isSuccess()) {
                this.selectRewardCategoryForEditing(this.getDraftValue("rewardCatEditId", this.getDraftValue("rewardCatSelected", "")));
            }
            this.rewardCategoryStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if (action.startsWith("rewardcat_item_remove:")) {
            String itemId = HordeConfigPage.extractActionArgument(action);
            List<String> items = HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("rewardCatEditItems", ""));
            if (!HordeConfigPage.removeIgnoreCase(items, itemId)) {
                HordeService.OperationResult result = HordeService.OperationResult.fail(english ? "Item not found in category." : "Item no encontrado en la categoria.");
                this.rewardCategoryStatusText = result.getMessage();
                return result;
            }
            this.draftValues.put("rewardCatEditItems", HordeConfigPage.buildRolesCsv(items));
            HordeService.OperationResult result = HordeService.OperationResult.ok(english ? "Reward item removed from category." : "Item de recompensa eliminado de la categoria.");
            this.rewardCategoryStatusText = result.getMessage();
            return result;
        }
        if (action.startsWith("rewardcat_open:")) {
            String categoryId = HordeConfigPage.extractActionArgument(action);
            this.selectRewardCategoryForEditing(categoryId);
            this.rewardCategoryEditorModalVisible = true;
            return null;
        }
        if (action.startsWith("rewardcat_delete:")) {
            String categoryId = HordeConfigPage.extractActionArgument(action);
            HordeService.OperationResult result = this.hordeService.deleteRewardCategoryDefinition(categoryId);
            this.rewardCategoryStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectRewardCategoryForEditing("");
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown rewards action: " + action : "Accion de recompensas desconocida: " + action);
    }

    private HordeService.OperationResult handleHordeDefinitionAction(String action, boolean english) {
        if (action == null || action.isBlank()) {
            return HordeService.OperationResult.fail(english ? "Unknown horde action." : "Accion de hordas desconocida.");
        }
        if ("hordedef_page_prev".equals(action)) {
            this.hordePage = Math.max(0, this.hordePage - 1);
            return null;
        }
        if ("hordedef_page_next".equals(action)) {
            this.hordePage = this.hordePage + 1;
            return null;
        }
        if ("hordedef_close_editor".equals(action)) {
            this.hordeEditorModalVisible = false;
            return null;
        }
        if ("hordedef_add".equals(action)) {
            HordeService.OperationResult result = this.hordeService.createHordeDefinitionDraft("");
            if (result != null && result.isSuccess()) {
                this.selectHordeForEditing("");
                this.hordeEditorModalVisible = true;
            }
            this.hordeStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("hordedef_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveHordeDefinitionFromUi(this.extractHordeValuesForSave());
            if (result != null && result.isSuccess()) {
                this.selectHordeForEditing(this.getDraftValue("hordeEditId", this.getDraftValue("hordeSelected", "")));
            }
            this.hordeStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if (action.startsWith("hordedef_open:")) {
            String hordeId = HordeConfigPage.extractActionArgument(action);
            this.selectHordeForEditing(hordeId);
            this.hordeEditorModalVisible = true;
            return null;
        }
        if (action.startsWith("hordedef_delete:")) {
            String hordeId = HordeConfigPage.extractActionArgument(action);
            HordeService.OperationResult result = this.hordeService.deleteHordeDefinition(hordeId);
            this.hordeStatusText = result == null ? "" : result.getMessage();
            if (result != null && result.isSuccess()) {
                this.selectHordeForEditing("");
            }
            return result;
        }
        return HordeService.OperationResult.fail(english ? "Unknown horde action: " + action : "Accion de hordas desconocida: " + action);
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
        if ("boss_close_editor".equals(action)) {
            this.bossEditorModalVisible = false;
            return null;
        }
        if ("boss_add".equals(action)) {
            HordeService.OperationResult result = this.hordeService.createBossDraft("");
            if (result != null && result.isSuccess()) {
                this.selectBossForEditing("");
                this.bossEditorModalVisible = true;
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
            this.bossEditorModalVisible = true;
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
        if ("arena_close_editor".equals(action)) {
            this.arenaEditorModalVisible = false;
            this.arenaIconPickerModalVisible = false;
            return null;
        }
        if ("arena_icon_picker_open".equals(action)) {
            this.arenaIconPickerModalVisible = true;
            return null;
        }
        if ("arena_icon_picker_close".equals(action)) {
            this.arenaIconPickerModalVisible = false;
            return null;
        }
        if (action.startsWith("arena_icon_pick:")) {
            String itemId = HordeConfigPage.extractActionArgument(action);
            if (itemId != null && !itemId.isBlank()) {
                this.draftValues.put("arenaEditIconItemId", itemId);
            }
            this.arenaIconPickerModalVisible = false;
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
                this.arenaEditorModalVisible = true;
                this.arenaIconPickerModalVisible = false;
            }
            this.arenaStatusText = result == null ? "" : result.getMessage();
            return result;
        }
        if ("arena_save".equals(action)) {
            HordeService.OperationResult result = this.hordeService.saveArenaDefinitionFromUi(this.extractArenaValuesForSave(), fallbackWorldName);
            if (result != null && result.isSuccess()) {
                this.selectArenaForEditing(this.getDraftValue("arenaEditId", this.getDraftValue("arenaSelected", "")));
                this.arenaIconPickerModalVisible = false;
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
            this.arenaEditorModalVisible = true;
            this.arenaIconPickerModalVisible = false;
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
        this.putDraftIfMissing("selectedBossId", config.selectedBossId == null ? "" : config.selectedBossId.trim());
        this.putDraftIfMissing("selectedHordeId", config.selectedHordeId == null ? "" : config.selectedHordeId.trim());
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

    private void ensurePlayerDraftDefaults(List<HordeService.AudiencePlayerSnapshot> rows) {
        HordeService.AudiencePlayerSnapshot selected = HordeConfigPage.findAudienceSnapshot(rows, this.getDraftValue("playerSelected", ""));
        if (selected == null && rows != null && !rows.isEmpty()) {
            selected = rows.get(0);
        }
        if (selected != null) {
            this.putDraftIfMissing("playerSelected", selected.playerId == null ? "" : selected.playerId.toString());
            this.putDraftIfMissing("playerEditMode", HordeConfigPage.normalizeAudienceMode(selected.mode));
        } else {
            this.putDraftIfMissing("playerEditMode", "player");
        }
    }

    private void ensureEnemyCategoryDraftDefaults(List<HordeService.EnemyCategorySnapshot> rows) {
        HordeService.EnemyCategorySnapshot selectedCategory = HordeConfigPage.findEnemyCategorySnapshot(rows, this.getDraftValue("enemyCategorySelected", ""));
        if (selectedCategory == null && rows != null && !rows.isEmpty()) {
            selectedCategory = rows.get(0);
        }
        if (selectedCategory != null) {
            this.putDraftIfMissing("enemyCategorySelected", selectedCategory.categoryId);
            this.putDraftIfMissing("enemyCategoryEditId", selectedCategory.categoryId);
            this.putDraftIfMissing("enemyCategoryEditRoles", selectedCategory.rolesCsv);
            this.putDraftIfMissing("enemyCategoryEditIconItemId", HordeConfigPage.firstNonEmpty(selectedCategory.iconItemId, HordeConfigPage.resolveEnemyCategoryIcon(selectedCategory.categoryId)));
            this.putDraftIfMissing("enemyCategoryRolePicker", selectedCategory.roles == null || selectedCategory.roles.isEmpty() ? "enemy" : selectedCategory.roles.get(0));
        } else {
            this.putDraftIfMissing("enemyCategoryEditId", "enemy_category_1");
            this.putDraftIfMissing("enemyCategoryEditRoles", "enemy");
            this.putDraftIfMissing("enemyCategoryEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID);
            this.putDraftIfMissing("enemyCategoryRolePicker", "enemy");
        }
    }

    private void ensureRewardCategoryDraftDefaults(List<HordeService.RewardCategorySnapshot> rows, List<String> rewardItemCatalogOptions) {
        HordeService.RewardCategorySnapshot selectedCategory = HordeConfigPage.findRewardCategorySnapshot(rows, this.getDraftValue("rewardCatSelected", ""));
        if (selectedCategory == null && rows != null && !rows.isEmpty()) {
            selectedCategory = rows.get(0);
        }
        if (selectedCategory != null) {
            this.putDraftIfMissing("rewardCatSelected", selectedCategory.categoryId);
            this.putDraftIfMissing("rewardCatEditId", selectedCategory.categoryId);
            this.putDraftIfMissing("rewardCatEditItems", selectedCategory.itemsCsv);
            this.putDraftIfMissing("rewardCatItemPicker", selectedCategory.items == null || selectedCategory.items.isEmpty() ? HordeConfigPage.firstNonEmpty(HordeConfigPage.firstEnemyRoleOption(rewardItemCatalogOptions), "") : selectedCategory.items.get(0));
        } else {
            this.putDraftIfMissing("rewardCatEditId", "reward_category_1");
            this.putDraftIfMissing("rewardCatEditItems", HordeConfigPage.firstNonEmpty(HordeConfigPage.firstEnemyRoleOption(rewardItemCatalogOptions), "Item_Misc_Mushroom"));
            this.putDraftIfMissing("rewardCatItemPicker", HordeConfigPage.firstNonEmpty(HordeConfigPage.firstEnemyRoleOption(rewardItemCatalogOptions), "Item_Misc_Mushroom"));
        }
    }

    private void ensureHordeDraftDefaults(List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> hordeRows, HordeService.HordeConfig config) {
        String requestedSelection = this.getDraftValue("hordeSelected", "");
        HordeDefinitionCatalogService.HordeDefinitionSnapshot selectedHorde = HordeConfigPage.findHordeSnapshot(hordeRows, requestedSelection);
        boolean shouldSyncFromSelection = requestedSelection == null || requestedSelection.isBlank() || selectedHorde == null;
        if (selectedHorde == null && hordeRows != null && !hordeRows.isEmpty()) {
            selectedHorde = hordeRows.get(0);
        }
        if (selectedHorde != null) {
            if (shouldSyncFromSelection) {
                this.applyHordeDraftFromSnapshot(selectedHorde);
                return;
            }
            this.putDraftIfMissing("hordeSelected", selectedHorde.hordeId);
            this.putDraftIfMissing("hordeEditId", selectedHorde.hordeId);
            this.putDraftIfMissing("enemyType", selectedHorde.enemyType);
            this.putDraftIfMissing("minRadius", HordeConfigPage.formatDouble(selectedHorde.minRadius));
            this.putDraftIfMissing("maxRadius", HordeConfigPage.formatDouble(selectedHorde.maxRadius));
            this.putDraftIfMissing("rounds", Integer.toString(selectedHorde.rounds));
            this.putDraftIfMissing("baseEnemies", Integer.toString(selectedHorde.baseEnemies));
            this.putDraftIfMissing("enemiesPerRound", Integer.toString(selectedHorde.enemiesPerRound));
            this.putDraftIfMissing("waveDelay", Integer.toString(selectedHorde.waveDelay));
        } else {
            this.putDraftIfMissing("hordeEditId", "horde_1");
            this.putDraftIfMissing("enemyType", config.enemyType == null ? "undead" : config.enemyType);
            this.putDraftIfMissing("minRadius", HordeConfigPage.formatDouble(config.minSpawnRadius));
            this.putDraftIfMissing("maxRadius", HordeConfigPage.formatDouble(config.maxSpawnRadius));
            this.putDraftIfMissing("rounds", Integer.toString(config.rounds));
            this.putDraftIfMissing("baseEnemies", Integer.toString(config.baseEnemiesPerRound));
            this.putDraftIfMissing("enemiesPerRound", Integer.toString(config.enemiesPerRoundIncrement));
            this.putDraftIfMissing("waveDelay", Integer.toString(config.waveDelaySeconds));
        }
    }

    private void ensureBossDraftDefaults(List<BossArenaCatalogService.BossDefinitionSnapshot> bossRows) {
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
            this.putDraftIfMissing("bossEditHp", HordeConfigPage.formatDouble(selectedBoss.modifiers == null ? 1.0 : selectedBoss.modifiers.hp));
            this.putDraftIfMissing("bossEditDamage", HordeConfigPage.formatDouble(selectedBoss.modifiers == null ? 1.0 : selectedBoss.modifiers.damage));
            this.putDraftIfMissing("bossEditSize", HordeConfigPage.formatDouble(selectedBoss.modifiers == null ? 1.0 : selectedBoss.modifiers.size));
            this.putDraftIfMissing("bossEditAttackRate", HordeConfigPage.formatDouble(selectedBoss.modifiers == null ? 1.0 : selectedBoss.modifiers.attackRate));
        }
        this.putDraftIfMissing("bossEditTier", "common");
        this.putDraftIfMissing("bossEditAmount", "1");
        this.putDraftIfMissing("bossEditHp", "1");
        this.putDraftIfMissing("bossEditDamage", "1");
        this.putDraftIfMissing("bossEditSize", "1");
        this.putDraftIfMissing("bossEditAttackRate", "1");
    }

    private void ensureArenaDraftDefaults(List<BossArenaCatalogService.ArenaDefinitionSnapshot> arenaRows) {
        BossArenaCatalogService.ArenaDefinitionSnapshot selectedArena = HordeConfigPage.findArenaSnapshot(arenaRows, this.getDraftValue("arenaSelected", ""));
        if (selectedArena == null && arenaRows != null && !arenaRows.isEmpty()) {
            selectedArena = arenaRows.get(0);
        }
        if (selectedArena != null) {
            this.putDraftIfMissing("arenaSelected", selectedArena.arenaId);
            this.putDraftIfMissing("arenaEditId", selectedArena.arenaId);
            this.putDraftIfMissing("arenaEditIconItemId", HordeConfigPage.firstNonEmpty(selectedArena.iconItemId, DEFAULT_ARENA_ITEM_ICON_ID));
            this.putDraftIfMissing("arenaEditX", HordeConfigPage.formatDouble(selectedArena.x));
            this.putDraftIfMissing("arenaEditY", HordeConfigPage.formatDouble(selectedArena.y));
            this.putDraftIfMissing("arenaEditZ", HordeConfigPage.formatDouble(selectedArena.z));
        }
        this.putDraftIfMissing("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID);
        this.putDraftIfMissing("arenaEditX", "0");
        this.putDraftIfMissing("arenaEditY", "64");
        this.putDraftIfMissing("arenaEditZ", "0");
    }

    private void selectAudiencePlayerForEditing(List<HordeService.AudiencePlayerSnapshot> rows, String requestedPlayerId) {
        HordeService.AudiencePlayerSnapshot selected = HordeConfigPage.findAudienceSnapshot(rows, requestedPlayerId);
        if (selected == null && rows != null && !rows.isEmpty()) {
            selected = rows.get(rows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("playerSelected");
            this.draftValues.put("playerEditMode", "player");
            return;
        }
        this.applyAudienceDraftFromSnapshot(selected);
        int selectedIndex = HordeConfigPage.findAudienceIndex(rows, selected.playerId == null ? "" : selected.playerId.toString());
        if (selectedIndex >= 0) {
            this.playerPage = selectedIndex / MAX_PLAYER_ROWS;
        }
    }

    private void selectEnemyCategoryForEditing(String requestedCategoryId) {
        List<HordeService.EnemyCategorySnapshot> rows = this.hordeService.getEnemyCategoryDefinitionsSnapshot();
        HordeService.EnemyCategorySnapshot selected = HordeConfigPage.findEnemyCategorySnapshot(rows, requestedCategoryId);
        if (selected == null && rows != null && !rows.isEmpty()) {
            selected = rows.get(rows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("enemyCategorySelected");
            this.draftValues.remove("enemyCategoryEditId");
            this.draftValues.remove("enemyCategoryEditRoles");
            this.draftValues.remove("enemyCategoryEditIconItemId");
            this.enemyCategoryRolePage = 0;
            return;
        }
        this.applyEnemyCategoryDraftFromSnapshot(selected);
        this.enemyCategoryRolePage = 0;
        int selectedIndex = HordeConfigPage.findEnemyCategoryIndex(rows, selected.categoryId);
        if (selectedIndex >= 0) {
            this.enemyCategoryPage = selectedIndex / MAX_ENEMY_CATEGORY_ROWS;
        }
    }

    private void selectRewardCategoryForEditing(String requestedCategoryId) {
        List<HordeService.RewardCategorySnapshot> rows = this.hordeService.getRewardCategoryDefinitionsSnapshot();
        HordeService.RewardCategorySnapshot selected = HordeConfigPage.findRewardCategorySnapshot(rows, requestedCategoryId);
        if (selected == null && rows != null && !rows.isEmpty()) {
            selected = rows.get(rows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("rewardCatSelected");
            this.draftValues.remove("rewardCatEditId");
            this.draftValues.remove("rewardCatEditItems");
            this.rewardCategoryItemPage = 0;
            return;
        }
        this.applyRewardCategoryDraftFromSnapshot(selected);
        this.rewardCategoryItemPage = 0;
        int selectedIndex = HordeConfigPage.findRewardCategoryIndex(rows, selected.categoryId);
        if (selectedIndex >= 0) {
            this.rewardCategoryPage = selectedIndex / MAX_REWARD_CATEGORY_ROWS;
        }
    }

    private void selectHordeForEditing(String requestedHordeId) {
        List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> hordeRows = this.hordeService.getHordeDefinitionsSnapshot();
        HordeDefinitionCatalogService.HordeDefinitionSnapshot selected = HordeConfigPage.findHordeSnapshot(hordeRows, requestedHordeId);
        if (selected == null && hordeRows != null && !hordeRows.isEmpty()) {
            selected = hordeRows.get(hordeRows.size() - 1);
        }
        if (selected == null) {
            this.draftValues.remove("hordeSelected");
            this.draftValues.remove("hordeEditId");
            return;
        }
        this.applyHordeDraftFromSnapshot(selected);
        int selectedIndex = HordeConfigPage.findHordeIndex(hordeRows, selected.hordeId);
        if (selectedIndex >= 0) {
            this.hordePage = selectedIndex / MAX_HORDE_ROWS;
        }
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
            this.draftValues.remove("bossEditHp");
            this.draftValues.remove("bossEditDamage");
            this.draftValues.remove("bossEditSize");
            this.draftValues.remove("bossEditAttackRate");
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
            this.draftValues.remove("arenaEditIconItemId");
            this.draftValues.remove("arenaEditX");
            this.draftValues.remove("arenaEditY");
            this.draftValues.remove("arenaEditZ");
            return;
        }
        this.applyArenaDraftFromSnapshot(selected);
    }

    private void applyAudienceDraftFromSnapshot(HordeService.AudiencePlayerSnapshot snapshot) {
        if (snapshot == null || snapshot.playerId == null) {
            return;
        }
        this.draftValues.put("playerSelected", snapshot.playerId.toString());
        this.draftValues.put("playerEditMode", HordeConfigPage.normalizeAudienceMode(snapshot.mode));
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
        this.draftValues.put("bossEditHp", HordeConfigPage.formatDouble(snapshot.modifiers == null ? 1.0 : snapshot.modifiers.hp));
        this.draftValues.put("bossEditDamage", HordeConfigPage.formatDouble(snapshot.modifiers == null ? 1.0 : snapshot.modifiers.damage));
        this.draftValues.put("bossEditSize", HordeConfigPage.formatDouble(snapshot.modifiers == null ? 1.0 : snapshot.modifiers.size));
        this.draftValues.put("bossEditAttackRate", HordeConfigPage.formatDouble(snapshot.modifiers == null ? 1.0 : snapshot.modifiers.attackRate));
    }

    private void applyEnemyCategoryDraftFromSnapshot(HordeService.EnemyCategorySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("enemyCategorySelected", snapshot.categoryId);
        this.draftValues.put("enemyCategoryEditId", snapshot.categoryId);
        this.draftValues.put("enemyCategoryEditRoles", snapshot.rolesCsv == null ? "" : snapshot.rolesCsv);
        this.draftValues.put("enemyCategoryEditIconItemId", HordeConfigPage.firstNonEmpty(snapshot.iconItemId, HordeConfigPage.resolveEnemyCategoryIcon(snapshot.categoryId)));
        if (snapshot.roles != null && !snapshot.roles.isEmpty()) {
            this.draftValues.put("enemyCategoryRolePicker", snapshot.roles.get(0));
        }
    }

    private void applyRewardCategoryDraftFromSnapshot(HordeService.RewardCategorySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("rewardCatSelected", snapshot.categoryId);
        this.draftValues.put("rewardCatEditId", snapshot.categoryId);
        this.draftValues.put("rewardCatEditItems", snapshot.itemsCsv == null ? "" : snapshot.itemsCsv);
        if (snapshot.items != null && !snapshot.items.isEmpty()) {
            this.draftValues.put("rewardCatItemPicker", snapshot.items.get(0));
        }
    }

    private void applyHordeDraftFromSnapshot(HordeDefinitionCatalogService.HordeDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("hordeSelected", snapshot.hordeId);
        this.draftValues.put("hordeEditId", snapshot.hordeId);
        this.draftValues.put("enemyType", snapshot.enemyType == null ? "undead" : snapshot.enemyType);
        this.draftValues.put("minRadius", HordeConfigPage.formatDouble(snapshot.minRadius));
        this.draftValues.put("maxRadius", HordeConfigPage.formatDouble(snapshot.maxRadius));
        this.draftValues.put("rounds", Integer.toString(snapshot.rounds));
        this.draftValues.put("baseEnemies", Integer.toString(snapshot.baseEnemies));
        this.draftValues.put("enemiesPerRound", Integer.toString(snapshot.enemiesPerRound));
        this.draftValues.put("waveDelay", Integer.toString(snapshot.waveDelay));
    }

    private void applyArenaDraftFromSnapshot(BossArenaCatalogService.ArenaDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.draftValues.put("arenaSelected", snapshot.arenaId);
        this.draftValues.put("arenaEditId", snapshot.arenaId);
        this.draftValues.put("arenaEditIconItemId", HordeConfigPage.firstNonEmpty(snapshot.iconItemId, DEFAULT_ARENA_ITEM_ICON_ID));
        this.draftValues.put("arenaEditX", HordeConfigPage.formatDouble(snapshot.x));
        this.draftValues.put("arenaEditY", HordeConfigPage.formatDouble(snapshot.y));
        this.draftValues.put("arenaEditZ", HordeConfigPage.formatDouble(snapshot.z));
    }

    private Map<String, String> extractEnemyCategoryValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "enemyCategorySelected", this.getDraftValue("enemyCategorySelected", ""));
        HordeConfigPage.putIfNotBlank(values, "enemyCategoryEditId", this.getDraftValue("enemyCategoryEditId", ""));
        HordeConfigPage.putIfNotBlank(values, "enemyCategoryEditRoles", this.getDraftValue("enemyCategoryEditRoles", ""));
        HordeConfigPage.putIfNotBlank(values, "enemyCategoryEditIconItemId", this.getDraftValue("enemyCategoryEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID));
        return values;
    }

    private Map<String, String> extractRewardCategoryValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "rewardCatSelected", this.getDraftValue("rewardCatSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "rewardCatEditId", this.getDraftValue("rewardCatEditId", ""));
        HordeConfigPage.putIfNotBlank(values, "rewardCatEditItems", this.getDraftValue("rewardCatEditItems", ""));
        return values;
    }

    private Map<String, String> extractBossValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "bossSelected", this.getDraftValue("bossSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditName", this.getDraftValue("bossEditName", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditNpcId", this.getDraftValue("bossEditNpcId", ""));
        HordeConfigPage.putIfNotBlank(values, "bossEditTier", this.getDraftValue("bossEditTier", "common"));
        HordeConfigPage.putIfNotBlank(values, "bossEditAmount", this.getDraftValue("bossEditAmount", "1"));
        HordeConfigPage.putIfNotBlank(values, "bossEditHp", this.getDraftValue("bossEditHp", "1"));
        HordeConfigPage.putIfNotBlank(values, "bossEditDamage", this.getDraftValue("bossEditDamage", "1"));
        HordeConfigPage.putIfNotBlank(values, "bossEditSize", this.getDraftValue("bossEditSize", "1"));
        HordeConfigPage.putIfNotBlank(values, "bossEditAttackRate", this.getDraftValue("bossEditAttackRate", "1"));
        values.put("bossEditLevelOverride", "0");
        // Boss advanced trigger/reward fields were removed from UI.
        // Persist deterministic defaults so older rows don't keep stale values.
        values.put("bossEditLootRadius", "0");
        values.put("bossSpawnTrigger", "before_boss");
        values.put("bossSpawnTriggerValue", "0");
        values.put("bossWaveRandomLocations", "false");
        values.put("bossTimedProximityEnabled", "false");
        values.put("bossTimedProximityArena", "");
        values.put("bossTimedProximityRadius", "0");
        values.put("bossTimedProximityCooldown", "0");
        return values;
    }

    private Map<String, String> extractHordeValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "hordeSelected", this.getDraftValue("hordeSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "hordeEditId", this.getDraftValue("hordeEditId", ""));
        HordeConfigPage.putIfNotBlank(values, "enemyType", this.getDraftValue("enemyType", ""));
        values.put("minRadius", "1");
        HordeConfigPage.putIfNotBlank(values, "maxRadius", this.getDraftValue("maxRadius", ""));
        HordeConfigPage.putIfNotBlank(values, "rounds", this.getDraftValue("rounds", ""));
        HordeConfigPage.putIfNotBlank(values, "baseEnemies", this.getDraftValue("baseEnemies", ""));
        HordeConfigPage.putIfNotBlank(values, "enemiesPerRound", this.getDraftValue("enemiesPerRound", ""));
        HordeConfigPage.putIfNotBlank(values, "waveDelay", this.getDraftValue("waveDelay", ""));
        return values;
    }

    private Map<String, String> extractArenaValuesForSave() {
        HashMap<String, String> values = new HashMap<String, String>();
        HordeConfigPage.putIfNotBlank(values, "arenaSelected", this.getDraftValue("arenaSelected", ""));
        HordeConfigPage.putIfNotBlank(values, "arenaEditId", this.getDraftValue("arenaEditId", ""));
        HordeConfigPage.putIfNotBlank(values, "arenaEditIconItemId", this.getDraftValue("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID));
        HordeConfigPage.putIfNotBlank(values, "arenaEditX", this.getDraftValue("arenaEditX", "0"));
        HordeConfigPage.putIfNotBlank(values, "arenaEditY", this.getDraftValue("arenaEditY", "64"));
        HordeConfigPage.putIfNotBlank(values, "arenaEditZ", this.getDraftValue("arenaEditZ", "0"));
        return values;
    }

    private void populatePlayerRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeService.AudiencePlayerSnapshot> rows, String language, boolean english) {
        commandBuilder.clear("#PlayersRowsList");
        int total = rows == null ? 0 : rows.size();
        int renderedRows = 0;
        if (rows != null) {
            for (HordeService.AudiencePlayerSnapshot row : rows) {
                if (row == null || row.playerId == null) {
                    continue;
                }
                String rowSelector = "#PlayersRowsList[" + renderedRows + "]";
                String mode = HordeConfigPage.normalizeAudienceMode(row.mode);
                String iconItemId = HordeConfigPage.resolveAudienceModeIcon(mode);
                commandBuilder.append("#PlayersRowsList", COMMON_LIST_ROW_LAYOUT)
                        .set(rowSelector + " #ArenaName.Text", HordeConfigPage.compactName(row.username, 30))
                        .set(rowSelector + " #ArenaCoords.Text", HordeConfigPage.audienceModeDisplay(mode, language))
                        .set(rowSelector + " #ArenaIcon.ItemId", iconItemId)
                        .set(rowSelector + " #ArenaDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildPlayerDefinitionAction("open", row.playerId.toString())))
                        .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildPlayerDefinitionAction("delete", row.playerId.toString())));
                ++renderedRows;
            }
        }
        commandBuilder.set("#PlayersPageLabel.Visible", false)
                .set("#PlayersPagePrevButton.Visible", false)
                .set("#PlayersPageNextButton.Visible", false)
                .set("#PlayersEmptyLabel.Visible", renderedRows == 0)
                .set("#PlayersEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No players detected in the current arena radius.", "No hay jugadores detectados en el radio actual de arena.") : "")
                .set("#PlayersOverflowLabel.Visible", false)
                .set("#PlayersOverflowLabel.Text", "")
                .set("#PlayersCountValue.Text", Integer.toString(total));
    }

    private void populateEnemyCategoryRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeService.EnemyCategorySnapshot> rows, String language, boolean english, List<String> rewardItemCatalogOptions) {
        commandBuilder.clear("#EnemyCatRowsList");
        int renderedRows = 0;
        if (rows != null) {
            for (HordeService.EnemyCategorySnapshot row : rows) {
                if (row == null || row.categoryId == null || row.categoryId.isBlank()) {
                    continue;
                }
                String rowSelector = "#EnemyCatRowsList[" + renderedRows + "]";
                String preview = row.rolesPreview == null || row.rolesPreview.isBlank() ? HordeConfigPage.t(language, english, "No enemy IDs", "Sin enemy IDs") : HordeConfigPage.compactName(row.rolesPreview, 52);
                String iconItemId = HordeConfigPage.firstNonEmpty(row.iconItemId, HordeConfigPage.resolveEnemyCategoryIcon(row.categoryId));
                commandBuilder.append("#EnemyCatRowsList", COMMON_LIST_ROW_LAYOUT)
                        .set(rowSelector + " #ArenaName.Text", row.categoryId)
                        .set(rowSelector + " #ArenaCoords.Text", preview)
                        .set(rowSelector + " #ArenaIcon.ItemId", iconItemId)
                        .set(rowSelector + " #ArenaDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildEnemyCategoryAction("open", row.categoryId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildEnemyCategoryAction("delete", row.categoryId)));
                ++renderedRows;
            }
        }
        commandBuilder.set("#EnemyCatPageLabel.Visible", false)
                .set("#EnemyCatPagePrevButton.Visible", false)
                .set("#EnemyCatPageNextButton.Visible", false)
                .set("#EnemyCatEmptyLabel.Visible", renderedRows == 0)
                .set("#EnemyCatEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No enemy categories yet. Press Add category to create one.", "Aun no hay categorias de enemigos. Pulsa Anadir categoria para crear una.") : "")
                .set("#EnemyCatOverflowLabel.Visible", false)
                .set("#EnemyCatOverflowLabel.Text", "");
        this.populateEnemyCategoryEditorRoles(commandBuilder, eventBuilder, HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("enemyCategoryEditRoles", "")), language, english, this.enemyCategoryEditorModalVisible, rewardItemCatalogOptions);
    }

    private void populateEnemyCategoryEditorRoles(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<String> roles, String language, boolean english, boolean editorVisible, List<String> rewardItemCatalogOptions) {
        commandBuilder.clear("#EnemyCatRolesRowsList");
        int renderedRows = 0;
        if (editorVisible && roles != null) {
            for (String roleId : roles) {
                if (roleId == null || roleId.isBlank()) {
                    continue;
                }
                String rowSelector = "#EnemyCatRolesRowsList[" + renderedRows + "]";
                String iconItemId = HordeConfigPage.resolveEnemyRoleIcon(roleId, rewardItemCatalogOptions);
                commandBuilder.append("#EnemyCatRolesRowsList", ENEMY_ROLE_ROW_LAYOUT)
                        .set(rowSelector + " #RoleName.Text", HordeConfigPage.compactName(roleId, 44))
                        .set(rowSelector + " #RoleMeta.Text", HordeConfigPage.t(language, english, "Enemy ID in category", "Enemy ID en categoria"))
                        .set(rowSelector + " #RoleIcon.ItemId", iconItemId)
                        .set(rowSelector + " #RoleDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #RoleDeleteButton", this.buildConfigSnapshotEvent("enemycat_role_remove:" + HordeConfigPage.firstNonEmpty(roleId, "")));
                ++renderedRows;
            }
        }
        commandBuilder.set("#EnemyCatRolesEmptyLabel.Visible", editorVisible && renderedRows == 0)
                .set("#EnemyCatRolesEmptyLabel.Text", editorVisible && renderedRows == 0 ? HordeConfigPage.t(language, english, "No Enemy IDs in this category yet.", "No hay Enemy IDs en esta categoria todavia.") : "")
                .set("#EnemyCatRolesOverflowLabel.Visible", false)
                .set("#EnemyCatRolesOverflowLabel.Text", "");
    }

    private void populateEnemyCategoryIconPicker(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<String> rewardItemCatalogOptions) {
        String selectedIconItemId = HordeConfigPage.firstNonEmpty(
                this.getDraftValue("enemyCategoryEditIconItemId", ""),
                HordeConfigPage.resolveEnemyCategoryIcon(this.getDraftValue("enemyCategoryEditId", this.getDraftValue("enemyCategorySelected", ""))),
                DEFAULT_ARENA_ITEM_ICON_ID
        );
        this.draftValues.put("enemyCategoryEditIconItemId", selectedIconItemId);
        commandBuilder.set("#EnemyCatEditIconItemId.Value", selectedIconItemId)
                .set("#EnemyCatEditIconPreview.ItemId", selectedIconItemId)
                .set("#EnemyCatEditIconCurrentLabel.Text", selectedIconItemId);

        boolean pickerVisible = this.enemyCategoryEditorModalVisible && this.enemyCategoryIconPickerModalVisible;
        commandBuilder.set("#EnemyCatIconPickerShade.Visible", pickerVisible)
                .set("#EnemyCatIconPickerFrame.Visible", pickerVisible)
                .set("#EnemyCatIconPickerCloseButton.Visible", pickerVisible)
                .set("#EnemyCatIconPickerTitleLabel.Visible", pickerVisible)
                .set("#EnemyCatIconPickerGrid.Visible", pickerVisible);
        commandBuilder.clear("#EnemyCatIconPickerGrid");
        if (!pickerVisible) {
            return;
        }

        List<String> iconOptions = HordeConfigPage.buildArenaIconPickerOptions(rewardItemCatalogOptions, selectedIconItemId);
        int rows = (iconOptions.size() + ARENA_ICON_PICKER_COLUMNS - 1) / ARENA_ICON_PICKER_COLUMNS;
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            commandBuilder.append("#EnemyCatIconPickerGrid", ARENA_ICON_PICKER_ROW_LAYOUT);
            String rowSelector = "#EnemyCatIconPickerGrid[" + rowIndex + "]";
            for (int column = 0; column < ARENA_ICON_PICKER_COLUMNS; ++column) {
                int iconIndex = rowIndex * ARENA_ICON_PICKER_COLUMNS + column;
                int slotNumber = column + 1;
                String buttonSelector = rowSelector + " #IconPickButton" + slotNumber;
                String iconSelector = rowSelector + " #IconPickIcon" + slotNumber;
                if (iconIndex < iconOptions.size()) {
                    String option = iconOptions.get(iconIndex);
                    commandBuilder.set(buttonSelector + ".Visible", true)
                            .set(iconSelector + ".Visible", true)
                            .set(iconSelector + ".ItemId", option);
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, buttonSelector, this.buildConfigSnapshotEvent(HordeConfigPage.buildEnemyCategoryAction("icon_pick", option)));
                    continue;
                }
                commandBuilder.set(buttonSelector + ".Visible", false)
                        .set(iconSelector + ".Visible", false);
            }
        }
    }

    private void populateEnemyCategoryEnemyPicker(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<String> enemyRoleOptions, String language, boolean english, List<String> rewardItemCatalogOptions) {
        boolean pickerVisible = this.enemyCategoryEditorModalVisible && this.enemyCategoryEnemyPickerModalVisible;
        commandBuilder.set("#EnemyCatEnemyPickerShade.Visible", pickerVisible)
                .set("#EnemyCatEnemyPickerFrame.Visible", pickerVisible)
                .set("#EnemyCatEnemyPickerCloseButton.Visible", pickerVisible)
                .set("#EnemyCatEnemyPickerTitleLabel.Visible", pickerVisible)
                .set("#EnemyCatEnemyPickerGrid.Visible", pickerVisible);
        commandBuilder.clear("#EnemyCatEnemyPickerGrid");
        if (!pickerVisible) {
            return;
        }

        List<String> options = HordeConfigPage.buildEnemyRolePickerOptions(enemyRoleOptions);
        int rows = (options.size() + ENEMY_PICKER_COLUMNS - 1) / ENEMY_PICKER_COLUMNS;
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            commandBuilder.append("#EnemyCatEnemyPickerGrid", ENEMY_PICKER_ROW_LAYOUT);
            String rowSelector = "#EnemyCatEnemyPickerGrid[" + rowIndex + "]";
            for (int column = 0; column < ENEMY_PICKER_COLUMNS; ++column) {
                int optionIndex = rowIndex * ENEMY_PICKER_COLUMNS + column;
                int slot = column + 1;
                String buttonSelector = rowSelector + " #EnemyPickButton" + slot;
                String iconSelector = rowSelector + " #EnemyPickIcon" + slot;
                String labelSelector = rowSelector + " #EnemyPickLabel" + slot;
                if (optionIndex < options.size()) {
                    String enemyRoleId = options.get(optionIndex);
                    commandBuilder.set(buttonSelector + ".Visible", true)
                            .set(iconSelector + ".Visible", true)
                            .set(labelSelector + ".Visible", true)
                            .set(iconSelector + ".ItemId", HordeConfigPage.resolveEnemyRoleIcon(enemyRoleId, rewardItemCatalogOptions))
                            .set(labelSelector + ".Text", HordeConfigPage.compactName(enemyRoleId, 22));
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, buttonSelector, this.buildConfigSnapshotEvent(HordeConfigPage.buildEnemyCategoryAction("enemy_pick", enemyRoleId)));
                    continue;
                }
                commandBuilder.set(buttonSelector + ".Visible", false)
                        .set(iconSelector + ".Visible", false)
                        .set(labelSelector + ".Visible", false)
                        .set(labelSelector + ".Text", "");
            }
        }
    }

    private void populateRewardCategoryRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeService.RewardCategorySnapshot> rows, String language, boolean english) {
        commandBuilder.clear("#RewardCatRowsList");
        int renderedRows = 0;
        if (rows != null) {
            for (HordeService.RewardCategorySnapshot row : rows) {
                if (row == null || row.categoryId == null || row.categoryId.isBlank()) {
                    continue;
                }
                String rowSelector = "#RewardCatRowsList[" + renderedRows + "]";
                String preview = row.itemsPreview == null || row.itemsPreview.isBlank() ? HordeConfigPage.t(language, english, "No items", "Sin items") : HordeConfigPage.compactName(row.itemsPreview, 52);
                String firstItem = row.items == null || row.items.isEmpty() ? DEFAULT_ARENA_ITEM_ICON_ID : row.items.get(0);
                String iconItemId = HordeConfigPage.resolveListIconCandidate(firstItem);
                commandBuilder.append("#RewardCatRowsList", COMMON_LIST_ROW_LAYOUT)
                        .set(rowSelector + " #ArenaName.Text", row.categoryId)
                        .set(rowSelector + " #ArenaCoords.Text", preview)
                        .set(rowSelector + " #ArenaIcon.ItemId", iconItemId)
                        .set(rowSelector + " #ArenaDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildRewardCategoryAction("open", row.categoryId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildRewardCategoryAction("delete", row.categoryId)));
                ++renderedRows;
            }
        }
        commandBuilder.set("#RewardCatPageLabel.Visible", false)
                .set("#RewardCatPagePrevButton.Visible", false)
                .set("#RewardCatPageNextButton.Visible", false)
                .set("#RewardCatEmptyLabel.Visible", renderedRows == 0)
                .set("#RewardCatEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No reward categories yet. Press Add category to create one.", "Aun no hay categorias de recompensa. Pulsa Anadir categoria para crear una.") : "")
                .set("#RewardCatOverflowLabel.Visible", false)
                .set("#RewardCatOverflowLabel.Text", "");
        this.populateRewardCategoryEditorItems(commandBuilder, eventBuilder, HordeConfigPage.parseEnemyCategoryRolesCsv(this.getDraftValue("rewardCatEditItems", "")), language, english, this.rewardCategoryEditorModalVisible);
    }

    private void populateRewardCategoryEditorItems(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<String> items, String language, boolean english, boolean editorVisible) {
        int total = items == null ? 0 : items.size();
        int pageCount = Math.max(1, (int)Math.ceil(total / (double)MAX_REWARD_CATEGORY_EDITOR_ITEM_ROWS));
        this.rewardCategoryItemPage = HordeConfigPage.clamp(this.rewardCategoryItemPage, 0, pageCount - 1);
        int start = this.rewardCategoryItemPage * MAX_REWARD_CATEGORY_EDITOR_ITEM_ROWS;
        for (int slot = 0; slot < MAX_REWARD_CATEGORY_EDITOR_ITEM_ROWS; ++slot) {
            int index = start + slot;
            int rowNumber = slot + 1;
            String rowSelector = "#RewardCatItemRow" + rowNumber;
            if (editorVisible && index < total) {
                String itemId = items.get(index);
                commandBuilder.set(rowSelector + ".Visible", true)
                        .set("#RewardCatItemName" + rowNumber + ".Text", itemId == null ? "" : HordeConfigPage.compactName(itemId, 28));
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RewardCatItemDelete" + rowNumber, this.buildConfigSnapshotEvent("rewardcat_item_remove:" + HordeConfigPage.firstNonEmpty(itemId, "")));
            } else {
                commandBuilder.set(rowSelector + ".Visible", false)
                        .set("#RewardCatItemName" + rowNumber + ".Text", "");
            }
        }
        String pageText = total <= 0 ? "0/0" : (this.rewardCategoryItemPage + 1) + "/" + pageCount;
        commandBuilder.set("#RewardCatItemsPageLabel.Text", pageText)
                .set("#RewardCatItemsPageLabel.Visible", editorVisible && pageCount > 1)
                .set("#RewardCatItemsPagePrevButton.Visible", editorVisible && pageCount > 1)
                .set("#RewardCatItemsPageNextButton.Visible", editorVisible && pageCount > 1)
                .set("#RewardCatItemsOverflowLabel.Visible", false)
                .set("#RewardCatItemsOverflowLabel.Text", "");
    }

    private void populateHordeRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> rows, String language, boolean english) {
        commandBuilder.clear("#HordeRowsList");
        int renderedRows = 0;
        if (rows != null) {
            for (HordeDefinitionCatalogService.HordeDefinitionSnapshot row : rows) {
                if (row == null || row.hordeId == null || row.hordeId.isBlank()) {
                    continue;
                }
                String rowSelector = "#HordeRowsList[" + renderedRows + "]";
                String enemyType = HordeConfigPage.firstNonEmpty(row.enemyType, "-");
                String subtitle = HordeConfigPage.t(language, english, "Type", "Tipo") + ": " + HordeConfigPage.compactName(enemyType, 20) + "  |  " + HordeConfigPage.t(language, english, "Rounds", "Rondas") + ": " + row.rounds;
                String iconItemId = HordeConfigPage.resolveEnemyCategoryIcon(enemyType);
                commandBuilder.append("#HordeRowsList", COMMON_LIST_ROW_LAYOUT)
                        .set(rowSelector + " #ArenaName.Text", row.hordeId)
                        .set(rowSelector + " #ArenaCoords.Text", subtitle)
                        .set(rowSelector + " #ArenaIcon.ItemId", iconItemId)
                        .set(rowSelector + " #ArenaDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildHordeDefinitionAction("open", row.hordeId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildHordeDefinitionAction("delete", row.hordeId)));
                ++renderedRows;
            }
        }
        commandBuilder.set("#HordePageLabel.Visible", false)
                .set("#HordePagePrevButton.Visible", false)
                .set("#HordePageNextButton.Visible", false)
                .set("#HordeEmptyLabel.Visible", renderedRows == 0)
                .set("#HordeEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No horde definitions yet. Press Add horde to create one.", "Aun no hay definiciones de horda. Pulsa Anadir horda para crear una.") : "")
                .set("#HordeOverflowLabel.Visible", false)
                .set("#HordeOverflowLabel.Text", "");
    }

    private void populateBossRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<BossArenaCatalogService.BossDefinitionSnapshot> rows, String language, boolean english) {
        commandBuilder.clear("#BossRowsList");
        int renderedRows = 0;
        if (rows != null) {
            for (BossArenaCatalogService.BossDefinitionSnapshot row : rows) {
                if (row == null || row.bossId == null || row.bossId.isBlank()) {
                    continue;
                }
                String rowSelector = "#BossRowsList[" + renderedRows + "]";
                String tierText = HordeConfigPage.compactName(HordeConfigPage.firstNonEmpty(row.tier, "common"), 10);
                String npcText = HordeConfigPage.compactName(HordeConfigPage.firstNonEmpty(row.npcId, "-"), 28);
                String subtitle = npcText + "  |  " + tierText + "  x" + Math.max(1, row.amount);
                String iconItemId = HordeConfigPage.resolveBossTierIcon(row.tier);
                commandBuilder.append("#BossRowsList", COMMON_LIST_ROW_LAYOUT)
                        .set(rowSelector + " #ArenaName.Text", row.bossId)
                        .set(rowSelector + " #ArenaCoords.Text", subtitle)
                        .set(rowSelector + " #ArenaIcon.ItemId", iconItemId)
                        .set(rowSelector + " #ArenaDeleteButton.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildBossAction("open", row.bossId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, rowSelector + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildBossAction("delete", row.bossId)));
                ++renderedRows;
            }
        }
        commandBuilder.set("#BossPageLabel.Visible", false)
                .set("#BossPagePrevButton.Visible", false)
                .set("#BossPageNextButton.Visible", false)
                .set("#BossEmptyLabel.Visible", renderedRows == 0)
                .set("#BossEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No bosses yet. Press Add Boss to create one.", "Aun no hay bosses. Pulsa Anadir Boss para crear uno.") : "")
                .set("#BossOverflowLabel.Visible", false)
                .set("#BossOverflowLabel.Text", "");
    }

    private void populateSoundRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, String roundStartSoundValue, double roundStartVolumeValue, String roundVictorySoundValue, double roundVictoryVolumeValue, String language, boolean english) {
        commandBuilder.clear("#SoundsRowsList");

        commandBuilder.append("#SoundsRowsList", COMMON_LIST_ROW_LAYOUT)
                .set("#SoundsRowsList[0] #ArenaName.Text", HordeConfigPage.t(language, english, "Round start", "Inicio ronda"))
                .set("#SoundsRowsList[0] #ArenaCoords.Text", HordeConfigPage.firstNonEmpty(roundStartSoundValue, "-") + "  |  " + String.format(Locale.ROOT, "%.0f%%", roundStartVolumeValue))
                .set("#SoundsRowsList[0] #ArenaIcon.ItemId", "Weapon_Wand_Wood")
                .set("#SoundsRowsList[0] #ArenaDeleteButton.Visible", false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SoundsRowsList[0] #ArenaOpenButton", this.buildConfigSnapshotEvent("sounds_open_start"));

        commandBuilder.append("#SoundsRowsList", COMMON_LIST_ROW_LAYOUT)
                .set("#SoundsRowsList[1] #ArenaName.Text", HordeConfigPage.t(language, english, "Round victory", "Victoria ronda"))
                .set("#SoundsRowsList[1] #ArenaCoords.Text", HordeConfigPage.firstNonEmpty(roundVictorySoundValue, "-") + "  |  " + String.format(Locale.ROOT, "%.0f%%", roundVictoryVolumeValue))
                .set("#SoundsRowsList[1] #ArenaIcon.ItemId", "Potion_Signature_Greater")
                .set("#SoundsRowsList[1] #ArenaDeleteButton.Visible", false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SoundsRowsList[1] #ArenaOpenButton", this.buildConfigSnapshotEvent("sounds_open_victory"));

        commandBuilder.set("#SoundsEmptyLabel.Visible", false)
                .set("#SoundsEmptyLabel.Text", "")
                .set("#SoundsOverflowLabel.Visible", false)
                .set("#SoundsOverflowLabel.Text", "");
    }

    private void populateArenaRows(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<BossArenaCatalogService.ArenaDefinitionSnapshot> rows, String language, boolean english) {
        commandBuilder.clear("#ArenaRowsList");
        int renderedRows = 0;
        if (rows != null) {
            for (BossArenaCatalogService.ArenaDefinitionSnapshot row : rows) {
                if (row == null || row.arenaId == null || row.arenaId.isBlank()) {
                    continue;
                }
                String selectorPrefix = "#ArenaRowsList[" + renderedRows + "]";
                String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", row.x, row.y, row.z);
                String iconItemId = HordeConfigPage.firstNonEmpty(row.iconItemId, DEFAULT_ARENA_ITEM_ICON_ID);
                commandBuilder.append("#ArenaRowsList", "Pages/HordeArenaRow.ui")
                        .set(selectorPrefix + " #ArenaName.Text", row.arenaId)
                        .set(selectorPrefix + " #ArenaCoords.Text", coords)
                        .set(selectorPrefix + " #ArenaIcon.ItemId", iconItemId);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selectorPrefix + " #ArenaOpenButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildArenaAction("open", row.arenaId)))
                        .addEventBinding(CustomUIEventBindingType.Activating, selectorPrefix + " #ArenaDeleteButton", this.buildConfigSnapshotEvent(HordeConfigPage.buildArenaAction("delete", row.arenaId)));
                ++renderedRows;
            }
        }
        commandBuilder.set("#ArenaEmptyLabel.Visible", renderedRows == 0)
                .set("#ArenaEmptyLabel.Text", renderedRows == 0 ? HordeConfigPage.t(language, english, "No arenas yet. Use Add Arena to create one from your position.", "Aun no hay arenas. Usa Anadir arena para crear una desde tu posicion.") : "")
                .set("#ArenaOverflowLabel.Visible", false)
                .set("#ArenaOverflowLabel.Text", "");
    }

    private void populateArenaIconPicker(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, List<String> rewardItemCatalogOptions) {
        String selectedIconItemId = HordeConfigPage.firstNonEmpty(this.getDraftValue("arenaEditIconItemId", DEFAULT_ARENA_ITEM_ICON_ID), DEFAULT_ARENA_ITEM_ICON_ID);
        this.draftValues.put("arenaEditIconItemId", selectedIconItemId);
        commandBuilder.set("#ArenaEditIconItemId.Value", selectedIconItemId)
                .set("#ArenaEditIconPreview.ItemId", selectedIconItemId)
                .set("#ArenaEditIconCurrentLabel.Text", selectedIconItemId);

        boolean pickerVisible = this.arenaEditorModalVisible && this.arenaIconPickerModalVisible;
        commandBuilder.set("#ArenaIconPickerShade.Visible", pickerVisible)
                .set("#ArenaIconPickerFrame.Visible", pickerVisible)
                .set("#ArenaIconPickerCloseButton.Visible", pickerVisible)
                .set("#ArenaIconPickerTitleLabel.Visible", pickerVisible)
                .set("#ArenaIconPickerGrid.Visible", pickerVisible);
        commandBuilder.clear("#ArenaIconPickerGrid");
        if (!pickerVisible) {
            return;
        }

        List<String> iconOptions = HordeConfigPage.buildArenaIconPickerOptions(rewardItemCatalogOptions, selectedIconItemId);
        int rows = (iconOptions.size() + ARENA_ICON_PICKER_COLUMNS - 1) / ARENA_ICON_PICKER_COLUMNS;
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            commandBuilder.append("#ArenaIconPickerGrid", ARENA_ICON_PICKER_ROW_LAYOUT);
            String rowSelector = "#ArenaIconPickerGrid[" + rowIndex + "]";
            for (int column = 0; column < ARENA_ICON_PICKER_COLUMNS; ++column) {
                int iconIndex = rowIndex * ARENA_ICON_PICKER_COLUMNS + column;
                int slotNumber = column + 1;
                String buttonSelector = rowSelector + " #IconPickButton" + slotNumber;
                String iconSelector = rowSelector + " #IconPickIcon" + slotNumber;
                if (iconIndex < iconOptions.size()) {
                    String option = iconOptions.get(iconIndex);
                    commandBuilder.set(buttonSelector + ".Visible", true)
                            .set(iconSelector + ".Visible", true)
                            .set(iconSelector + ".ItemId", option);
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, buttonSelector, this.buildConfigSnapshotEvent(HordeConfigPage.buildArenaAction("icon_pick", option)));
                    continue;
                }
                commandBuilder.set(buttonSelector + ".Visible", false)
                        .set(iconSelector + ".Visible", false);
            }
        }
    }

    private static List<String> buildArenaIconPickerOptions(List<String> rawOptions, String selectedIconItemId) {
        ArrayList<String> options = new ArrayList<String>();
        String selected = HordeConfigPage.firstNonEmpty(selectedIconItemId, DEFAULT_ARENA_ITEM_ICON_ID);
        options.add(selected);
        if (rawOptions != null) {
            for (String rawOption : rawOptions) {
                if (rawOption == null) {
                    continue;
                }
                String cleaned = rawOption.trim();
                if (cleaned.isBlank() || HordeConfigPage.containsIgnoreCase(options, cleaned)) {
                    continue;
                }
                String lower = cleaned.toLowerCase(Locale.ROOT);
                if (lower.startsWith("random") || "none".equals(lower)) {
                    continue;
                }
                options.add(cleaned);
            }
        }
        if (options.isEmpty()) {
            options.add(DEFAULT_ARENA_ITEM_ICON_ID);
        }
        return options;
    }

    private static List<String> buildEnemyRolePickerOptions(List<String> rawOptions) {
        ArrayList<String> options = new ArrayList<String>();
        if (rawOptions != null) {
            for (String rawOption : rawOptions) {
                if (rawOption == null) {
                    continue;
                }
                String cleaned = rawOption.trim();
                if (cleaned.isBlank() || HordeConfigPage.containsIgnoreCase(options, cleaned)) {
                    continue;
                }
                options.add(cleaned);
            }
        }
        return options;
    }

    private static String resolveEnemyRoleIcon(String roleId) {
        return HordeConfigPage.resolveEnemyRoleIcon(roleId, null);
    }

    private static String resolveEnemyRoleIcon(String roleId, List<String> rewardItemCatalogOptions) {
        String cleaned = HordeConfigPage.firstNonEmpty(roleId, "").trim();
        String fallbackIcon = HordeConfigPage.firstAvailableIcon(
                rewardItemCatalogOptions,
                DEFAULT_ARENA_ITEM_ICON_ID,
                "Ingredient_Bar_Iron",
                "Ingredient_Crystal_Purple",
                "Ingredient_Crystal_Green",
                "Ingredient_Bar_Gold"
        );
        if (cleaned.isBlank()) {
            return fallbackIcon;
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.contains("dragon") || lower.contains("wyvern") || lower.contains("drake") || lower.contains("wizard") || lower.contains("mage") || lower.contains("void") || lower.contains("spectre") || lower.contains("spirit") || lower.contains("elemental")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Ingredient_Crystal_Purple", "Ingredient_Crystal_Blue", "Ingredient_Voidheart", fallbackIcon);
        }
        if (lower.contains("skeleton") || lower.contains("zombie") || lower.contains("ghoul") || lower.contains("lich") || lower.contains("wraith") || lower.contains("knight") || lower.contains("lancer") || lower.contains("praetorian")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Ingredient_Bar_Iron", "Weapon_Sword_Mithril", "Weapon_Sword_Onyxium", fallbackIcon);
        }
        if (lower.contains("goblin") || lower.contains("trork") || lower.contains("trooper") || lower.contains("orbis") || lower.contains("miner") || lower.contains("scrapper") || lower.contains("bandit")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Weapon_Sword_Mithril", "Tool_Pickaxe_Iron", "Ingredient_Bar_Copper", fallbackIcon);
        }
        if (lower.contains("spider") || lower.contains("scarak") || lower.contains("insect") || lower.contains("beetle") || lower.contains("larva") || lower.contains("roach")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Ingredient_Fabric_Scrap_Silk", "Ingredient_Bolt_Silk", "Ingredient_Crystal_Green", fallbackIcon);
        }
        if (lower.contains("wolf") || lower.contains("crocodile") || lower.contains("raptor") || lower.contains("rex") || lower.contains("saurian") || lower.contains("beast")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Ingredient_Leather_Scaled", "Ingredient_Hide_Scaled", "Ingredient_Leather_Heavy", fallbackIcon);
        }
        if (lower.contains("slime") || lower.contains("frog") || lower.contains("toad") || lower.contains("mushroom")) {
            return HordeConfigPage.firstAvailableIcon(rewardItemCatalogOptions, "Ingredient_Crystal_Green", "Potion_Signature_Lesser", fallbackIcon);
        }
        return fallbackIcon;
    }

    private static String firstAvailableIcon(List<String> rewardItemCatalogOptions, String ... candidates) {
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                if (rewardItemCatalogOptions == null || rewardItemCatalogOptions.isEmpty() || HordeConfigPage.containsIgnoreCase(rewardItemCatalogOptions, candidate)) {
                    return candidate;
                }
            }
        }
        return DEFAULT_ARENA_ITEM_ICON_ID;
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
                        || "selectedBossId".equals(field.configKey)
                        || "selectedHordeId".equals(field.configKey)
                        || "rewardCategory".equals(field.configKey)
                        || "finalBossEnabled".equals(field.configKey)
                        || "language".equals(field.configKey);
            case TAB_HORDE:
                return "hordeSelected".equals(field.configKey)
                        || "hordeEditId".equals(field.configKey)
                        || "maxRadius".equals(field.configKey)
                        || "rounds".equals(field.configKey)
                        || "baseEnemies".equals(field.configKey)
                        || "enemiesPerRound".equals(field.configKey)
                        || "waveDelay".equals(field.configKey)
                        || "enemyType".equals(field.configKey);
            case TAB_ENEMIES:
                return "enemyCategorySelected".equals(field.configKey)
                        || "enemyCategoryEditId".equals(field.configKey)
                        || "enemyCategoryEditRoles".equals(field.configKey)
                        || "enemyCategoryRolePicker".equals(field.configKey)
                        || "enemyCategoryEditIconItemId".equals(field.configKey);
            case TAB_PLAYERS:
                return "arenaJoinRadius".equals(field.configKey)
                        || "playerSelected".equals(field.configKey)
                        || "playerEditMode".equals(field.configKey);
            case TAB_SOUNDS:
                return "roundStartSoundId".equals(field.configKey)
                        || "roundStartVolume".equals(field.configKey)
                        || "roundVictorySoundId".equals(field.configKey)
                        || "roundVictoryVolume".equals(field.configKey);
            case TAB_REWARDS:
                return "rewardCatSelected".equals(field.configKey)
                        || "rewardCatEditId".equals(field.configKey)
                        || "rewardCatEditItems".equals(field.configKey)
                        || "rewardCatItemPicker".equals(field.configKey);
            case TAB_BOSSES:
                return "bossSelected".equals(field.configKey)
                        || "bossEditName".equals(field.configKey)
                        || "bossEditNpcId".equals(field.configKey)
                        || "bossEditTier".equals(field.configKey)
                        || "bossEditAmount".equals(field.configKey)
                        || "bossEditHp".equals(field.configKey)
                        || "bossEditDamage".equals(field.configKey)
                        || "bossEditSize".equals(field.configKey)
                        || "bossEditAttackRate".equals(field.configKey);
            case TAB_ARENAS:
                return "arenaSelected".equals(field.configKey)
                        || "arenaEditId".equals(field.configKey)
                        || "arenaEditIconItemId".equals(field.configKey)
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

    private static List<DropdownEntryInfo> buildPlayerModeEntries(String language, boolean english, String selectedMode) {
        ArrayList<String> values = new ArrayList<String>();
        values.add("player");
        values.add("spectator");
        values.add("exit");
        String selected = HordeConfigPage.normalizeAudienceMode(selectedMode);
        if (!HordeConfigPage.containsIgnoreCase(values, selected)) {
            values.add(selected);
        }
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>(values.size());
        for (String value : values) {
            String label;
            if ("spectator".equals(value)) {
                label = HordeConfigPage.t(language, english, "Spectator", "Espectador");
            } else if ("exit".equals(value)) {
                label = HordeConfigPage.t(language, english, "Exit area", "Salir del area");
            } else {
                label = HordeConfigPage.t(language, english, "Player", "Jugador");
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(label), value));
        }
        return entries;
    }

    private static List<String> filterRewardItemPickerOptions(List<String> options) {
        ArrayList<String> filtered = new ArrayList<String>();
        if (options != null) {
            for (String option : options) {
                if (option == null) {
                    continue;
                }
                String cleaned = option.trim();
                if (cleaned.isBlank() || HordeConfigPage.containsIgnoreCase(filtered, cleaned)) {
                    continue;
                }
                filtered.add(cleaned);
            }
        }
        if (filtered.isEmpty()) {
            filtered.add("Item_Misc_Mushroom");
        }
        return filtered;
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
        return HordeI18n.translateUi(HordeService.normalizeLanguage(language), englishText, spanishText);
    }

    private void setLocalizedTexts(UICommandBuilder commandBuilder, String language, boolean english) {
        commandBuilder.set("#TitleLabel.Text", HordeConfigPage.t(language, english, "Horde PVE Config", "Horda PVE Config"))
                .set("#SubTitleLabel.Text", "")
                .set("#TabGeneralButton.Text", HordeConfigPage.t(language, english, "General", "General"))
                .set("#TabArenasButton.Text", HordeConfigPage.t(language, english, "Arenas", "Arenas"))
                .set("#TabEnemiesButton.Text", HordeConfigPage.t(language, english, "Enemies", "Enemigos"))
                .set("#TabHordeButton.Text", HordeConfigPage.t(language, english, "Horde", "Horda"))
                .set("#TabBossesButton.Text", HordeConfigPage.t(language, english, "Bosses", "Bosses"))
                .set("#TabPlayersButton.Text", HordeConfigPage.t(language, english, "Players", "Jugadores"))
                .set("#TabRewardsButton.Text", HordeConfigPage.t(language, english, "Rewards", "Recompensas"))
                .set("#TabSoundsButton.Text", HordeConfigPage.t(language, english, "Sounds", "Sonidos"))
                .set("#TabHelpButton.Text", HordeConfigPage.t(language, english, "Help", "Ayuda"))
                .set("#TabHintLabel.Text", "")
                .set("#GeneralArenaLabel.Text", HordeConfigPage.t(language, english, "Current horde arena", "Arena actual de la horda"))
                .set("#GeneralBossLabel.Text", HordeConfigPage.t(language, english, "Current horde boss", "Boss actual de la horda"))
                .set("#GeneralHordeLabel.Text", HordeConfigPage.t(language, english, "Current horde", "Horda actual"))
                .set("#GeneralRewardLabel.Text", HordeConfigPage.t(language, english, "Current reward category", "Categoria de recompensa actual"))
                .set("#SpawnLabel.Text", HordeConfigPage.t(language, english, "Center (X Y Z)", "Centro (X Y Z)"))
                .set("#SetSpawnButton.Text", HordeConfigPage.t(language, english, "Use my current position", "Usar mi posicion actual"))
                .set("#RadiusLabel.Text", HordeConfigPage.t(language, english, "Enemy spawn radius setup", "Configuracion del radio de aparicion de enemigos"))
                .set("#RoundConfigLabel.Text", HordeConfigPage.t(language, english, "Round setup", "Configuracion de ronda"))
                .set("#MinRadiusLabel.Text", HordeConfigPage.t(language, english, "Minimum radius", "Radio minimo"))
                .set("#MaxRadiusLabel.Text", HordeConfigPage.t(language, english, "Maximum radius", "Radio maximo"))
                .set("#ArenaJoinRadiusLabel.Text", HordeConfigPage.t(language, english, "Arena players radius", "Radio de jugadores de arena"))
                .set("#PlayersTitleLabel.Text", HordeConfigPage.t(language, english, "Player audience definitions", "Definiciones de audiencia de jugadores"))
                .set("#PlayersAddButton.Text", HordeConfigPage.t(language, english, "Refresh players", "Actualizar jugadores"))
                .set("#PlayersCountLabel.Text", HordeConfigPage.t(language, english, "Detected", "Detectados"))
                .set("#PlayersHeaderName.Text", HordeConfigPage.t(language, english, "Player", "Jugador"))
                .set("#PlayersHeaderMode.Text", HordeConfigPage.t(language, english, "Mode", "Modo"))
                .set("#PlayersHeaderActions.Text", "")
                .set("#PlayersEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Player editor", "Editor de jugador"))
                .set("#PlayerEditModeLabel.Text", HordeConfigPage.t(language, english, "Audience mode", "Modo de audiencia"))
                .set("#PlayersSaveButton.Text", HordeConfigPage.t(language, english, "Save player mode", "Guardar modo jugador"))
                .set("#PlayersPagePrevButton.Text", "<")
                .set("#PlayersPageNextButton.Text", ">")
                .set("#AudienceHelpLabel.Text", "")
                .set("#EnemyCatTitleLabel.Text", HordeConfigPage.t(language, english, "Enemy category definitions", "Definiciones de categorias de enemigos"))
                .set("#EnemyCatAddButton.Text", HordeConfigPage.t(language, english, "Add category", "Anadir categoria"))
                .set("#EnemyCatHeaderName.Text", HordeConfigPage.t(language, english, "Category ID", "Categoria ID"))
                .set("#EnemyCatHeaderPreview.Text", HordeConfigPage.t(language, english, "Enemy IDs", "Enemy IDs"))
                .set("#EnemyCatHeaderActions.Text", "")
                .set("#EnemyCatEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Enemy category editor", "Editor de categoria de enemigos"))
                .set("#EnemyCatEditIdLabel.Text", HordeConfigPage.t(language, english, "Category ID", "Categoria ID"))
                .set("#EnemyCatIconSelectorLabel.Text", HordeConfigPage.t(language, english, "Category icon", "Icono de categoria"))
                .set("#EnemyCatIconPickerOpenButton.Text", HordeConfigPage.t(language, english, "Choose icon", "Elegir icono"))
                .set("#EnemyCatIconPickerTitleLabel.Text", HordeConfigPage.t(language, english, "Select an icon", "Selecciona un icono"))
                .set("#EnemyCatRolePickerLabel.Text", HordeConfigPage.t(language, english, "Enemy ID", "Enemy ID"))
                .set("#EnemyCatEnemyPickerOpenButton.Text", HordeConfigPage.t(language, english, "Add enemy", "Anadir enemigo"))
                .set("#EnemyCatRoleAddButton.Text", HordeConfigPage.t(language, english, "Add", "Anadir"))
                .set("#EnemyCatEditRolesLabel.Text", HordeConfigPage.t(language, english, "Enemy IDs in category", "Enemy IDs en categoria"))
                .set("#EnemyCatEnemyPickerTitleLabel.Text", HordeConfigPage.t(language, english, "Select enemy IDs", "Selecciona Enemy IDs"))
                .set("#EnemyCatEditRolesHelpLabel.Text", "")
                .set("#EnemyCatRolesOverflowLabel.Text", "")
                .set("#EnemyCatPagePrevButton.Text", "<")
                .set("#EnemyCatPageNextButton.Text", ">")
                .set("#EnemyCatRolesPagePrevButton.Text", "<")
                .set("#EnemyCatRolesPageNextButton.Text", ">")
                .set("#EnemyCatSaveButton.Text", HordeConfigPage.t(language, english, "Save category", "Guardar categoria"))
                .set("#RewardCatTitleLabel.Text", HordeConfigPage.t(language, english, "Reward category definitions", "Definiciones de categorias de recompensa"))
                .set("#RewardCatAddButton.Text", HordeConfigPage.t(language, english, "Add category", "Anadir categoria"))
                .set("#RewardCatHeaderName.Text", HordeConfigPage.t(language, english, "Category ID", "Categoria ID"))
                .set("#RewardCatHeaderPreview.Text", HordeConfigPage.t(language, english, "Items", "Items"))
                .set("#RewardCatHeaderActions.Text", "")
                .set("#RewardCatEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Reward category editor", "Editor de categoria de recompensa"))
                .set("#RewardCatEditIdLabel.Text", HordeConfigPage.t(language, english, "Category ID", "Categoria ID"))
                .set("#RewardCatItemPickerLabel.Text", HordeConfigPage.t(language, english, "Reward item ID", "Reward item ID"))
                .set("#RewardCatItemAddButton.Text", HordeConfigPage.t(language, english, "Add", "Anadir"))
                .set("#RewardCatEditItemsLabel.Text", HordeConfigPage.t(language, english, "Items in category", "Items en categoria"))
                .set("#RewardCatEditItemsHelpLabel.Text", "")
                .set("#RewardCatItemsOverflowLabel.Text", "")
                .set("#RewardCatPagePrevButton.Text", "<")
                .set("#RewardCatPageNextButton.Text", ">")
                .set("#RewardCatItemsPagePrevButton.Text", "<")
                .set("#RewardCatItemsPageNextButton.Text", ">")
                .set("#RewardCatSaveButton.Text", HordeConfigPage.t(language, english, "Save category", "Guardar categoria"))
                .set("#HordesTitleLabel.Text", HordeConfigPage.t(language, english, "Horde definitions", "Definiciones de hordas"))
                .set("#HordeAddButton.Text", HordeConfigPage.t(language, english, "Add horde", "Anadir horda"))
                .set("#HordeHeaderId.Text", HordeConfigPage.t(language, english, "Horde ID", "Horde ID"))
                .set("#HordeHeaderType.Text", HordeConfigPage.t(language, english, "Enemy type", "Tipo enemigo"))
                .set("#HordeHeaderRounds.Text", HordeConfigPage.t(language, english, "Rounds", "Rondas"))
                .set("#HordeHeaderActions.Text", "")
                .set("#HordeEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Horde editor", "Editor de horda"))
                .set("#HordeEditIdLabel.Text", HordeConfigPage.t(language, english, "Horde ID", "Horde ID"))
                .set("#HordePagePrevButton.Text", "<")
                .set("#HordePageNextButton.Text", ">")
                .set("#HordeSaveButton.Text", HordeConfigPage.t(language, english, "Save horde", "Guardar horda"))
                .set("#RoundLabel.Text", HordeConfigPage.t(language, english, "Number of rounds", "Cantidad de rondas"))
                .set("#BaseEnemiesLabel.Text", HordeConfigPage.t(language, english, "Base enemies per round", "Cantidad base de enemigos por ronda"))
                .set("#EnemiesPerRoundLabel.Text", HordeConfigPage.t(language, english, "Enemy increment per round", "Incremento de enemigos por ronda"))
                .set("#WaveDelayLabel.Text", HordeConfigPage.t(language, english, "Delay between rounds (s)", "Tiempo de espera entre rondas (s)"))
                .set("#RoleLabel.Text", HordeConfigPage.t(language, english, "Enemy type", "Tipo enemigo"))
                .set("#LanguageLabel.Text", HordeConfigPage.t(language, english, "Interface language", "Idioma interfaz"))
                .set("#EnemyLevelRangeLabel.Text", "")
                .set("#EnemyLevelWipLabel.Text", "")
                .set("#AutoStartEnabledLabel.Text", HordeConfigPage.t(language, english, "Automatic horde mode", "Modo horda automatica"))
                .set("#AutoStartIntervalLabel.Text", HordeConfigPage.t(language, english, "Start every (minutes)", "Iniciar cada (minutos)"))
                .set("#AutoStartApplyButton.Text", HordeConfigPage.t(language, english, "Apply auto mode", "Aplicar modo auto"))
                .set("#FinalBossLabel.Text", HordeConfigPage.t(language, english, "Final boss", "Boss final"))
                .set("#SoundsTitleLabel.Text", HordeConfigPage.t(language, english, "Sound configuration", "Configuracion de sonidos"))
                .set("#SoundsHeaderEvent.Text", HordeConfigPage.t(language, english, "Event", "Evento"))
                .set("#SoundsHeaderSound.Text", HordeConfigPage.t(language, english, "Sound ID", "Sound ID"))
                .set("#SoundsHeaderVolume.Text", HordeConfigPage.t(language, english, "Volume", "Volumen"))
                .set("#SoundRowStartEvent.Text", HordeConfigPage.t(language, english, "Round start", "Inicio ronda"))
                .set("#SoundRowVictoryEvent.Text", HordeConfigPage.t(language, english, "Round victory", "Victoria ronda"))
                .set("#SoundsEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Sound editor", "Editor de sonidos"))
                .set("#SoundsSaveButton.Text", HordeConfigPage.t(language, english, "Save sounds", "Guardar sonidos"))
                .set("#RoundStartSoundLabel.Text", HordeConfigPage.t(language, english, "Round start sound", "Sonido inicio ronda"))
                .set("#RoundVictorySoundLabel.Text", HordeConfigPage.t(language, english, "Round victory sound", "Sonido victoria ronda"))
                .set("#RoundStartVolumeLabel.Text", HordeConfigPage.t(language, english, "Start volume (%)", "Volumen inicio (%)"))
                .set("#RoundVictoryVolumeLabel.Text", HordeConfigPage.t(language, english, "Victory volume (%)", "Volumen victoria (%)"))
                .set("#RoundSoundHelpLabel.Text", "")
                .set("#BossesTitleLabel.Text", HordeConfigPage.t(language, english, "Boss definitions", "Definiciones de bosses"))
                .set("#BossAddButton.Text", HordeConfigPage.t(language, english, "Add boss", "Anadir boss"))
                .set("#BossHeaderName.Text", HordeConfigPage.t(language, english, "Boss ID", "Boss ID"))
                .set("#BossHeaderNpc.Text", HordeConfigPage.t(language, english, "Enemy ID", "Enemy ID"))
                .set("#BossHeaderTier.Text", HordeConfigPage.t(language, english, "Tier", "Tier"))
                .set("#BossHeaderAmount.Text", HordeConfigPage.t(language, english, "Amt", "Cant"))
                .set("#BossHeaderActions.Text", "")
                .set("#BossEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Boss editor", "Editor de boss"))
                .set("#BossEditNameLabel.Text", HordeConfigPage.t(language, english, "Boss ID", "Boss ID"))
                .set("#BossEditNpcIdLabel.Text", HordeConfigPage.t(language, english, "Enemy ID", "Enemy ID"))
                .set("#BossEditTierLabel.Text", HordeConfigPage.t(language, english, "Tier", "Tier"))
                .set("#BossEditAmountLabel.Text", HordeConfigPage.t(language, english, "Amount", "Cantidad"))
                .set("#BossEditHpLabel.Text", HordeConfigPage.t(language, english, "HP multiplier", "Multiplicador HP"))
                .set("#BossEditDamageLabel.Text", HordeConfigPage.t(language, english, "Damage multiplier", "Multiplicador dano"))
                .set("#BossEditSizeLabel.Text", HordeConfigPage.t(language, english, "Size multiplier", "Multiplicador tamano"))
                .set("#BossEditAttackRateLabel.Text", HordeConfigPage.t(language, english, "Attack rate x", "Velocidad ataque x"))
                .set("#BossPagePrevButton.Text", "<")
                .set("#BossPageNextButton.Text", ">")
                .set("#BossSaveButton.Text", HordeConfigPage.t(language, english, "Save boss", "Guardar boss"))
                .set("#ArenasTitleLabel.Text", HordeConfigPage.t(language, english, "Arena definitions", "Definiciones de arenas"))
                .set("#ArenaAddButton.Text", HordeConfigPage.t(language, english, "Add arena", "Anadir arena"))
                .set("#ArenaHeaderName.Text", HordeConfigPage.t(language, english, "Arena ID", "Arena ID"))
                .set("#ArenaHeaderCoords.Text", HordeConfigPage.t(language, english, "Coordinates", "Coordenadas"))
                .set("#ArenaHeaderActions.Text", "")
                .set("#ArenaEditorTitleLabel.Text", HordeConfigPage.t(language, english, "Arena editor", "Editor de arena"))
                .set("#ArenaEditIdLabel.Text", HordeConfigPage.t(language, english, "Arena ID", "Arena ID"))
                .set("#ArenaIconSelectorLabel.Text", HordeConfigPage.t(language, english, "Arena icon", "Icono de arena"))
                .set("#ArenaIconPickerOpenButton.Text", HordeConfigPage.t(language, english, "Choose icon", "Elegir icono"))
                .set("#ArenaCoordsTitleLabel.Text", HordeConfigPage.t(language, english, "Coordinates", "Coordenadas"))
                .set("#ArenaEditXLabel.Text", "X")
                .set("#ArenaEditYLabel.Text", "Y")
                .set("#ArenaEditZLabel.Text", "Z")
                .set("#ArenaUseCurrentPositionButton.Text", HordeConfigPage.t(language, english, "Use my current position", "Usar mi posicion actual"))
                .set("#ArenaSaveButton.Text", HordeConfigPage.t(language, english, "Save arena", "Guardar arena"))
                .set("#ArenaIconPickerTitleLabel.Text", HordeConfigPage.t(language, english, "Select an icon", "Selecciona un icono"))
                .set("#StatusTitleLabel.Text", "")
                .set("#ReloadModButton.Text", HordeConfigPage.t(language, english, "Reload config", "Recargar config"))
                .set("#SaveButton.Text", HordeConfigPage.t(language, english, "Save config", "Guardar config"))
                .set("#StartButton.Text", HordeConfigPage.t(language, english, "Start horde", "Iniciar horda"))
                .set("#StopButton.Text", HordeConfigPage.t(language, english, "Stop horde", "Detener horda"))
                .set("#SkipRoundButton.Text", HordeConfigPage.t(language, english, "Skip round", "Pasar ronda"))
                .set("#HelpIntroLabel.Text", HordeConfigPage.t(language, english, "Quick guide for Horde PVE Config (v1.4.0)", "Guia rapida para Horde PVE Config (v1.4.0)"))
                .set("#HelpCommandsLabel.Text", HordeConfigPage.t(language, english, "Main player commands", "Comandos principales de jugador"))
                .set("#HelpCommandsLine1.Text", HordeConfigPage.t(language, english, "/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): open config UI.", "/hordeconfig (alias: /hconfig /hordecfg /hordepve /spawnve /spawnpve): abre la UI de configuracion."))
                .set("#HelpCommandsLine2.Text", HordeConfigPage.t(language, english, "/hordeconfig start | stop | status | logs | setspawn | reload.", "/hordeconfig start | stop | status | logs | setspawn | reload."))
                .set("#HelpCommandsLine3.Text", HordeConfigPage.t(language, english, "/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.", "/hordeconfig enemy <tipo> | tipos | role <rolNpc|auto> | roles | reward <rondas> | spectator <on|off> | player | arearadius <bloques>."))
                .set("#HelpConfigLabel.Text", HordeConfigPage.t(language, english, "What Save Config stores", "Que guarda Guardar config"))
                .set("#HelpConfigLine1.Text", HordeConfigPage.t(language, english, "Center/world, min-max spawn radius, players area radius and interface language.", "Centro/mundo, radio minimo-maximo de aparicion, radio de area de jugadores e idioma interfaz."))
                .set("#HelpConfigLine2.Text", HordeConfigPage.t(language, english, "Horde definitions by ID (enemy type, radii, rounds, scaling) plus selected arena/boss/horde.", "Definiciones de horda por ID (tipo enemigo, radios, rondas, escalado) mas arena/boss/horda seleccionados."))
                .set("#HelpConfigLine3.Text", HordeConfigPage.t(language, english, "Rewards and sounds (start/victory ID and volume) are also persisted.", "Recompensas y sonidos (ID de inicio/victoria y volumen) tambien se guardan."))
                .set("#HelpExternalLabel.Text", HordeConfigPage.t(language, english, "External JSON files (plugin data folder)", "JSON externos (carpeta de datos del plugin)"))
                .set("#HelpExternalLine1.Text", HordeConfigPage.t(language, english, "horde-definitions.json: editable horde presets shown in the Horde tab.", "horde-definitions.json: presets editables de horda mostrados en la pestana Horda."))
                .set("#HelpExternalLine2.Text", HordeConfigPage.t(language, english, "enemy-categories.json and reward-items.json: enemy categories and reward catalogs.", "enemy-categories.json y reward-items.json: categorias de enemigos y catalogos de recompensa."))
                .set("#HelpExternalLine3.Text", HordeConfigPage.t(language, english, "horde-sounds.json plus arenas.json/bosses.json: sounds and arena/boss catalogs.", "horde-sounds.json mas arenas.json/bosses.json: sonidos y catalogos de arena/boss."))
                .set("#HelpReloadLabel.Text", HordeConfigPage.t(language, english, "Reload and deployment", "Recarga y despliegue"))
                .set("#HelpReloadLine1.Text", HordeConfigPage.t(language, english, "'Reload config' or /hordareload config reloads all JSON config files without restart.", "'Recargar config' o /hordareload config recarga todos los JSON de configuracion sin reiniciar."))
                .set("#HelpReloadLine2.Text", HordeConfigPage.t(language, english, "Replacing the mod .jar still requires a full server restart.", "Reemplazar el .jar del mod sigue requiriendo reinicio completo del servidor."))
                .set("#HordeCloseButton.Text", "X");
    }

    private void applyTabVisibility(UICommandBuilder commandBuilder, String tab) {
        boolean generalTab = TAB_GENERAL.equals(tab);
        boolean enemiesTab = TAB_ENEMIES.equals(tab);
        boolean hordeTab = TAB_HORDE.equals(tab);
        boolean playersTab = TAB_PLAYERS.equals(tab);
        boolean soundsTab = TAB_SOUNDS.equals(tab);
        boolean rewardsTab = TAB_REWARDS.equals(tab);
        boolean bossesTab = TAB_BOSSES.equals(tab);
        boolean arenasTab = TAB_ARENAS.equals(tab);
        boolean helpTab = TAB_HELP.equals(tab);
        boolean definitionsBackdrop = false;
        boolean editorBackdrop = false;

        commandBuilder.set("#CategoryTabs.SelectedTab", tab);
        this.setVisible(commandBuilder, definitionsBackdrop, "#DefinitionsColumnBackdrop");
        this.setVisible(commandBuilder, editorBackdrop, "#EditorColumnBackdrop");
        this.setVisible(commandBuilder, generalTab, "#GeneralArenaLabel", "#GeneralArenaId", "#GeneralBossLabel", "#GeneralBossId", "#GeneralHordeLabel", "#GeneralHordeId", "#GeneralRewardLabel", "#GeneralRewardId", "#FinalBossLabel", "#FinalBossEnabled", "#LanguageLabel", "#Language", "#AutoStartEnabledLabel", "#AutoStartEnabled", "#AutoStartIntervalLabel", "#AutoStartInterval", "#AutoStartApplyButton");
        this.setVisible(commandBuilder, enemiesTab, "#EnemyCatTitleLabel", "#EnemyCatAddButton", "#EnemyCatListInset", "#EnemyCatRowsList", "#EnemyCatEmptyLabel", "#EnemyCatOverflowLabel");
        this.setVisible(commandBuilder, hordeTab, "#HordesTitleLabel", "#HordeAddButton", "#HordeListInset", "#HordeRowsList", "#HordeEmptyLabel", "#HordeOverflowLabel");
        this.setVisible(commandBuilder, playersTab, "#PlayersTitleLabel", "#PlayersAddButton", "#PlayersListInset", "#PlayersRowsList", "#PlayersEmptyLabel", "#PlayersOverflowLabel");
        this.setVisible(commandBuilder, soundsTab, "#SoundsTitleLabel", "#SoundsListInset", "#SoundsRowsList", "#SoundsEmptyLabel", "#SoundsOverflowLabel");
        this.setVisible(commandBuilder, rewardsTab, "#RewardCatTitleLabel", "#RewardCatAddButton", "#RewardCatListInset", "#RewardCatRowsList", "#RewardCatEmptyLabel", "#RewardCatOverflowLabel");
        this.setVisible(commandBuilder, bossesTab, "#BossesTitleLabel", "#BossAddButton", "#BossListInset", "#BossRowsList", "#BossEmptyLabel", "#BossOverflowLabel");
        this.setVisible(commandBuilder, arenasTab, "#ArenasTitleLabel", "#ArenaAddButton", "#ArenaListInset", "#ArenaRowsList", "#ArenaEmptyLabel", "#ArenaOverflowLabel");
        this.setVisible(commandBuilder, helpTab, "#HelpIntroLabel", "#HelpCommandsLabel", "#HelpCommandsLine1", "#HelpCommandsLine2", "#HelpCommandsLine3", "#HelpConfigLabel", "#HelpConfigLine1", "#HelpConfigLine2", "#HelpConfigLine3", "#HelpExternalLabel", "#HelpExternalLine1", "#HelpExternalLine2", "#HelpExternalLine3", "#HelpReloadLabel", "#HelpReloadLine1", "#HelpReloadLine2");
        this.setVisible(commandBuilder, false, "#SubTitleLabel", "#TabHintLabel", "#StatusTitleLabel", "#StatusPanel", "#StatusLabel", "#SpawnStateLabel", "#SpawnLabel", "#SpawnX", "#SpawnY", "#SpawnZ", "#SetSpawnButton", "#HordeSelected", "#BossSelectedLabel", "#BossSelected", "#ArenaSelectedLabel", "#ArenaSelected", "#BossStatusLabel", "#ArenaStatusLabel", "#ArenaEditorTitleLabel", "#ArenaEditIdLabel", "#ArenaEditId", "#ArenaIconSelectorLabel", "#ArenaIconSelectorCard", "#ArenaEditIconPreview", "#ArenaEditIconCurrentLabel", "#ArenaIconPickerOpenButton", "#ArenaEditIconItemId", "#ArenaCoordsTitleLabel", "#ArenaEditXLabel", "#ArenaEditX", "#ArenaEditYLabel", "#ArenaEditY", "#ArenaEditZLabel", "#ArenaEditZ", "#ArenaUseCurrentPositionButton", "#ArenaSaveButton", "#ArenaEditorModalShade", "#ArenaEditorModalFrame", "#ArenaEditorCloseButton", "#ArenaIconPickerShade", "#ArenaIconPickerFrame", "#ArenaIconPickerCloseButton", "#ArenaIconPickerTitleLabel", "#ArenaIconPickerGrid", "#RoleHelpLabel", "#RewardCommandsHelpLabel", "#PlayerMultiplierLabel", "#PlayerMultiplier", "#RadiusLabel", "#RoundConfigLabel", "#EnemyLevelRangeLabel", "#EnemyLevelWipLabel", "#EnemyLevelMin", "#EnemyLevelRangeSeparator", "#EnemyLevelMax", "#LanguagePrevButton", "#LanguageNextButton", "#FinalBossPrevButton", "#FinalBossNextButton", "#RoundStartSoundPrevButton", "#RoundStartSoundNextButton", "#RoundVictorySoundPrevButton", "#RoundVictorySoundNextButton", "#RewardCategoryPrevButton", "#RewardCategoryNextButton", "#RewardItemPrevButton", "#RewardItemNextButton", "#RewardEveryRoundsLabel", "#RewardEveryRounds", "#RewardCategoryLabel", "#RewardCategory", "#RewardCommandsLabel", "#RewardItemId", "#RewardItemQuantityLabel", "#RewardItemQuantity", "#PlayersListTitle", "#PlayersListHint", "#PlayersRefreshButton", "#AudiencePlayersRows", "#AudiencePlayersEmptyLabel", "#HelpDiscordButton", "#HelpCurseForgeButton", "#CategoryBar", "#CategoryOuterTop", "#CategoryOuterBottom", "#CategoryOuterLeft", "#CategoryOuterRight", "#CategoryInnerTop", "#CategoryInnerBottom", "#CategoryCornerTL", "#CategoryCornerTR", "#CategoryCornerBL", "#CategoryCornerBR", "#CategoryBarInset", "#CategoryBarBottomEdge", "#CategoryLine", "#TabGeneralPlate", "#TabArenasPlate", "#TabEnemiesPlate", "#TabHordePlate", "#TabBossesPlate", "#TabPlayersPlate", "#TabSoundsPlate", "#TabRewardsPlate", "#TabHelpPlate", "#TabGeneralActiveBack", "#TabArenasActiveBack", "#TabEnemiesActiveBack", "#TabHordeActiveBack", "#TabBossesActiveBack", "#TabPlayersActiveBack", "#TabSoundsActiveBack", "#TabRewardsActiveBack", "#TabHelpActiveBack", "#TabGeneralActiveTop", "#TabArenasActiveTop", "#TabEnemiesActiveTop", "#TabHordeActiveTop", "#TabBossesActiveTop", "#TabPlayersActiveTop", "#TabSoundsActiveTop", "#TabRewardsActiveTop", "#TabHelpActiveTop", "#TabGeneralActiveNotch", "#TabArenasActiveNotch", "#TabEnemiesActiveNotch", "#TabHordeActiveNotch", "#TabBossesActiveNotch", "#TabPlayersActiveNotch", "#TabSoundsActiveNotch", "#TabRewardsActiveNotch", "#TabHelpActiveNotch");
        this.applyPlayersEditorModalVisibility(commandBuilder, playersTab);
        this.applyEnemyCategoryEditorModalVisibility(commandBuilder, enemiesTab);
        this.applyRewardCategoryEditorModalVisibility(commandBuilder, rewardsTab);
        this.applyHordeEditorModalVisibility(commandBuilder, hordeTab);
        this.applyBossEditorModalVisibility(commandBuilder, bossesTab);
        this.applySoundsEditorModalVisibility(commandBuilder, soundsTab);
        this.applyArenaEditorModalVisibility(commandBuilder, arenasTab);
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

    private void applyArenaEditorModalVisibility(UICommandBuilder commandBuilder, boolean arenasTab) {
        boolean visible = arenasTab && this.arenaEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#ArenaEditorModalShade", "#ArenaEditorModalFrame", "#ArenaEditorCloseButton", "#ArenaEditorTitleLabel", "#ArenaEditIdLabel", "#ArenaEditId", "#ArenaIconSelectorLabel", "#ArenaIconSelectorCard", "#ArenaEditIconPreview", "#ArenaEditIconCurrentLabel", "#ArenaIconPickerOpenButton", "#ArenaCoordsTitleLabel", "#ArenaEditXLabel", "#ArenaEditX", "#ArenaEditYLabel", "#ArenaEditY", "#ArenaEditZLabel", "#ArenaEditZ", "#ArenaUseCurrentPositionButton", "#ArenaSaveButton", "#ArenaStatusLabel");
    }

    private void applyPlayersEditorModalVisibility(UICommandBuilder commandBuilder, boolean playersTab) {
        boolean visible = playersTab && this.playerEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#PlayersEditorModalShade", "#PlayersEditorModalFrame", "#PlayersEditorCloseButton", "#PlayersEditorTitleLabel", "#PlayersSaveButton", "#PlayerSelected", "#PlayerEditModeLabel", "#PlayerEditMode", "#ArenaJoinRadiusLabel", "#ArenaJoinRadius", "#PlayersCountLabel", "#PlayersCountValue", "#PlayerStatusLabel");
    }

    private void applyEnemyCategoryEditorModalVisibility(UICommandBuilder commandBuilder, boolean enemiesTab) {
        boolean visible = enemiesTab && this.enemyCategoryEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#EnemyCatEditorModalShade", "#EnemyCatEditorModalFrame", "#EnemyCatEditorCloseButton", "#EnemyCatEditorTitleLabel", "#EnemyCatEditIdLabel", "#EnemyCatEditId", "#EnemyCatIconSelectorLabel", "#EnemyCatIconSelectorCard", "#EnemyCatEditIconPreview", "#EnemyCatEditIconCurrentLabel", "#EnemyCatIconPickerOpenButton", "#EnemyCatRolePickerLabel", "#EnemyCatEnemyPickerOpenButton", "#EnemyCatEditRolesLabel", "#EnemyCatRolesListInset", "#EnemyCatRolesRowsList", "#EnemyCatRolesEmptyLabel", "#EnemyCatSaveButton", "#EnemyCatStatusLabel");
        boolean pickerVisible = visible && this.enemyCategoryIconPickerModalVisible;
        this.setVisible(commandBuilder, pickerVisible, "#EnemyCatIconPickerShade", "#EnemyCatIconPickerFrame", "#EnemyCatIconPickerCloseButton", "#EnemyCatIconPickerTitleLabel", "#EnemyCatIconPickerGrid");
        boolean enemyPickerVisible = visible && this.enemyCategoryEnemyPickerModalVisible;
        this.setVisible(commandBuilder, enemyPickerVisible, "#EnemyCatEnemyPickerShade", "#EnemyCatEnemyPickerFrame", "#EnemyCatEnemyPickerCloseButton", "#EnemyCatEnemyPickerTitleLabel", "#EnemyCatEnemyPickerGrid");
    }

    private void applyRewardCategoryEditorModalVisibility(UICommandBuilder commandBuilder, boolean rewardsTab) {
        boolean visible = rewardsTab && this.rewardCategoryEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#RewardCatEditorModalShade", "#RewardCatEditorModalFrame", "#RewardCatEditorCloseButton", "#RewardCatEditorTitleLabel", "#RewardCatEditIdLabel", "#RewardCatEditId", "#RewardCatItemPickerLabel", "#RewardCatItemPicker", "#RewardCatItemAddButton", "#RewardCatEditItemsLabel", "#RewardCatItemRow1", "#RewardCatItemRow2", "#RewardCatItemRow3", "#RewardCatItemRow4", "#RewardCatItemRow5", "#RewardCatItemRow6", "#RewardCatItemRow7", "#RewardCatItemRow8", "#RewardCatItemRow9", "#RewardCatItemRow10", "#RewardCatItemsPagePrevButton", "#RewardCatItemsPageNextButton", "#RewardCatItemsPageLabel", "#RewardCatSaveButton", "#RewardCatStatusLabel");
    }

    private void applyHordeEditorModalVisibility(UICommandBuilder commandBuilder, boolean hordeTab) {
        boolean visible = hordeTab && this.hordeEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#HordeEditorModalShade", "#HordeEditorModalFrame", "#HordeEditorCloseButton", "#HordeEditorTitleLabel", "#HordeEditIdLabel", "#HordeEditId", "#RoleLabel", "#EnemyType", "#MaxRadiusLabel", "#MaxRadius", "#RoundLabel", "#Rounds", "#BaseEnemiesLabel", "#BaseEnemies", "#EnemiesPerRoundLabel", "#EnemiesPerRound", "#WaveDelayLabel", "#WaveDelay", "#HordeSaveButton", "#HordeStatusLabel");
    }

    private void applyBossEditorModalVisibility(UICommandBuilder commandBuilder, boolean bossesTab) {
        boolean visible = bossesTab && this.bossEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#BossEditorModalShade", "#BossEditorModalFrame", "#BossEditorCloseButton", "#BossEditorTitleLabel", "#BossEditNameLabel", "#BossEditName", "#BossEditAmountLabel", "#BossEditAmount", "#BossEditNpcIdLabel", "#BossEditNpcId", "#BossEditTierLabel", "#BossEditTier", "#BossEditHpLabel", "#BossEditHp", "#BossEditDamageLabel", "#BossEditDamage", "#BossEditSizeLabel", "#BossEditSize", "#BossEditAttackRateLabel", "#BossEditAttackRate", "#BossSaveButton", "#BossStatusLabel");
    }

    private void applySoundsEditorModalVisibility(UICommandBuilder commandBuilder, boolean soundsTab) {
        boolean visible = soundsTab && this.soundsEditorModalVisible;
        this.setVisible(commandBuilder, visible, "#SoundsEditorModalShade", "#SoundsEditorModalFrame", "#SoundsEditorCloseButton", "#SoundsEditorTitleLabel", "#SoundsSaveButton", "#RoundStartSoundLabel", "#RoundStartSoundId", "#RoundStartVolumeLabel", "#RoundStartVolume", "#RoundVictorySoundLabel", "#RoundVictorySoundId", "#RoundVictoryVolumeLabel", "#RoundVictoryVolume");
    }

    private static String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return TAB_GENERAL;
        }
        String normalized = tab.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case TAB_GENERAL:
            case TAB_ENEMIES:
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

    private static String resolveAudienceModeIcon(String mode) {
        String normalizedMode = HordeConfigPage.normalizeAudienceMode(mode);
        if ("spectator".equals(normalizedMode)) {
            return "Weapon_Wand_Wood";
        }
        if ("exit".equals(normalizedMode)) {
            return "Potion_Signature_Greater";
        }
        return DEFAULT_ARENA_ITEM_ICON_ID;
    }

    private static String resolveEnemyCategoryIcon(String categoryOrEnemyType) {
        String normalized = HordeConfigPage.normalizeEnemyTypeInput(categoryOrEnemyType);
        if ("random".equals(normalized) || "random-all".equals(normalized)) {
            return "Potion_Signature_Greater";
        }
        if ("void".equals(normalized) || "scarak".equals(normalized) || "elementals".equals(normalized)) {
            return "Weapon_Wand_Wood";
        }
        return DEFAULT_ARENA_ITEM_ICON_ID;
    }

    private static String resolveBossTierIcon(String tier) {
        String normalizedTier = HordeConfigPage.firstNonEmpty(tier, "common").trim().toLowerCase(Locale.ROOT);
        if ("legendary".equals(normalizedTier) || "mythic".equals(normalizedTier)) {
            return "Ingredient_Bar_Gold";
        }
        if ("rare".equals(normalizedTier) || "epic".equals(normalizedTier)) {
            return "Potion_Signature_Greater";
        }
        return "Weapon_Wand_Wood";
    }

    private static String resolveListIconCandidate(String itemId) {
        String cleaned = HordeConfigPage.firstNonEmpty(itemId, "").trim();
        if (cleaned.isBlank()) {
            return DEFAULT_ARENA_ITEM_ICON_ID;
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if ("auto".equals(lower) || "none".equals(lower) || lower.startsWith("random")) {
            return DEFAULT_ARENA_ITEM_ICON_ID;
        }
        return cleaned;
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

    private static UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue.trim());
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static String buildBossAction(String action, String bossId) {
        return "boss_" + action + ":" + HordeConfigPage.firstNonEmpty(bossId, "");
    }

    private static String buildPlayerDefinitionAction(String action, String playerId) {
        return "playerdef_" + action + ":" + HordeConfigPage.firstNonEmpty(playerId, "");
    }

    private static String buildHordeDefinitionAction(String action, String hordeId) {
        return "hordedef_" + action + ":" + HordeConfigPage.firstNonEmpty(hordeId, "");
    }

    private static String buildEnemyCategoryAction(String action, String categoryId) {
        return "enemycat_" + action + ":" + HordeConfigPage.firstNonEmpty(categoryId, "");
    }

    private static String buildRewardCategoryAction(String action, String categoryId) {
        return "rewardcat_" + action + ":" + HordeConfigPage.firstNonEmpty(categoryId, "");
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

    private static List<String> collectBossIds(List<BossArenaCatalogService.BossDefinitionSnapshot> rows) {
        ArrayList<String> ids = new ArrayList<String>();
        if (rows == null) {
            return ids;
        }
        for (BossArenaCatalogService.BossDefinitionSnapshot row : rows) {
            if (row == null || row.bossId == null || row.bossId.isBlank() || HordeConfigPage.containsIgnoreCase(ids, row.bossId)) {
                continue;
            }
            ids.add(row.bossId);
        }
        return ids;
    }

    private static String firstBossId(List<BossArenaCatalogService.BossDefinitionSnapshot> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        for (BossArenaCatalogService.BossDefinitionSnapshot row : rows) {
            if (row != null && row.bossId != null && !row.bossId.isBlank()) {
                return row.bossId;
            }
        }
        return "";
    }

    private static List<String> collectHordeIds(List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> rows) {
        ArrayList<String> ids = new ArrayList<String>();
        if (rows == null) {
            return ids;
        }
        for (HordeDefinitionCatalogService.HordeDefinitionSnapshot row : rows) {
            if (row == null || row.hordeId == null || row.hordeId.isBlank() || HordeConfigPage.containsIgnoreCase(ids, row.hordeId)) {
                continue;
            }
            ids.add(row.hordeId);
        }
        return ids;
    }

    private static String firstHordeId(List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        for (HordeDefinitionCatalogService.HordeDefinitionSnapshot row : rows) {
            if (row != null && row.hordeId != null && !row.hordeId.isBlank()) {
                return row.hordeId;
            }
        }
        return "";
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

    private static HordeService.EnemyCategorySnapshot findEnemyCategorySnapshot(List<HordeService.EnemyCategorySnapshot> rows, String categoryId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String requested = categoryId == null ? "" : categoryId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (HordeService.EnemyCategorySnapshot row : rows) {
            if (row == null || row.categoryId == null) {
                continue;
            }
            if (row.categoryId.equalsIgnoreCase(requested)) {
                return row;
            }
        }
        return null;
    }

    private static HordeService.RewardCategorySnapshot findRewardCategorySnapshot(List<HordeService.RewardCategorySnapshot> rows, String categoryId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String requested = categoryId == null ? "" : categoryId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (HordeService.RewardCategorySnapshot row : rows) {
            if (row == null || row.categoryId == null) {
                continue;
            }
            if (row.categoryId.equalsIgnoreCase(requested)) {
                return row;
            }
        }
        return null;
    }

    private static HordeService.AudiencePlayerSnapshot findAudienceSnapshot(List<HordeService.AudiencePlayerSnapshot> rows, String playerId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        UUID requestedId = HordeConfigPage.parseUuid(playerId);
        if (requestedId != null) {
            for (HordeService.AudiencePlayerSnapshot row : rows) {
                if (row == null || row.playerId == null) {
                    continue;
                }
                if (requestedId.equals((Object)row.playerId)) {
                    return row;
                }
            }
        }
        String requested = playerId == null ? "" : playerId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (HordeService.AudiencePlayerSnapshot row : rows) {
            if (row == null || row.playerId == null) {
                continue;
            }
            if (row.playerId.toString().equalsIgnoreCase(requested)) {
                return row;
            }
        }
        return null;
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

    private static HordeDefinitionCatalogService.HordeDefinitionSnapshot findHordeSnapshot(List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> rows, String hordeId) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String requested = hordeId == null ? "" : hordeId.trim();
        if (requested.isBlank()) {
            return null;
        }
        for (HordeDefinitionCatalogService.HordeDefinitionSnapshot row : rows) {
            if (row == null || row.hordeId == null) {
                continue;
            }
            if (row.hordeId.equalsIgnoreCase(requested)) {
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

    private static int findHordeIndex(List<HordeDefinitionCatalogService.HordeDefinitionSnapshot> rows, String hordeId) {
        if (rows == null || rows.isEmpty() || hordeId == null || hordeId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); ++i) {
            HordeDefinitionCatalogService.HordeDefinitionSnapshot row = rows.get(i);
            if (row == null || row.hordeId == null) {
                continue;
            }
            if (row.hordeId.equalsIgnoreCase(hordeId)) {
                return i;
            }
        }
        return -1;
    }

    private static int findEnemyCategoryIndex(List<HordeService.EnemyCategorySnapshot> rows, String categoryId) {
        if (rows == null || rows.isEmpty() || categoryId == null || categoryId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); ++i) {
            HordeService.EnemyCategorySnapshot row = rows.get(i);
            if (row == null || row.categoryId == null) {
                continue;
            }
            if (row.categoryId.equalsIgnoreCase(categoryId)) {
                return i;
            }
        }
        return -1;
    }

    private static int findRewardCategoryIndex(List<HordeService.RewardCategorySnapshot> rows, String categoryId) {
        if (rows == null || rows.isEmpty() || categoryId == null || categoryId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); ++i) {
            HordeService.RewardCategorySnapshot row = rows.get(i);
            if (row == null || row.categoryId == null) {
                continue;
            }
            if (row.categoryId.equalsIgnoreCase(categoryId)) {
                return i;
            }
        }
        return -1;
    }

    private static int findAudienceIndex(List<HordeService.AudiencePlayerSnapshot> rows, String playerId) {
        if (rows == null || rows.isEmpty() || playerId == null || playerId.isBlank()) {
            return -1;
        }
        UUID requestedId = HordeConfigPage.parseUuid(playerId);
        for (int i = 0; i < rows.size(); ++i) {
            HordeService.AudiencePlayerSnapshot row = rows.get(i);
            if (row == null || row.playerId == null) {
                continue;
            }
            if (requestedId != null && requestedId.equals((Object)row.playerId)) {
                return i;
            }
            if (row.playerId.toString().equalsIgnoreCase(playerId.trim())) {
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

    private static String firstEnemyRoleOption(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        for (String option : options) {
            if (option == null || option.isBlank()) {
                continue;
            }
            return option.trim();
        }
        return "";
    }

    private static List<String> parseEnemyCategoryRolesCsv(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new ArrayList<String>();
        }
        String normalized = rawValue.replace('\r', '\n').replace(',', '\n').replace(';', '\n');
        String[] parts = normalized.split("\\n");
        ArrayList<String> roles = new ArrayList<String>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String role = part.trim();
            if (role.isBlank() || HordeConfigPage.containsIgnoreCase(roles, role)) {
                continue;
            }
            roles.add(role);
        }
        return roles;
    }

    private static String buildRolesCsv(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        ArrayList<String> clean = new ArrayList<String>();
        for (String role : roles) {
            if (role == null || role.isBlank() || HordeConfigPage.containsIgnoreCase(clean, role)) {
                continue;
            }
            clean.add(role.trim());
        }
        return String.join(", ", clean);
    }

    private static boolean removeIgnoreCase(List<String> values, String targetValue) {
        if (values == null || values.isEmpty() || targetValue == null || targetValue.isBlank()) {
            return false;
        }
        for (int i = 0; i < values.size(); ++i) {
            String value = values.get(i);
            if (value == null) {
                continue;
            }
            if (value.equalsIgnoreCase(targetValue.trim())) {
                values.remove(i);
                return true;
            }
        }
        return false;
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



