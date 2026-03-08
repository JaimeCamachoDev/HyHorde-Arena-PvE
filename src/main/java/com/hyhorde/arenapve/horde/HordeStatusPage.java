package com.hyhorde.arenapve.horde;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyhorde.arenapve.horde.HordeService;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public final class HordeStatusPage
extends CustomUIPage {
    private static final String LAYOUT = "Pages/HordeStatusPage.ui";
    private final Consumer<UUID> onDismissCallback;
    private HordeService.StatusSnapshot snapshot;

    private HordeStatusPage(PlayerRef playerRef, HordeService.StatusSnapshot initialSnapshot, Consumer<UUID> onDismissCallback) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.snapshot = initialSnapshot;
        this.onDismissCallback = onDismissCallback;
    }

    public static HordeStatusPage open(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, HordeService.StatusSnapshot initialSnapshot, Consumer<UUID> onDismissCallback) {
        HordeStatusPage page = new HordeStatusPage(playerRef, initialSnapshot, onDismissCallback);
        player.getPageManager().openCustomPage(playerEntityRef, store, (CustomUIPage)page);
        return page;
    }

    public void updateSnapshot(HordeService.StatusSnapshot snapshot) {
        this.snapshot = snapshot;
        this.rebuild();
    }

    public void closeFromService() {
        this.close();
    }

    public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
        HordeService.StatusSnapshot status = this.snapshot;
        String stateText = status.active ? "Activa" : "Inactiva";
        String nextRoundText = status.active ? Long.toString(status.nextRoundInSeconds) + "s" : "-";
        commandBuilder.append(LAYOUT).set("#StateValue.Text", stateText).set("#WorldValue.Text", status.worldName).set("#RoundValue.Text", status.currentRound + "/" + status.totalRounds).set("#AliveValue.Text", Integer.toString(status.aliveEnemies)).set("#SpawnedValue.Text", Integer.toString(status.totalSpawned)).set("#KillsValue.Text", Integer.toString(status.totalKilled)).set("#ElapsedValue.Text", HordeStatusPage.formatDuration(status.elapsedSeconds)).set("#NextRoundValue.Text", nextRoundText).set("#RoleValue.Text", status.role.isBlank() ? "auto" : status.role);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"action", (String)"close"));
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        try {
            JsonObject payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
            if ("close".equals(HordeStatusPage.read(payload, "action"))) {
                this.close();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void onDismiss(Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
        super.onDismiss(playerEntityRef, store);
        if (this.onDismissCallback != null) {
            this.onDismissCallback.accept(this.playerRef.getUuid());
        }
    }

    private static String read(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String formatDuration(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%02dh %02dm %02ds", hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%02dm %02ds", minutes, secs);
    }
}


