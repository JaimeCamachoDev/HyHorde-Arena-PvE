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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public final class HordeStatusPage
extends CustomUIPage {
    private static final String LAYOUT = "Pages/HordeStatusPage.ui";
    private static final int MAX_LEADERBOARD_ROWS = 14;
    private final Consumer<UUID> onDismissCallback;
    private HordeService.StatusSnapshot snapshot;

    private HordeStatusPage(PlayerRef playerRef, HordeService.StatusSnapshot initialSnapshot, Consumer<UUID> onDismissCallback) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
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
        String language = HordeService.normalizeLanguage(status.language);
        boolean english = HordeService.isEnglishLanguage(language);
        String stateText = status.active ? HordeStatusPage.t(language, english, "Active", "Activa") : HordeStatusPage.t(language, english, "Inactive", "Inactiva");
        String nextRoundText = status.active ? status.nextRoundInSeconds + "s" : "-";
        String remainingText = HordeStatusPage.t(language, english, status.aliveEnemies + " enemies remaining", "Quedan " + status.aliveEnemies + " enemigos");
        String personalLine = this.buildPersonalLine(status, language, english);
        String killsWord = HordeStatusPage.t(language, english, " kills", " bajas");
        String deathsWord = HordeStatusPage.t(language, english, " deaths", " muertes");
        commandBuilder.append(LAYOUT)
                .set("#TitleLabel.Text", HordeStatusPage.t(language, english, "Horde PVE - Status", "Horda PVE - Estado"))
                .set("#StateLabel.Text", HordeStatusPage.t(language, english, "State", "Estado"))
                .set("#StateValue.Text", stateText)
                .set("#WorldLabel.Text", HordeStatusPage.t(language, english, "World", "Mundo"))
                .set("#WorldValue.Text", status.worldName)
                .set("#RoundLabel.Text", HordeStatusPage.t(language, english, "Round", "Ronda"))
                .set("#RoundValue.Text", status.currentRound + "/" + status.totalRounds)
                .set("#AliveLabel.Text", HordeStatusPage.t(language, english, "Alive enemies", "Enemigos vivos"))
                .set("#AliveValue.Text", Integer.toString(status.aliveEnemies))
                .set("#RemainingLabel.Text", HordeStatusPage.t(language, english, "Counter", "Contador"))
                .set("#RemainingValue.Text", remainingText)
                .set("#SpawnedLabel.Text", HordeStatusPage.t(language, english, "Total spawned", "Spawn total"))
                .set("#SpawnedValue.Text", Integer.toString(status.totalSpawned))
                .set("#KillsLabel.Text", HordeStatusPage.t(language, english, "Kills detected", "Kills detectadas"))
                .set("#KillsValue.Text", Integer.toString(status.totalKilled))
                .set("#DeathsLabel.Text", HordeStatusPage.t(language, english, "Player deaths", "Muertes jugadores"))
                .set("#DeathsValue.Text", Integer.toString(status.totalDeaths))
                .set("#ElapsedLabel.Text", HordeStatusPage.t(language, english, "Elapsed", "Tiempo total"))
                .set("#ElapsedValue.Text", HordeStatusPage.formatDuration(status.elapsedSeconds))
                .set("#NextRoundLabel.Text", HordeStatusPage.t(language, english, "Next round", "Siguiente ronda"))
                .set("#NextRoundValue.Text", nextRoundText)
                .set("#RoleLabel.Text", HordeStatusPage.t(language, english, "Type / role", "Tipo / rol"))
                .set("#RoleValue.Text", status.role.isBlank() ? "auto" : status.role)
                .set("#PlayersCountLabel.Text", HordeStatusPage.t(language, english, "Tracked players", "Jugadores rastreados"))
                .set("#PlayersCountValue.Text", Integer.toString(status.players.size()))
                .set("#PlayerInfoLabel.Text", HordeStatusPage.t(language, english, "Your stats", "Tus estadisticas"))
                .set("#PlayerInfo.Text", personalLine)
                .set("#LeaderboardTitle.Text", HordeStatusPage.t(language, english, "LEADERBOARD", "CLASIFICACION"))
                .set("#EmptyLeaderboardLabel.Text", status.players.isEmpty() ? HordeStatusPage.t(language, english, "No player stats yet.", "Aun no hay estadisticas.") : "")
                .set("#CloseButton.Text", HordeStatusPage.t(language, english, "Close", "Cerrar"));
        int rowIndex = 0;
        for (HordeService.PlayerSnapshot row : status.players) {
            if (row == null || rowIndex >= MAX_LEADERBOARD_ROWS) continue;
            commandBuilder.append("#LeaderboardRows", "Pages/HordeLeaderboardRow.ui");
            Message killsMessage = Message.raw((String)Integer.toString(row.kills)).color(Color.GREEN);
            Message deathsMessage = Message.raw((String)Integer.toString(row.deaths)).color(Color.RED);
            String nameText = this.playerRef.getUuid().equals((Object)row.playerId) ? "* " + HordeStatusPage.compactName(row.username, 14) : HordeStatusPage.compactName(row.username, 16);
            commandBuilder.set("#LeaderboardRows[" + rowIndex + "] #RowName.Text", nameText).set("#LeaderboardRows[" + rowIndex + "] #RowKills.TextSpans", killsMessage).set("#LeaderboardRows[" + rowIndex + "] #RowKillsMessage.Text", killsWord).set("#LeaderboardRows[" + rowIndex + "] #RowDeaths.TextSpans", deathsMessage).set("#LeaderboardRows[" + rowIndex + "] #RowDeathsMessage.Text", deathsWord);
            ++rowIndex;
        }
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

    private String buildPersonalLine(HordeService.StatusSnapshot status, String language, boolean english) {
        HordeService.PlayerSnapshot self = status.findPlayer(this.playerRef.getUuid());
        if (self == null) {
            return HordeStatusPage.t(language, english, "No personal stats yet.", "Sin estadisticas personales por ahora.");
        }
        String englishText = String.format(Locale.ROOT, "%s - %d kills / %d deaths", HordeStatusPage.compactName(self.username, 18), self.kills, self.deaths);
        String spanishText = String.format(Locale.ROOT, "%s - %d bajas / %d muertes", HordeStatusPage.compactName(self.username, 18), self.kills, self.deaths);
        return HordeStatusPage.t(language, english, englishText, spanishText);
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

    private static String compactName(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "Player";
        }
        String value = input.trim();
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + ".";
    }
}
