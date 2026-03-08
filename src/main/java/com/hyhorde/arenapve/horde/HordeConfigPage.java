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
        boolean active = this.hordeService.isActive();
        commandBuilder.append(LAYOUT).set("#SpawnX.Value", HordeConfigPage.formatDouble(config.spawnX)).set("#SpawnY.Value", HordeConfigPage.formatDouble(config.spawnY)).set("#SpawnZ.Value", HordeConfigPage.formatDouble(config.spawnZ)).set("#MinRadius.Value", HordeConfigPage.formatDouble(config.minSpawnRadius)).set("#MaxRadius.Value", HordeConfigPage.formatDouble(config.maxSpawnRadius)).set("#Rounds.Value", Integer.toString(config.rounds)).set("#BaseEnemies.Value", Integer.toString(config.baseEnemiesPerRound)).set("#EnemiesPerRound.Value", Integer.toString(config.enemiesPerRoundIncrement)).set("#WaveDelay.Value", Integer.toString(config.waveDelaySeconds)).set("#EnemyType.Value", config.enemyType == null ? "auto" : config.enemyType).set("#RewardEveryRounds.Value", Integer.toString(config.rewardEveryRounds)).set("#RewardCommands.Value", HordeConfigPage.formatRewardCommands(config.rewardCommands)).set("#SpawnStateLabel.Text", HordeConfigPage.buildSpawnLabel(config)).set("#StatusLabel.Text", this.hordeService.getStatusLine()).set("#StartButton.Visible", !active).set("#StopButton.Visible", active);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"action", (String)"close")).addEventBinding(CustomUIEventBindingType.Activating, "#SetSpawnButton", EventData.of((String)"action", (String)"set_spawn_here")).addEventBinding(CustomUIEventBindingType.Activating, "#RolesButton", EventData.of((String)"action", (String)"enemy_types")).addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", this.buildConfigSnapshotEvent("save")).addEventBinding(CustomUIEventBindingType.Activating, "#StartButton", this.buildConfigSnapshotEvent("start")).addEventBinding(CustomUIEventBindingType.Activating, "#StopButton", EventData.of((String)"action", (String)"stop"));
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        JsonObject payload;
        try {
            payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)"No se pudo interpretar el evento de la UI."));
            return;
        }
        String action = HordeConfigPage.read(payload, "action");
        World world = ((EntityStore)store.getExternalData()).getWorld();
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
            default: {
                result = HordeService.OperationResult.fail("Accion de UI desconocida: " + action);
            }
        }
        if (result != null) {
            this.playerRef.sendMessage(Message.raw((String)result.getMessage()));
        }
        this.rebuild();
    }

    private EventData buildConfigSnapshotEvent(String action) {
        return EventData.of((String)"action", (String)action).append("@SpawnX", "#SpawnX.Value").append("@SpawnY", "#SpawnY.Value").append("@SpawnZ", "#SpawnZ.Value").append("@MinRadius", "#MinRadius.Value").append("@MaxRadius", "#MaxRadius.Value").append("@Rounds", "#Rounds.Value").append("@BaseEnemies", "#BaseEnemies.Value").append("@EnemiesPerRound", "#EnemiesPerRound.Value").append("@WaveDelay", "#WaveDelay.Value").append("@EnemyType", "#EnemyType.Value").append("@RewardEveryRounds", "#RewardEveryRounds.Value").append("@RewardCommands", "#RewardCommands.Value");
    }

    private void sendEnemyTypesPreview() {
        List<String> enemyTypes = this.hordeService.getEnemyTypeOptions();
        this.playerRef.sendMessage(Message.raw((String)("Tipos de enemigo: " + String.join(", ", enemyTypes))));
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
        values.put("enemyType", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "enemyType"), HordeConfigPage.read(payload, "@EnemyType"), HordeConfigPage.read(payload, "EnemyType"), HordeConfigPage.read(payload, "role"), HordeConfigPage.read(payload, "@Role"), HordeConfigPage.read(payload, "Role")));
        values.put("rewardEveryRounds", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardEveryRounds"), HordeConfigPage.read(payload, "@RewardEveryRounds"), HordeConfigPage.read(payload, "RewardEveryRounds")));
        values.put("rewardCommands", HordeConfigPage.firstNonEmpty(HordeConfigPage.read(payload, "rewardCommands"), HordeConfigPage.read(payload, "@RewardCommands"), HordeConfigPage.read(payload, "RewardCommands")));
        return values;
    }

    private static String buildSpawnLabel(HordeService.HordeConfig config) {
        if (!config.spawnConfigured) {
            return "Centro de horda no configurado. Puedes usar tu posicion actual.";
        }
        return String.format(Locale.ROOT, "Centro actual: %.2f %.2f %.2f | Mundo: %s", config.spawnX, config.spawnY, config.spawnZ, config.worldName);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatRewardCommands(List<String> rewardCommands) {
        if (rewardCommands == null || rewardCommands.isEmpty()) {
            return "";
        }
        return String.join((CharSequence)"; ", rewardCommands);
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
}


