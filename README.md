# HyHorde Arena PVE

Mod de hordas PVE por rondas para Hytale.  
Version actual: `1.5.0`.

## Novedades 1.5.0

- Compatibilidad con Update 4: `ServerVersion` fijada a `2026.03.26-89796e57b` para eliminar aviso de version objetivo no definida.
- Refactor de internacionalizacion para UI/HUD/comandos con prioridad de traduccion correcta al cambiar idioma.
- Ampliacion de diccionarios `pt/fr/de` para cubrir textos faltantes en editor, estado y ayuda.
- Ajustes de estabilidad en mensajes de UI para evitar rutas de texto sin traducir en idiomas no `es/en`.
- Guardado automatico del `arenaJoinRadius` desde la pestana de jugadores al mover el slider.

## Comandos

- `/hordahelp` -> ayuda rapida en chat.
- `/hordeconfig` -> abre la UI de configuracion (alias: `/hconfig`, `/hordecfg`, `/hordepve`, `/spawnve`, `/spawnpve`).
- `/hordeconfig start` -> inicia la horda.
- `/hordeconfig stop` -> detiene la horda y limpia enemigos de la sesion.
- `/hordeconfig status` -> muestra estado actual.
- `/hordeconfig logs` -> muestra ruta de logs.
- `/hordeconfig setspawn` -> guarda el centro de la horda en tu posicion.
- `/hordeconfig enemy <tipo>` -> cambia categoria de enemigos.
- `/hordeconfig tipos` -> diagnostico de categorias y roles detectados.
- `/hordeconfig role <rolNpc|auto>` -> fuerza rol NPC o vuelve a auto.
- `/hordeconfig roles` -> lista roles NPC disponibles.
- `/hordeconfig reward <rondas>` -> cada cuantas rondas se entrega recompensa.
- `/hordeconfig spectator <on|off>` -> marca preferencia espectador/jugador para el bloqueo al inicio.
- `/hordeconfig player` -> vuelve a modo jugador.
- `/hordeconfig arearadius <bloques>` -> radio de captura de participantes de arena.
- `/hordareload config` -> recarga configuraciones JSON del mod sin reiniciar.

## Tipos de enemigo

- `random`
- `random-all`
- `undead`
- `goblins`
- `scarak`
- `void`
- `wild`
- `elementals`

Nota: la disponibilidad real depende de los roles NPC de tu modpack. Usa `/hordeconfig tipos`.

## Flujo rapido

1. Ejecuta `/hordeconfig`.
2. Pulsa `Usar mi posicion actual` o ejecuta `/hordeconfig setspawn`.
3. Ajusta parametros por pestanas (`General`, `Horda`, `Jugadores`, `Sonidos`, `Recompensas`).
4. En `Jugadores`, define `arearadius` y el modo de cada jugador (jugador/espectador/salir).
5. Guarda con `Guardar config`.
6. Inicia con `/hordeconfig start`.
7. Deten con `/hordeconfig stop`.

## Configuracion del mod (JSON)

Todos los archivos viven en la carpeta de datos del plugin.

### 1) `horde-config.json`

Claves principales soportadas:

- Spawn y arena: `spawnConfigured`, `worldName`, `spawnX`, `spawnY`, `spawnZ`, `minSpawnRadius`, `maxSpawnRadius`, `arenaJoinRadius`.
- Rondas y dificultad: `rounds`, `baseEnemiesPerRound`, `enemiesPerRoundIncrement`, `waveDelaySeconds`, `playerMultiplier`.
- Enemigos: `enemyType`, `npcRole`, `finalBossEnabled`, `enemyLevelMin`, `enemyLevelMax` (sistema de niveles WIP/desactivado).
- Idioma/UI: `language` (`es` o `en`).
- Recompensas: `rewardEveryRounds`, `rewardCategory`, `rewardItemId`, `rewardItemQuantity`.
- Sonidos: `roundStartSoundId`, `roundVictorySoundId`.

### 2) `enemy-categories.json`

- Define categorias de horda y sus roles NPC.
- Permite configurar `finalBossRoles`.
- Permite excluir coincidencias por texto con `blockedRoleHints`.
- Plantilla base en `src/main/resources/enemy-categories.example.json`.

### 3) `reward-items.json`

- Define pool de items por categoria de recompensa.
- Alimenta el selector de recompensas de la UI.
- Compatible con `random` y `random_all`.
- Plantilla base en `src/main/resources/reward-items.example.json`.

### 4) `horde-sounds.json`

- Ajusta sugerencias y filtros para detectar sonidos de inicio/victoria.
- Campos: `roundStartHints`, `roundVictoryHints`, `roundStartBlockedKeywords`, `roundVictoryBlockedKeywords`, `weakKeywords`.
- Plantilla base en `src/main/resources/horde-sounds.example.json`.

## Recarga de configuracion

- `Recargar config` en la UI o `/hordareload config` recarga `horde-config.json` y JSON externos.
- La recarga en caliente del `.jar` no esta soportada: para actualizar binario del mod hay que reiniciar servidor.

## Build

```powershell
.\gradlew.bat clean jar
```

Salida esperada:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`

## Troubleshooting UI

- Guia de errores recurrentes de Custom UI: `docs/CUSTOM_UI_GUARDRAILS.md`
