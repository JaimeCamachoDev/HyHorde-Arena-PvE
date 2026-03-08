package com.hyhorde.arenapve.horde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hyhorde.arenapve.horde.HordeStatusPage;
import it.unimi.dsi.fastutil.Pair;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HordeService {
    private static final String[] ENEMY_ROLE_HINTS = new String[]{"enemy", "hostile", "bandit", "goblin", "skeleton", "zombie", "spider", "wolf", "wraith", "void", "demon", "beast"};
    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 200;
    private static final int MIN_ENEMIES_PER_ROUND = 1;
    private static final int MAX_ENEMIES_PER_ROUND = 250;
    private static final int MIN_ENEMY_INCREMENT = 0;
    private static final int MAX_ENEMY_INCREMENT = 250;
    private static final int MIN_WAVE_DELAY_SECONDS = 0;
    private static final int MAX_WAVE_DELAY_SECONDS = 300;
    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 128.0;
    private final PluginBase plugin;
    private final Gson gson;
    private final Path configPath;
    private final Map<UUID, HordeStatusPage> statusPages;
    private HordeConfig config;
    private HordeSession session;

    public HordeService(PluginBase plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configPath = plugin.getDataDirectory().resolve("horde-config.json");
        this.statusPages = new HashMap<UUID, HordeStatusPage>();
        this.config = HordeConfig.defaults();
        this.loadConfig();
    }

    public synchronized HordeConfig getConfigSnapshot() {
        return this.config.copy();
    }

    public synchronized boolean isActive() {
        return this.session != null;
    }

    public synchronized String getStatusLine() {
        if (this.session == null) {
            return "Sin horda activa. Usa /hordapve para abrir la interfaz.";
        }
        int alive = HordeService.countAlive(this.session.activeEnemies);
        return "Horda activa | Ronda " + this.session.currentRound + "/" + this.config.rounds + " | Enemigos vivos: " + alive + " | Spawn total: " + this.session.totalSpawned + " | Kills detectadas: " + this.session.totalKilled + " | Rol: " + this.session.role + " | Recompensa cada: " + this.config.rewardEveryRounds + " ronda(s)";
    }

    public synchronized List<String> getAvailableRoles() {
        return new ArrayList<String>(NPCPlugin.get().getRoleTemplateNames(true));
    }

    public synchronized StatusSnapshot getStatusSnapshot() {
        return this.createStatusSnapshot(this.session == null ? null : this.session.world);
    }

    public synchronized OperationResult reloadConfigFromDisk() {
        this.loadConfig();
        this.refreshStatusHud(this.session);
        return OperationResult.ok("Configuracion de hordas recargada desde disco.");
    }

    public synchronized OperationResult openStatusHud(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, PlayerRef playerRef, World world) {
        Player player = (Player)store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return OperationResult.fail("No se pudo abrir el panel de estado ahora mismo.");
        }
        HordeStatusPage previous = this.statusPages.remove(playerRef.getUuid());
        if (previous != null) {
            previous.closeFromService();
        }
        StatusSnapshot snapshot = this.createStatusSnapshot(world);
        HordeStatusPage page = HordeStatusPage.open(playerEntityRef, store, player, playerRef, snapshot, this::unregisterStatusPage);
        this.statusPages.put(playerRef.getUuid(), page);
        return OperationResult.ok("Panel de estado de horda abierto.");
    }

    public synchronized OperationResult setConfiguredRole(String roleInput) {
        String role;
        String string = role = roleInput == null ? "" : roleInput.trim();
        if (role.equalsIgnoreCase("auto")) {
            role = "";
        }
        this.config.npcRole = role;
        this.saveConfig();
        if (role.isEmpty()) {
            return OperationResult.ok("Rol de NPC en modo automatico.");
        }
        return OperationResult.ok("Rol de NPC configurado en: " + role);
    }

    public synchronized OperationResult setRewardEveryRounds(int everyRounds) {
        if (everyRounds <= 0 || everyRounds > 200) {
            return OperationResult.fail("reward debe estar entre 1 y 200.");
        }
        this.config.rewardEveryRounds = everyRounds;
        this.saveConfig();
        return OperationResult.ok("Recompensas configuradas cada " + everyRounds + " ronda(s).");
    }

    public synchronized OperationResult setSpawnFromPlayer(PlayerRef playerRef, World world) {
        Transform transform = playerRef.getTransform();
        Vector3d position = transform.getPosition();
        this.config.spawnConfigured = true;
        this.config.worldName = world.getName();
        this.config.spawnX = position.x;
        this.config.spawnY = position.y;
        this.config.spawnZ = position.z;
        this.saveConfig();
        return OperationResult.ok(String.format(Locale.ROOT, "Centro de horda guardado en %.2f %.2f %.2f (mundo: %s).", this.config.spawnX, this.config.spawnY, this.config.spawnZ, this.config.worldName));
    }

    public synchronized OperationResult applyUiConfig(Map<String, String> values, World world) {
        HordeConfig updated = this.config.copy();
        try {
            updated.spawnX = HordeService.parseDouble(values.get("spawnX"), updated.spawnX, "spawnX");
            updated.spawnY = HordeService.parseDouble(values.get("spawnY"), updated.spawnY, "spawnY");
            updated.spawnZ = HordeService.parseDouble(values.get("spawnZ"), updated.spawnZ, "spawnZ");
            updated.minSpawnRadius = HordeService.parseDouble(values.get("minRadius"), updated.minSpawnRadius, "minRadius");
            updated.maxSpawnRadius = HordeService.parseDouble(values.get("maxRadius"), updated.maxSpawnRadius, "maxRadius");
            updated.rounds = HordeService.parseInt(values.get("rounds"), updated.rounds, "rounds");
            updated.baseEnemiesPerRound = HordeService.parseInt(values.get("baseEnemies"), updated.baseEnemiesPerRound, "baseEnemies");
            updated.enemiesPerRoundIncrement = HordeService.parseInt(values.get("enemiesPerRound"), updated.enemiesPerRoundIncrement, "enemiesPerRound");
            updated.waveDelaySeconds = HordeService.parseInt(values.get("waveDelay"), updated.waveDelaySeconds, "waveDelay");
        }
        catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }
        String roleValue = values.get("role");
        if (roleValue != null) {
            String trimmedRole = roleValue.trim();
            String string = updated.npcRole = trimmedRole.equalsIgnoreCase("auto") ? "" : trimmedRole;
        }
        if (updated.minSpawnRadius < 1.0 || updated.minSpawnRadius > 128.0) {
            return OperationResult.fail("minRadius debe estar entre 1.0 y 128.0.");
        }
        if (updated.maxSpawnRadius < 1.0 || updated.maxSpawnRadius > 128.0) {
            return OperationResult.fail("maxRadius debe estar entre 1.0 y 128.0.");
        }
        if (updated.maxSpawnRadius < updated.minSpawnRadius) {
            return OperationResult.fail("maxRadius debe ser mayor o igual a minRadius.");
        }
        if (updated.rounds < 1 || updated.rounds > 200) {
            return OperationResult.fail("rounds debe estar entre 1 y 200.");
        }
        if (updated.baseEnemiesPerRound < 1 || updated.baseEnemiesPerRound > 250) {
            return OperationResult.fail("baseEnemies debe estar entre 1 y 250.");
        }
        if (updated.enemiesPerRoundIncrement < 0 || updated.enemiesPerRoundIncrement > 250) {
            return OperationResult.fail("enemiesPerRound debe estar entre 0 y 250.");
        }
        if (updated.waveDelaySeconds < 0 || updated.waveDelaySeconds > 300) {
            return OperationResult.fail("waveDelay debe estar entre 0 y 300 segundos.");
        }
        updated.spawnConfigured = true;
        updated.worldName = world.getName();
        this.config = updated;
        this.saveConfig();
        return OperationResult.ok("Configuracion de hordas guardada.");
    }

    public synchronized OperationResult start(Store<EntityStore> store, PlayerRef startedBy, World world) {
        HordeSession newSession;
        if (this.session != null) {
            return OperationResult.fail("Ya hay una horda activa.");
        }
        List roles = NPCPlugin.get().getRoleTemplateNames(true);
        if (roles.isEmpty()) {
            return OperationResult.fail("No hay roles de NPC disponibles.");
        }
        if (!this.config.spawnConfigured || !Objects.equals(this.config.worldName, world.getName())) {
            Transform transform = startedBy.getTransform();
            this.config.spawnConfigured = true;
            this.config.worldName = world.getName();
            this.config.spawnX = transform.getPosition().x;
            this.config.spawnY = transform.getPosition().y;
            this.config.spawnZ = transform.getPosition().z;
            this.saveConfig();
        }
        String selectedRole = HordeService.chooseRole(roles, this.config.npcRole);
        this.session = newSession = new HordeSession(world, store, selectedRole);
        newSession.ticker = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> this.tickSession(newSession), 1000L, 1000L, TimeUnit.MILLISECONDS);
        world.sendMessage(Message.raw((String)String.format(Locale.ROOT, "Horda PVE iniciada en %.2f %.2f %.2f | %d rondas | rol: %s", this.config.spawnX, this.config.spawnY, this.config.spawnZ, this.config.rounds, selectedRole)));
        this.spawnNextRound(newSession, new Vector3f(startedBy.getTransform().getRotation()));
        Ref startedByRef = startedBy.getReference();
        if (startedByRef != null && startedByRef.isValid()) {
            this.openStatusHud((Ref<EntityStore>)startedByRef, store, startedBy, world);
        }
        return OperationResult.ok("Horda iniciada.");
    }

    public synchronized OperationResult stop(boolean cleanupAliveEnemies) {
        if (this.session == null) {
            return OperationResult.fail("No hay horda activa.");
        }
        this.endSession(this.session, "Horda detenida manualmente.", cleanupAliveEnemies);
        return OperationResult.ok("Horda detenida.");
    }

    public synchronized void shutdown() {
        if (this.session == null) {
            this.closeAllStatusPages();
            return;
        }
        this.endSession(this.session, "Horda finalizada por apagado del plugin.", false);
        this.closeAllStatusPages();
    }

    private synchronized void tickSession(HordeSession trackedSession) {
        if (this.session != trackedSession) {
            return;
        }
        World world = trackedSession.world;
        if (!world.isAlive()) {
            this.endSession(trackedSession, "Horda finalizada: el mundo dejo de estar activo.", false);
            return;
        }
        world.execute(() -> {
            HordeService hordeService = this;
            synchronized (hordeService) {
                if (this.session != trackedSession) {
                    return;
                }
                int removed = HordeService.removeInvalidRefs(trackedSession.activeEnemies);
                if (removed > 0) {
                    trackedSession.totalKilled += removed;
                }
                if (trackedSession.roundActive && trackedSession.activeEnemies.isEmpty()) {
                    trackedSession.roundActive = false;
                    this.grantRoundRewards(trackedSession, trackedSession.currentRound);
                    if (trackedSession.currentRound >= this.config.rounds) {
                        this.endSession(trackedSession, "Horda completada. Todas las rondas terminadas.", false);
                        return;
                    }
                    trackedSession.nextRoundAtMillis = System.currentTimeMillis() + (long)this.config.waveDelaySeconds * 1000L;
                    world.sendMessage(Message.raw((String)("Ronda " + trackedSession.currentRound + " completada. La siguiente ronda empieza en " + this.config.waveDelaySeconds + "s.")));
                }
                if (!trackedSession.roundActive && trackedSession.currentRound < this.config.rounds && System.currentTimeMillis() >= trackedSession.nextRoundAtMillis) {
                    this.spawnNextRound(trackedSession, Vector3f.ZERO);
                }
                this.refreshStatusHud(trackedSession);
            }
        });
    }

    private void spawnNextRound(HordeSession sessionToAdvance, Vector3f baseRotation) {
        int nextRound = sessionToAdvance.currentRound + 1;
        int targetCount = this.config.baseEnemiesPerRound + (nextRound - 1) * this.config.enemiesPerRoundIncrement;
        targetCount = Math.max(1, targetCount);
        Vector3d center = new Vector3d(this.config.spawnX, this.config.spawnY, this.config.spawnZ);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        for (int i = 0; i < targetCount; ++i) {
            double angle = random.nextDouble(0.0, Math.PI * 2);
            double distance = random.nextDouble(this.config.minSpawnRadius, this.config.maxSpawnRadius);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            Vector3d spawnPosition = new Vector3d(center).add(offsetX, 0.0, offsetZ);
            try {
                Pair created = NPCPlugin.get().spawnNPC(sessionToAdvance.store, sessionToAdvance.role, null, spawnPosition, new Vector3f(baseRotation));
                if (created == null || created.left() == null) continue;
                sessionToAdvance.activeEnemies.add((Ref<EntityStore>)((Ref)created.left()));
                ++sessionToAdvance.totalSpawned;
                ++spawned;
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (spawned == 0) {
            this.endSession(sessionToAdvance, "La horda se cancelo: no se pudo spawnear ningun NPC con el rol " + sessionToAdvance.role + ".", false);
            return;
        }
        sessionToAdvance.currentRound = nextRound;
        sessionToAdvance.roundActive = true;
        sessionToAdvance.nextRoundAtMillis = 0L;
        sessionToAdvance.world.sendMessage(Message.raw((String)("Ronda " + nextRound + "/" + this.config.rounds + " iniciada: " + spawned + " enemigos.")));
    }

    private void grantRoundRewards(HordeSession trackedSession, int completedRound) {
        if (completedRound <= 0 || this.config.rewardEveryRounds <= 0) {
            return;
        }
        if (completedRound % this.config.rewardEveryRounds != 0) {
            return;
        }
        trackedSession.world.sendMessage(Message.raw((String)("Recompensa desbloqueada por completar la ronda " + completedRound + ".")));
        if (this.config.rewardCommands == null || this.config.rewardCommands.isEmpty()) {
            return;
        }
        for (PlayerRef playerRef : trackedSession.world.getPlayerRefs()) {
            if (playerRef == null || !playerRef.isValid()) continue;
            String playerName = playerRef.getUsername();
            for (String template : this.config.rewardCommands) {
                if (template == null || template.isBlank()) continue;
                String command = template.trim().replace("%player%", playerName).replace("%round%", Integer.toString(completedRound));
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                try {
                    CommandManager.get().handleCommand(playerRef, command);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().at(Level.WARNING).log("No se pudo ejecutar comando de recompensa '%s': %s", (Object)command, (Object)ex.getMessage());
                }
            }
        }
    }

    private void endSession(HordeSession trackedSession, String reason, boolean cleanupAliveEnemies) {
        if (this.session != trackedSession) {
            return;
        }
        if (trackedSession.ticker != null) {
            trackedSession.ticker.cancel(false);
        }
        if (cleanupAliveEnemies) {
            for (Ref<EntityStore> enemyRef : trackedSession.activeEnemies) {
                if (enemyRef == null || !enemyRef.isValid()) continue;
                try {
                    trackedSession.store.removeEntity(enemyRef, RemoveReason.REMOVE);
                }
                catch (Exception exception) {}
            }
        }
        int aliveAtEnd = HordeService.countAlive(trackedSession.activeEnemies);
        trackedSession.activeEnemies.clear();
        this.session = null;
        trackedSession.world.sendMessage(Message.raw((String)(reason + " (vivos restantes: " + aliveAtEnd + ").")));
        this.refreshStatusHud(null);
    }

    private StatusSnapshot createStatusSnapshot(World requestedWorld) {
        if (this.session == null) {
            String worldName = requestedWorld != null ? requestedWorld.getName() : this.config.worldName;
            return StatusSnapshot.inactive(this.config.rounds, worldName);
        }
        int alive = HordeService.countAlive(this.session.activeEnemies);
        long now = System.currentTimeMillis();
        long elapsedSeconds = Math.max(0L, (now - this.session.startedAtMillis) / 1000L);
        long nextRoundInSeconds = this.session.roundActive ? 0L : Math.max(0L, (this.session.nextRoundAtMillis - now + 999L) / 1000L);
        return StatusSnapshot.active(this.session.currentRound, this.config.rounds, alive, this.session.totalSpawned, this.session.totalKilled, this.session.role, elapsedSeconds, nextRoundInSeconds, this.session.world.getName());
    }

    private void refreshStatusHud(HordeSession trackedSession) {
        if (this.statusPages.isEmpty()) {
            return;
        }
        World world = trackedSession == null ? null : trackedSession.world;
        StatusSnapshot snapshot = this.createStatusSnapshot(world);
        ArrayList<UUID> staleEntries = new ArrayList<UUID>();
        for (Map.Entry<UUID, HordeStatusPage> entry : this.statusPages.entrySet()) {
            HordeStatusPage page = entry.getValue();
            if (page == null) {
                staleEntries.add(entry.getKey());
                continue;
            }
            try {
                page.updateSnapshot(snapshot);
            }
            catch (Exception ex) {
                staleEntries.add(entry.getKey());
            }
        }
        for (UUID staleEntry : staleEntries) {
            this.statusPages.remove(staleEntry);
        }
    }

    private void unregisterStatusPage(UUID playerId) {
        this.statusPages.remove(playerId);
    }

    private void closeAllStatusPages() {
        for (HordeStatusPage page : this.statusPages.values()) {
            if (page == null) continue;
            try {
                page.closeFromService();
            }
            catch (Exception exception) {}
        }
        this.statusPages.clear();
    }

    private static String chooseRole(List<String> roles, String configuredRole) {
        if (configuredRole != null && !configuredRole.isBlank()) {
            for (String role : roles) {
                if (!role.equalsIgnoreCase(configuredRole)) continue;
                return role;
            }
        }
        for (String string : ENEMY_ROLE_HINTS) {
            for (String role : roles) {
                if (!role.equalsIgnoreCase(string)) continue;
                return role;
            }
        }
        for (String string : ENEMY_ROLE_HINTS) {
            for (String role : roles) {
                if (!role.toLowerCase(Locale.ROOT).contains(string)) continue;
                return role;
            }
        }
        return roles.get(0);
    }

    private static int removeInvalidRefs(Set<Ref<EntityStore>> refs) {
        int removed = 0;
        HashSet<Ref<EntityStore>> stale = new HashSet<Ref<EntityStore>>();
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) continue;
            stale.add(ref);
        }
        for (Ref ref : stale) {
            refs.remove(ref);
            ++removed;
        }
        return removed;
    }

    private static int countAlive(Set<Ref<EntityStore>> refs) {
        int alive = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid()) continue;
            ++alive;
        }
        return alive;
    }

    private static int parseInt(String input, int currentValue, String name) {
        if (input == null || input.isBlank()) {
            return currentValue;
        }
        try {
            return Integer.parseInt(HordeService.normalizeNumberInput(input, false));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " debe ser un numero entero. Valor recibido: " + input);
        }
    }

    private static double parseDouble(String input, double currentValue, String name) {
        if (input == null || input.isBlank()) {
            return currentValue;
        }
        try {
            return Double.parseDouble(HordeService.normalizeNumberInput(input, true));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " debe ser un numero decimal valido. Valor recibido: " + input);
        }
    }

    private static String normalizeNumberInput(String input, boolean decimal) {
        String normalized = input.trim().replace(',', '.');
        if (!decimal && normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }

    private static String safeRoleValue(String role) {
        return role == null ? "" : role;
    }

    private void loadConfig() {
        try {
            Files.createDirectories(this.plugin.getDataDirectory(), new FileAttribute[0]);
        }
        catch (IOException ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo crear la carpeta de datos para HordeService: %s", (Object)ex.getMessage());
            return;
        }
        if (!Files.exists(this.configPath, new LinkOption[0])) {
            this.saveConfig();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(this.configPath, StandardCharsets.UTF_8);){
            HordeConfig loaded = (HordeConfig)this.gson.fromJson((Reader)reader, HordeConfig.class);
            if (loaded != null) {
                this.config = HordeService.sanitize(loaded);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo leer horde-config.json, se usaran valores por defecto: %s", (Object)ex.getMessage());
            this.config = HordeConfig.defaults();
            this.saveConfig();
        }
    }

    private void saveConfig() {
        try {
            Files.createDirectories(this.plugin.getDataDirectory(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(this.configPath, StandardCharsets.UTF_8, new OpenOption[0]);){
                this.gson.toJson((Object)this.config, (Appendable)writer);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().at(Level.WARNING).log("No se pudo guardar la configuracion de hordas: %s", (Object)ex.getMessage());
        }
    }

    private static HordeConfig sanitize(HordeConfig source) {
        HordeConfig sanitized = source.copy();
        if (sanitized.rounds < 1) {
            sanitized.rounds = HordeConfig.defaults().rounds;
        } else if (sanitized.rounds > 200) {
            sanitized.rounds = 200;
        }
        if (sanitized.baseEnemiesPerRound < 1) {
            sanitized.baseEnemiesPerRound = HordeConfig.defaults().baseEnemiesPerRound;
        } else if (sanitized.baseEnemiesPerRound > 250) {
            sanitized.baseEnemiesPerRound = 250;
        }
        if (sanitized.enemiesPerRoundIncrement < 0) {
            sanitized.enemiesPerRoundIncrement = HordeConfig.defaults().enemiesPerRoundIncrement;
        } else if (sanitized.enemiesPerRoundIncrement > 250) {
            sanitized.enemiesPerRoundIncrement = 250;
        }
        if (sanitized.waveDelaySeconds < 0) {
            sanitized.waveDelaySeconds = HordeConfig.defaults().waveDelaySeconds;
        } else if (sanitized.waveDelaySeconds > 300) {
            sanitized.waveDelaySeconds = 300;
        }
        if (sanitized.rewardEveryRounds <= 0) {
            sanitized.rewardEveryRounds = HordeConfig.defaults().rewardEveryRounds;
        }
        if (sanitized.rewardCommands == null) {
            sanitized.rewardCommands = new ArrayList<String>(HordeConfig.defaults().rewardCommands);
        }
        sanitized.minSpawnRadius = HordeService.clamp(sanitized.minSpawnRadius, 1.0, 128.0);
        sanitized.maxSpawnRadius = HordeService.clamp(sanitized.maxSpawnRadius, 1.0, 128.0);
        if (sanitized.maxSpawnRadius < sanitized.minSpawnRadius) {
            double temp = sanitized.minSpawnRadius;
            sanitized.minSpawnRadius = sanitized.maxSpawnRadius;
            sanitized.maxSpawnRadius = temp;
        }
        sanitized.npcRole = HordeService.safeRoleValue(sanitized.npcRole).trim();
        sanitized.worldName = sanitized.worldName == null || sanitized.worldName.isBlank() ? "default" : sanitized.worldName;
        return sanitized;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static final class HordeConfig {
        public boolean spawnConfigured;
        public String worldName;
        public double spawnX;
        public double spawnY;
        public double spawnZ;
        public double minSpawnRadius;
        public double maxSpawnRadius;
        public int rounds;
        public int baseEnemiesPerRound;
        public int enemiesPerRoundIncrement;
        public int waveDelaySeconds;
        public String npcRole;
        public int rewardEveryRounds;
        public List<String> rewardCommands;

        public static HordeConfig defaults() {
            HordeConfig defaults = new HordeConfig();
            defaults.spawnConfigured = false;
            defaults.worldName = "default";
            defaults.spawnX = 0.0;
            defaults.spawnY = 64.0;
            defaults.spawnZ = 0.0;
            defaults.minSpawnRadius = 5.0;
            defaults.maxSpawnRadius = 12.0;
            defaults.rounds = 5;
            defaults.baseEnemiesPerRound = 8;
            defaults.enemiesPerRoundIncrement = 2;
            defaults.waveDelaySeconds = 8;
            defaults.npcRole = "";
            defaults.rewardEveryRounds = 2;
            defaults.rewardCommands = new ArrayList<String>();
            return defaults;
        }

        public HordeConfig copy() {
            HordeConfig copy = new HordeConfig();
            copy.spawnConfigured = this.spawnConfigured;
            copy.worldName = this.worldName;
            copy.spawnX = this.spawnX;
            copy.spawnY = this.spawnY;
            copy.spawnZ = this.spawnZ;
            copy.minSpawnRadius = this.minSpawnRadius;
            copy.maxSpawnRadius = this.maxSpawnRadius;
            copy.rounds = this.rounds;
            copy.baseEnemiesPerRound = this.baseEnemiesPerRound;
            copy.enemiesPerRoundIncrement = this.enemiesPerRoundIncrement;
            copy.waveDelaySeconds = this.waveDelaySeconds;
            copy.npcRole = this.npcRole;
            copy.rewardEveryRounds = this.rewardEveryRounds;
            copy.rewardCommands = this.rewardCommands == null ? new ArrayList<String>() : new ArrayList<String>(this.rewardCommands);
            return copy;
        }
    }

    private static final class HordeSession {
        private final World world;
        private final Store<EntityStore> store;
        private final String role;
        private final Set<Ref<EntityStore>> activeEnemies;
        private int currentRound;
        private int totalSpawned;
        private int totalKilled;
        private boolean roundActive;
        private long nextRoundAtMillis;
        private final long startedAtMillis;
        private ScheduledFuture<?> ticker;

        private HordeSession(World world, Store<EntityStore> store, String role) {
            this.world = world;
            this.store = store;
            this.role = role;
            this.activeEnemies = new HashSet<Ref<EntityStore>>();
            this.currentRound = 0;
            this.totalSpawned = 0;
            this.totalKilled = 0;
            this.roundActive = false;
            this.nextRoundAtMillis = 0L;
            this.startedAtMillis = System.currentTimeMillis();
        }
    }

    public static final class StatusSnapshot {
        public final boolean active;
        public final int currentRound;
        public final int totalRounds;
        public final int aliveEnemies;
        public final int totalSpawned;
        public final int totalKilled;
        public final String role;
        public final long elapsedSeconds;
        public final long nextRoundInSeconds;
        public final String worldName;

        private StatusSnapshot(boolean active, int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName) {
            this.active = active;
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
            this.aliveEnemies = aliveEnemies;
            this.totalSpawned = totalSpawned;
            this.totalKilled = totalKilled;
            this.role = role == null ? "" : role;
            this.elapsedSeconds = Math.max(0L, elapsedSeconds);
            this.nextRoundInSeconds = Math.max(0L, nextRoundInSeconds);
            this.worldName = worldName == null ? "default" : worldName;
        }

        public static StatusSnapshot inactive(int totalRounds, String worldName) {
            return new StatusSnapshot(false, 0, totalRounds, 0, 0, 0, "", 0L, 0L, worldName);
        }

        public static StatusSnapshot active(int currentRound, int totalRounds, int aliveEnemies, int totalSpawned, int totalKilled, String role, long elapsedSeconds, long nextRoundInSeconds, String worldName) {
            return new StatusSnapshot(true, currentRound, totalRounds, aliveEnemies, totalSpawned, totalKilled, role, elapsedSeconds, nextRoundInSeconds, worldName);
        }
    }

    public static final class OperationResult {
        private final boolean success;
        private final String message;

        private OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OperationResult ok(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult fail(String message) {
            return new OperationResult(false, message);
        }

        public boolean isSuccess() {
            return this.success;
        }

        public String getMessage() {
            return this.message;
        }
    }
}


