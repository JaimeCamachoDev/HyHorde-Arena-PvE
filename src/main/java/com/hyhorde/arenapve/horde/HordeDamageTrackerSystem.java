package com.hyhorde.arenapve.horde;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;

public final class HordeDamageTrackerSystem
extends EntityEventSystem<EntityStore, Damage> {
    private final HordeService hordeService;

    public HordeDamageTrackerSystem(HordeService hordeService) {
        super(Damage.class);
        this.hordeService = hordeService;
    }

    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        PlayerRef victimPlayer;
        if (damage == null || damage.isCancelled()) {
            return;
        }
        EntityStore entityStore = (EntityStore)store.getExternalData();
        if (entityStore == null || !this.hordeService.isTrackingWorld(entityStore.getWorld())) {
            return;
        }
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (victimRef == null) {
            return;
        }
        EntityStatMap victimStats = (EntityStatMap)store.getComponent(victimRef, EntityStatMap.getComponentType());
        if (victimStats == null) {
            return;
        }
        EntityStatValue health = victimStats.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.get() - damage.getAmount() > 0.0f) {
            return;
        }
        Ref<EntityStore> sourceRef = null;
        PlayerRef attackerPlayer = null;
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource) {
            sourceRef = ((Damage.EntitySource)source).getRef();
            if (sourceRef != null) {
                attackerPlayer = (PlayerRef)store.getComponent(sourceRef, PlayerRef.getComponentType());
            }
        }
        if (attackerPlayer != null && this.hordeService.isTrackedEnemy(victimRef)) {
            this.hordeService.registerEnemyKill(victimRef, attackerPlayer);
            return;
        }
        if ((victimPlayer = (PlayerRef)chunk.getComponent(index, PlayerRef.getComponentType())) != null && sourceRef != null && this.hordeService.isTrackedEnemy(sourceRef)) {
            this.hordeService.registerPlayerDeath(victimPlayer, sourceRef);
        }
    }

    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class));
    }
}
