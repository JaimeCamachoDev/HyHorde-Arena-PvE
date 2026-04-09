# Error History

## 2026-04-09 - Server crash after latest UI changes (investigation)

### Symptom
- Player disconnects with `Crash - Crash` shortly after joining.
- World teardown follows with `Exception when adding player to world`.

### Evidence (logs)
- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs\2026-04-09_21-06-37_server.log`
- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs\2026-04-09_21-06-19_server.log`

Key signatures:
- `Failed to load manifest for pack at mods\VZ.HytaleMod_Test`
- `CodecException: Failed to decode`
- `java.io.IOException: Unexpected character: 22, '"' expected '{'!`
- Follow-up runtime shutdown noise:
  - `java.lang.IllegalStateException: Store is shutdown!`
  - `CompletionException: CancellationException`

### Root cause status
- **Confirmed**: malformed JSON in an external pack manifest (`mods\VZ.HytaleMod_Test`) triggers decode failure during `AssetModule` setup.
- **Not confirmed**: direct crash caused by the latest HyHorde UI dropdown sizing change. No `ThirdParty(HyHorde:ArenaPVE)` stacktrace appears in these logs for this incident.

### Impact
- Asset loading enters a degraded state at startup.
- Session becomes unstable and ends in disconnect/shutdown chain.

### Mitigation
1. Remove or fix `mods\VZ.HytaleMod_Test\manifest.json` (JSON starts with invalid token; expected `{`).
2. Retest with only HyHorde enabled (disable unrelated packs with invalid manifests).
3. If crash persists after isolation, capture a fresh log and grep for:
   - `ThirdParty(HyHorde:ArenaPVE)`
   - `HordeConfigPage`
   - first `Caused by` block after player join.

### Notes
- The repeated warnings `missing or invalid manifest.json` for other folders in `mods` are additional noise and should be cleaned up for reliable diagnosis.
- Applied in local test environment (2026-04-09):
  - Rewrote `mods\VZ.HytaleMod_Test\manifest.json` to a minimal schema-compatible manifest.
  - Removed non-essential fields from that manifest (`LoadBefore`, `SubPlugins`, author subfields) to avoid decode incompatibility.
  - Verified JSON validity for all discovered `manifest.json` files under `Mod-Test\mods`.

## 2026-04-09 - Boss stat modifiers with RPGLeveling installed

### Symptom
- In Boss Editor, multipliers (`HP`, `Damage`, `Size`, `Attack Rate`) appeared not to apply reliably when another leveling/stat mod was present.

### Root cause
- HyHorde applied boss modifiers immediately on spawn.
- RPG/stat systems can also touch enemy stats in early lifecycle frames, so immediate writes can be overwritten.

### Compatibility approach (non-dependent)
- No hard dependency on RPGLeveling was added.
- HyHorde now queues boss modifier application and retries for a short window:
  - initial delay: `1000ms`
  - retry interval: `500ms`
  - max attempts: `12`
- Successful sub-modifiers are tracked (`hp`, `size`, `attackRate`) to avoid repeated reapplication of already-applied values.
- Runtime damage multiplier map remains active from spawn, independent of optional mod presence.

### Outcome
- Works standalone (without RPGLeveling).
- Works with RPGLeveling present using delayed/retry reconciliation, without requiring RPGLeveling API linkage.

## 2026-04-09 - Boss XP duplicated with RPGLeveling

### Symptom
- Boss configured with custom XP (example: `10`) gave double XP:
  - base XP from RPGLeveling kill flow
  - plus extra XP injected by HyHorde

### Root cause
- Two independent award paths were active at the same kill:
  1. RPGLeveling native `ENTITY_KILL` grant.
  2. HyHorde manual `addXP(...)` call on boss death.

### Final solution (official RPG API path)
- Removed manual extra award from HyHorde death handler.
- Added a runtime listener through RPGLeveling API:
  - `RPGLevelingAPI.registerExperienceGainedListener(...)`
  - intercepts `ExperienceGainedEvent`
  - for `XPSource == ENTITY_KILL` and matching boss entity UUID:
    - force `event.setXpAmount(configuredBossXp)`
- Boss UUID mapping is armed on spawn using:
  - `UUIDComponent.getUuid()`
  - map `entityUuid -> configuredBossXp`
- For level override compatibility, HyHorde now uses RPGLeveling native flow:
  - `RPGLevelingPlugin.putSpawnLevelForEntity(entityUuid, level)`
  - updates `MobLevelData`
  - applies HP multiplier through RPGLeveling plugin methods.

### Why this avoids duplication
- XP is now controlled in a single place (RPGLeveling event stream), replacing amount instead of adding a second grant.

### Compatibility note
- Boss `Level` and `Experience points` parameters are **RPGLeveling-only**.
- If `RPGLeveling` is not installed, HyHorde treats them as safe no-op:
  - no crash
  - no error spam
  - no fallback stat mutation from these two parameters.

### Runtime verification logs
- `RPGLeveling XP override listener registered.`
- `Boss XP override armed | boss=<id> | xp=<n>`
- `Boss XP override applied | entity=<uuid> | xp=<n>`
