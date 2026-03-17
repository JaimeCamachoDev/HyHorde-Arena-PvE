package com.hyhorde.arenapve.horde;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HordeHudSystem
extends EntityTickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger(HordeHudSystem.class.getName());
    private static final Query<EntityStore> QUERY = Archetype.of(Player.getComponentType(), PlayerRef.getComponentType());
    private static final int UPDATE_INTERVAL = 20;
    private static final String MHUD_CLASS = "com.buuz135.mhud.MultipleHUD";
    private static final String MHUD_CLASS_LEGACY = "com.buuz135.multiplehud.MultipleHUD";
    private static final PluginIdentifier MHUD_PLUGIN_ID = new PluginIdentifier("Buuz135", "MultipleHUD");
    private static final long MHUD_RESOLVE_RETRY_MILLIS = 3000L;
    private static final String MHUD_IDENTIFIER = "HyHordeArenaPVE_HordeHUD";
    private final HordeService hordeService;
    private final Map<UUID, HordeRuntimeHud> huds;
    private final Map<UUID, HudAttachMode> attachModes;
    private final Map<UUID, Integer> tickCounters;
    private boolean mhudAvailable;
    private long nextMhudResolveAttemptAtMs;
    private Object mhudInstance;
    private Method mhudGetInstanceMethod;
    private Method mhudSetCustomHudMethod;
    private Method mhudHideCustomHudMethod;
    private String mhudResolvedClassName;

    public HordeHudSystem(HordeService hordeService) {
        this.hordeService = hordeService;
        this.huds = new ConcurrentHashMap<UUID, HordeRuntimeHud>();
        this.attachModes = new ConcurrentHashMap<UUID, HudAttachMode>();
        this.tickCounters = new ConcurrentHashMap<UUID, Integer>();
        this.mhudAvailable = false;
        this.nextMhudResolveAttemptAtMs = 0L;
        this.mhudInstance = null;
        this.mhudGetInstanceMethod = null;
        this.mhudSetCustomHudMethod = null;
        this.mhudHideCustomHudMethod = null;
        this.mhudResolvedClassName = null;
    }

    public void tick(float deltaTime, int tickCounter, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        EntityStore entityStore = (EntityStore)store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        this.hordeService.tickAutoStart(store, world);
        // Known issue history (important for future maintenance):
        // - Client disconnect: "Failed to apply CustomUI HUD commands".
        // - Root cause: two mods writing CustomUIHud packets for the same player.
        // Mitigation in this system:
        // 1) HUD updates only from world tick thread.
        // 2) Direct mode never steals ownership from foreign CustomUIHud.
        // 3) If MHUD is installed, use it as multiplexer (Economy + Horde at once).
        // 4) Never clear with setCustomHud(null) directly (use an empty HUD payload).
        boolean hordeActiveInWorld = world != null && this.hordeService.isTrackingWorld(world);
        for (int entityIndex = 0; entityIndex < chunk.size(); ++entityIndex) {
            Player player = (Player)chunk.getComponent(entityIndex, Player.getComponentType());
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null || playerRef == null || playerRef.getUuid() == null) {
                continue;
            }
            UUID playerId = playerRef.getUuid();
            boolean shouldShowHud = hordeActiveInWorld && this.hordeService.isArenaAudience(playerRef);
            HordeRuntimeHud trackedHud = this.huds.get(playerId);
            HudAttachMode attachMode = this.attachModes.getOrDefault(playerId, HudAttachMode.DIRECT);
            if (!shouldShowHud) {
                if (trackedHud != null || this.tickCounters.containsKey(playerId)) {
                    this.removeHudForPlayer(player, playerRef, playerId, trackedHud, attachMode);
                } else {
                    this.tickCounters.remove(playerId);
                }
                continue;
            }
            if (trackedHud == null) {
                trackedHud = this.createHudForPlayer(player, playerRef, playerId);
                if (trackedHud == null) {
                    continue;
                }
                attachMode = this.attachModes.getOrDefault(playerId, HudAttachMode.DIRECT);
            }
            if (attachMode == HudAttachMode.DIRECT && player.getHudManager().getCustomHud() != trackedHud) {
                // Another plugin took direct ownership while horde was active.
                this.removeTrackingOnly(playerId);
                continue;
            }
            if (attachMode == HudAttachMode.MHUD && !this.isMhudAvailable()) {
                // MHUD disappeared or became unavailable; avoid sending stale updates.
                this.removeTrackingOnly(playerId);
                continue;
            }
            int localCounter = this.tickCounters.getOrDefault(playerId, 0) + 1;
            if (localCounter < UPDATE_INTERVAL) {
                this.tickCounters.put(playerId, localCounter);
                continue;
            }
            this.tickCounters.put(playerId, 0);
            try {
                trackedHud.setSnapshot(this.hordeService.getStatusSnapshot());
                if (attachMode == HudAttachMode.MHUD) {
                    if (!this.pushHudUpdateThroughMhud(player, playerRef, trackedHud)) {
                        this.removeTrackingOnly(playerId);
                    }
                } else {
                    trackedHud.updateHud();
                }
            }
            catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to update Horde HUD for player: " + playerRef.getUsername(), ex);
                this.removeTrackingOnly(playerId);
            }
        }
    }

    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    private HordeRuntimeHud createHudForPlayer(Player player, PlayerRef playerRef, UUID playerId) {
        try {
            CustomUIHud currentHud = player.getHudManager().getCustomHud();
            if (currentHud instanceof HordeRuntimeHud) {
                HordeRuntimeHud existing = (HordeRuntimeHud)currentHud;
                this.huds.put(playerId, existing);
                this.attachModes.put(playerId, HudAttachMode.DIRECT);
                this.tickCounters.put(playerId, 0);
                return existing;
            }
            if (currentHud instanceof EmptyRuntimeHud) {
                // Internal clear marker from previous horde end. Safe to replace.
                currentHud = null;
            }
            HordeRuntimeHud hud = new HordeRuntimeHud(playerRef, this.hordeService);
            if (this.isMhudAvailable()) {
                // Preferred coexistence path:
                // MHUD can multiplex Economy + Horde without conflicting packet streams.
                if (this.pushHudUpdateThroughMhud(player, playerRef, hud)) {
                    hud.setSnapshot(this.hordeService.getStatusSnapshot());
                    this.pushHudUpdateThroughMhud(player, playerRef, hud);
                    this.huds.put(playerId, hud);
                    this.attachModes.put(playerId, HudAttachMode.MHUD);
                    this.tickCounters.put(playerId, 0);
                    return hud;
                }
            }
            if (currentHud != null) {
                // No MHUD available and another mod already owns CustomUIHud.
                // Do not force takeover: this used to cause client disconnects.
                return null;
            }
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)hud);
            hud.setSnapshot(this.hordeService.getStatusSnapshot());
            hud.updateHud();
            this.huds.put(playerId, hud);
            this.attachModes.put(playerId, HudAttachMode.DIRECT);
            this.tickCounters.put(playerId, 0);
            return hud;
        }
        catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to create Horde HUD for player: " + playerRef.getUsername(), ex);
            return null;
        }
    }

    private void removeHudForPlayer(Player player, PlayerRef playerRef, UUID playerId, HordeRuntimeHud trackedHud, HudAttachMode attachMode) {
        this.huds.remove(playerId);
        this.attachModes.remove(playerId);
        this.tickCounters.remove(playerId);
        if (trackedHud == null) {
            return;
        }
        try {
            if (attachMode == HudAttachMode.MHUD && this.isMhudAvailable()) {
                this.hideHudThroughMhud(player, playerRef);
                return;
            }
            CustomUIHud currentHud = player.getHudManager().getCustomHud();
            if (currentHud != trackedHud) {
                return;
            }
            // Avoid setCustomHud(null): some client builds can crash applying null payloads.
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)new EmptyRuntimeHud(playerRef));
        }
        catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to clear Horde HUD binding for player: " + playerRef.getUsername(), ex);
        }
    }

    private void removeTrackingOnly(UUID playerId) {
        this.huds.remove(playerId);
        this.attachModes.remove(playerId);
        this.tickCounters.remove(playerId);
    }

    private synchronized boolean isMhudAvailable() {
        // Compatibility note:
        // ArenaPVE can be enabled before MultipleHUD/Economy in plugin load order.
        // If we cache "MHUD unavailable" forever, Horde HUD never appears with Economy.
        // Retry resolution with backoff so late plugin initialization is handled safely.
        if (this.mhudAvailable && this.resolveMhudInstance() != null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextMhudResolveAttemptAtMs) {
            return false;
        }
        if (this.resolveMhudSupport()) {
            this.nextMhudResolveAttemptAtMs = 0L;
            return true;
        }
        this.nextMhudResolveAttemptAtMs = now + MHUD_RESOLVE_RETRY_MILLIS;
        return false;
    }

    private synchronized boolean resolveMhudSupport() {
        if (this.resolveMhudInstance() != null && this.mhudSetCustomHudMethod != null && this.mhudHideCustomHudMethod != null) {
            this.mhudAvailable = true;
            return true;
        }
        this.clearMhudBindings();
        if (this.tryResolveMhudFromPluginManager()) {
            return true;
        }
        if (this.tryResolveMhudByClassName(MHUD_CLASS)) {
            return true;
        }
        if (this.tryResolveMhudByClassName(MHUD_CLASS_LEGACY)) {
            return true;
        }
        return false;
    }

    private boolean tryResolveMhudFromPluginManager() {
        try {
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager == null) {
                return false;
            }
            PluginBase plugin = pluginManager.getPlugin(MHUD_PLUGIN_ID);
            if (plugin == null) {
                for (PluginBase candidate : pluginManager.getPlugins()) {
                    String className = candidate.getClass().getName();
                    if (!MHUD_CLASS.equals(className) && !MHUD_CLASS_LEGACY.equals(className)) continue;
                    plugin = candidate;
                    break;
                }
            }
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }
            // Resolve API from the real plugin instance to avoid cross-classloader Class.forName failures.
            if (!this.bindMhudApi(plugin.getClass())) {
                return false;
            }
            this.mhudInstance = plugin;
            this.mhudAvailable = true;
            this.logMhudDetected(plugin.getClass().getName(), "PluginManager");
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    private boolean tryResolveMhudByClassName(String className) {
        try {
            Class<?> mhudClass = this.loadMhudClass(className);
            if (mhudClass == null || !this.bindMhudApi(mhudClass)) {
                return false;
            }
            Object instance = this.resolveMhudInstance();
            if (instance == null) {
                return false;
            }
            this.mhudAvailable = true;
            this.logMhudDetected(mhudClass.getName(), "reflection");
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    private Class<?> loadMhudClass(String className) {
        try {
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager != null && pluginManager.getBridgeClassLoader() != null) {
                try {
                    return Class.forName(className, false, pluginManager.getBridgeClassLoader());
                }
                catch (ClassNotFoundException ignored) {
                    // Fallbacks below.
                }
            }
        }
        catch (Exception ignored) {
            // Fallbacks below.
        }
        try {
            return Class.forName(className);
        }
        catch (ClassNotFoundException ignored) {
            // Final fallback below.
        }
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            return contextClassLoader == null ? null : contextClassLoader.loadClass(className);
        }
        catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private boolean bindMhudApi(Class<?> mhudClass) {
        try {
            this.mhudSetCustomHudMethod = mhudClass.getMethod("setCustomHud", Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            try {
                this.mhudHideCustomHudMethod = mhudClass.getMethod("hideCustomHud", Player.class, String.class);
            }
            catch (NoSuchMethodException ignored) {
                this.mhudHideCustomHudMethod = mhudClass.getMethod("hideCustomHud", Player.class, PlayerRef.class, String.class);
            }
            try {
                this.mhudGetInstanceMethod = mhudClass.getMethod("getInstance");
            }
            catch (NoSuchMethodException ignored) {
                this.mhudGetInstanceMethod = null;
            }
            return true;
        }
        catch (Exception ignored) {
            this.clearMhudBindings();
            return false;
        }
    }

    private void clearMhudBindings() {
        this.mhudAvailable = false;
        this.mhudInstance = null;
        this.mhudGetInstanceMethod = null;
        this.mhudSetCustomHudMethod = null;
        this.mhudHideCustomHudMethod = null;
        this.mhudResolvedClassName = null;
    }

    private void invalidateMhudBindings() {
        this.clearMhudBindings();
        this.nextMhudResolveAttemptAtMs = System.currentTimeMillis() + MHUD_RESOLVE_RETRY_MILLIS;
    }

    private void logMhudDetected(String className, String source) {
        if (className == null) {
            return;
        }
        if (!className.equals(this.mhudResolvedClassName)) {
            this.mhudResolvedClassName = className;
            LOGGER.log(Level.INFO, "MHUD detected via " + source + " (" + className + "). Horde HUD will run in multiplexed mode.");
        }
    }

    private boolean pushHudUpdateThroughMhud(Player player, PlayerRef playerRef, HordeRuntimeHud hud) {
        try {
            Object instance = this.getMhudInstance();
            if (instance == null || this.mhudSetCustomHudMethod == null) {
                return false;
            }
            this.mhudSetCustomHudMethod.invoke(instance, player, playerRef, MHUD_IDENTIFIER, hud);
            return true;
        }
        catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to push Horde HUD through MHUD.", ex);
            this.invalidateMhudBindings();
            return false;
        }
    }

    private void hideHudThroughMhud(Player player, PlayerRef playerRef) {
        try {
            Object instance = this.getMhudInstance();
            if (instance == null || this.mhudHideCustomHudMethod == null) {
                return;
            }
            if (this.mhudHideCustomHudMethod.getParameterCount() == 2) {
                this.mhudHideCustomHudMethod.invoke(instance, player, MHUD_IDENTIFIER);
            } else {
                this.mhudHideCustomHudMethod.invoke(instance, player, playerRef, MHUD_IDENTIFIER);
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to remove Horde HUD from MHUD.", ex);
            this.invalidateMhudBindings();
        }
    }

    private Object getMhudInstance() {
        Object instance = this.resolveMhudInstance();
        if (instance != null) {
            return instance;
        }
        this.invalidateMhudBindings();
        return null;
    }

    private Object resolveMhudInstance() {
        try {
            if (this.mhudInstance instanceof PluginBase plugin) {
                if (!plugin.isEnabled()) {
                    return null;
                }
                return this.mhudInstance;
            }
            if (this.mhudInstance != null) {
                return this.mhudInstance;
            }
            if (this.mhudGetInstanceMethod == null) {
                return null;
            }
            Object instance = this.mhudGetInstanceMethod.invoke(null);
            if (instance != null) {
                this.mhudInstance = instance;
            }
            return instance;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private enum HudAttachMode {
        DIRECT,
        MHUD
    }

    private static final class EmptyRuntimeHud
    extends CustomUIHud {
        private EmptyRuntimeHud(PlayerRef playerRef) {
            super(playerRef);
        }

        protected void build(UICommandBuilder commandBuilder) {
            // Intentionally empty. Sends an overwrite packet with no UI commands.
        }
    }

    private static final class HordeRuntimeHud
    extends CustomUIHud {
        private static final String LAYOUT = "Hud/HordeArenaHud.ui";
        private final HordeService hordeService;
        private HordeService.StatusSnapshot snapshot;

        private HordeRuntimeHud(PlayerRef playerRef, HordeService hordeService) {
            super(playerRef);
            this.hordeService = hordeService;
            this.snapshot = hordeService.getStatusSnapshot();
        }

        private void setSnapshot(HordeService.StatusSnapshot snapshot) {
            this.snapshot = snapshot == null ? this.hordeService.getStatusSnapshot() : snapshot;
        }

        protected void build(UICommandBuilder commandBuilder) {
            commandBuilder.append(LAYOUT);
            this.updateHudValues(commandBuilder);
        }

        private void updateHud() {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            try {
                this.updateHudValues(commandBuilder);
                this.update(false, commandBuilder);
            }
            catch (Exception ex) {
                UICommandBuilder rebuildBuilder = new UICommandBuilder();
                try {
                    this.build(rebuildBuilder);
                    this.update(true, rebuildBuilder);
                }
                catch (Exception ignored) {
                    // Keep HUD silent if update fails repeatedly.
                }
            }
        }

        private void updateHudValues(UICommandBuilder commandBuilder) {
            HordeService.StatusSnapshot status = this.snapshot == null ? this.hordeService.getStatusSnapshot() : this.snapshot;
            String language = HordeService.normalizeLanguage(status.language);
            boolean english = HordeService.isEnglishLanguage(language);
            String worldText = status.worldName == null || status.worldName.isBlank() ? "default" : status.worldName;
            String stateLine = HordeI18n.translateLegacy(language, english ? "State: " + (status.active ? "Active" : "Inactive") + " | World: " + worldText : "Estado: " + (status.active ? "Activa" : "Inactiva") + " | Mundo: " + worldText);
            String roundLine = HordeI18n.translateLegacy(language, english ? "Round: " + status.currentRound + "/" + status.totalRounds : "Ronda: " + status.currentRound + "/" + status.totalRounds);
            String enemiesLine = HordeI18n.translateLegacy(language, english ? "Enemies alive: " + status.aliveEnemies : "Enemigos vivos: " + status.aliveEnemies);
            String killsLine = HordeI18n.translateLegacy(language, english ? "Kills: " + status.totalKilled + " | Deaths: " + status.totalDeaths : "Bajas: " + status.totalKilled + " | Muertes: " + status.totalDeaths);
            String nextLine = HordeI18n.translateLegacy(language, english ? "Next round: " + (status.nextRoundInSeconds > 0L ? status.nextRoundInSeconds + "s" : "-") : "Siguiente: " + (status.nextRoundInSeconds > 0L ? status.nextRoundInSeconds + "s" : "-"));
            String rewardLine = this.buildRewardLine(language, english);
            commandBuilder.set("#TitleLabel.Text", HordeI18n.translateLegacy(language, english ? "HORDE PVE" : "HORDA PVE")).set("#StateLine.Text", stateLine).set("#RoundLine.Text", roundLine).set("#EnemiesLine.Text", enemiesLine).set("#KillsLine.Text", killsLine).set("#NextLine.Text", nextLine).set("#RewardLine.Text", rewardLine);
        }

        private String buildRewardLine(String language, boolean english) {
            HordeService.HordeConfig config = this.hordeService.getConfigSnapshot();
            String itemId = config.rewardItemId == null ? "" : config.rewardItemId.trim();
            int quantity = Math.max(1, config.rewardItemQuantity);
            String mode;
            if (itemId.isBlank()) {
                mode = english ? "none" : "ninguno";
            } else if ("random".equalsIgnoreCase(itemId)) {
                mode = english ? "random" : "aleatorio";
            } else {
                mode = itemId;
            }
            if (english) {
                return HordeI18n.translateLegacy(language, "Reward: " + mode + " x" + quantity + " | Every " + config.rewardEveryRounds + " round(s)");
            }
            return HordeI18n.translateLegacy(language, "Recompensa: " + mode + " x" + quantity + " | Cada " + config.rewardEveryRounds + " ronda(s)");
        }
    }
}
