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
