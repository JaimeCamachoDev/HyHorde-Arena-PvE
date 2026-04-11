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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HordeBossDamageScalingSystem extends EntityEventSystem<EntityStore, Damage> {
    private final HordeService hordeService;

    public HordeBossDamageScalingSystem(HordeService hordeService) {
        super(Damage.class);
        this.hordeService = hordeService;
    }

    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        if (damage == null || damage.isCancelled()) {
            return;
        }
        EntityStore entityStore = (EntityStore)store.getExternalData();
        if (entityStore == null || !this.hordeService.isTrackingWorld(entityStore.getWorld())) {
            return;
        }
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (victimRef != null) {
            float incomingMultiplier = this.hordeService.getTrackedBossIncomingDamageMultiplier(victimRef);
            if (Float.isFinite(incomingMultiplier) && Math.abs(incomingMultiplier - 1.0f) >= 0.0001f) {
                float incomingBase = Math.max(0.0f, damage.getAmount());
                float incomingScaled = Math.max(0.0f, incomingBase * incomingMultiplier);
                damage.setAmount(incomingScaled);
            }
        }
        Damage.Source source = damage.getSource();
        float damageMultiplier = this.resolveDamageMultiplierFromSource(source);
        if (!Float.isFinite(damageMultiplier) || Math.abs(damageMultiplier - 1.0f) < 0.0001f) {
            return;
        }
        float baseAmount = Math.max(0.0f, damage.getAmount());
        float scaledAmount = Math.max(0.0f, baseAmount * damageMultiplier);
        damage.setAmount(scaledAmount);
    }

    private float resolveDamageMultiplierFromSource(Damage.Source source) {
        if (source == null) {
            return 1.0f;
        }
        LinkedHashSet<Ref<EntityStore>> candidateRefs = new LinkedHashSet<Ref<EntityStore>>();
        if (source instanceof Damage.EntitySource) {
            Ref<EntityStore> directRef = ((Damage.EntitySource)source).getRef();
            if (directRef != null) {
                candidateRefs.add(directRef);
            }
        }
        candidateRefs.addAll(this.extractRefsByReflection(source));
        float resolved = 1.0f;
        for (Ref<EntityStore> ref : candidateRefs) {
            if (ref == null) {
                continue;
            }
            float multiplier = this.hordeService.getTrackedBossDamageMultiplier(ref);
            if (!Float.isFinite(multiplier)) {
                continue;
            }
            if (Math.abs(multiplier - 1.0f) < 0.0001f) {
                continue;
            }
            resolved = multiplier;
            break;
        }
        return resolved;
    }

    private Collection<Ref<EntityStore>> extractRefsByReflection(Object source) {
        ArrayList<Ref<EntityStore>> refs = new ArrayList<Ref<EntityStore>>();
        if (source == null) {
            return refs;
        }
        String[] candidateMethods = new String[]{
                "getRef",
                "getSourceRef",
                "getOwnerRef",
                "getAttackerRef",
                "getEntityRef",
                "getShooterRef",
                "getProjectileOwnerRef",
                "getCasterRef"
        };
        for (String methodName : candidateMethods) {
            this.tryCollectRefsFromMethod(source, methodName, refs);
        }
        return refs;
    }

    @SuppressWarnings("unchecked")
    private void tryCollectRefsFromMethod(Object source, String methodName, Collection<Ref<EntityStore>> out) {
        if (source == null || methodName == null || methodName.isBlank() || out == null) {
            return;
        }
        try {
            Method method = source.getClass().getMethod(methodName, new Class[0]);
            Object result = method.invoke(source, new Object[0]);
            this.collectRefsFromResult(result, out);
        }
        catch (Exception ignored) {
            // optional source accessors vary by runtime
        }
    }

    @SuppressWarnings("unchecked")
    private void collectRefsFromResult(Object result, Collection<Ref<EntityStore>> out) {
        if (result == null || out == null) {
            return;
        }
        if (result instanceof Ref<?>) {
            try {
                out.add((Ref<EntityStore>)result);
            }
            catch (Exception ignored) {
                // mismatched generic type, ignore
            }
            return;
        }
        if (result instanceof Collection<?>) {
            for (Object value : (Collection<?>)result) {
                this.collectRefsFromResult(value, out);
            }
            return;
        }
        if (result.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(result);
            for (int i = 0; i < length; ++i) {
                Object value = java.lang.reflect.Array.get(result, i);
                this.collectRefsFromResult(value, out);
            }
            return;
        }
        try {
            Method getRefMethod = result.getClass().getMethod("getRef", new Class[0]);
            Object nested = getRefMethod.invoke(result, new Object[0]);
            this.collectRefsFromResult(nested, out);
        }
        catch (Exception ignored) {
            // no nested ref available
        }
    }

    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class));
    }
}
