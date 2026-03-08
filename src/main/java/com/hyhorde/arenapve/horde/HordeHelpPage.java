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
import com.hyhorde.arenapve.commands.HordeHelpCommand;
import java.util.Locale;

public final class HordeHelpPage
extends CustomUIPage {
    private static final String LAYOUT = "Pages/HordeHelpPage.ui";
    private final HordeService hordeService;

    private HordeHelpPage(PlayerRef playerRef, HordeService hordeService) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.hordeService = hordeService;
    }

    public static void open(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, HordeService hordeService) {
        HordeHelpPage page = new HordeHelpPage(playerRef, hordeService);
        player.getPageManager().openCustomPage(playerEntityRef, store, (CustomUIPage)page);
    }

    public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
        HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
        String rewardSummary = config.rewardItemId == null || config.rewardItemId.isBlank() ? "Sin item configurado" : config.rewardItemId + " x" + config.rewardItemQuantity;
        commandBuilder.append(LAYOUT).set("#StatusValue.Text", this.hordeService.getStatusLine()).set("#QuickSummary.Text", String.format(Locale.ROOT, "Rondas: %d | Tipo: %s | Jugadores x%d | Recompensa cada: %d | Item: %s", config.rounds, config.enemyType, config.playerMultiplier, config.rewardEveryRounds, rewardSummary));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OpenConfigButton", EventData.of((String)"action", (String)"open_config")).addEventBinding(CustomUIEventBindingType.Activating, "#ChatHelpButton", EventData.of((String)"action", (String)"chat_help")).addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"action", (String)"close"));
    }

    public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, String payloadText) {
        JsonObject payload;
        try {
            payload = JsonParser.parseString((String)payloadText).getAsJsonObject();
        }
        catch (Exception ex) {
            this.playerRef.sendMessage(Message.raw((String)"No se pudo interpretar el evento de la ayuda."));
            return;
        }
        String action = HordeHelpPage.read(payload, "action");
        switch (action) {
            case "close": {
                this.close();
                return;
            }
            case "chat_help": {
                HordeHelpCommand.sendChatHelp(this.playerRef);
                return;
            }
            case "open_config": {
                Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
                if (player == null) {
                    this.playerRef.sendMessage(Message.raw((String)"No se pudo abrir el menu de configuracion."));
                    return;
                }
                HordeConfigPage.open(playerEntityRef, store, player, this.playerRef, this.hordeService);
                return;
            }
        }
    }

    private static String read(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }
}
