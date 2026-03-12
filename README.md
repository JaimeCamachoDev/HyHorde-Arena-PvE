# HyHorde Arena PVE

Mod de hordas PVE por rondas para Hytale.  
Version actual: `1.1.1`.

## Novedades 1.1.1

- Nueva pestana `Ayuda` dentro de la UI `Horde PVE Config`.
- En la pestana `Ayuda` se documentan comandos, claves de `horde-config.json` y JSON externos.
- README actualizado con flujo, comandos y configuracion extendida.

## Comandos

- `/hordahelp` -> ayuda rapida en chat.
- `/hordapve` -> abre la UI de configuracion (tambien `/hordepve`, `/spawnve`, `/spawnpve`).
- `/hordapve start` -> inicia la horda.
- `/hordapve stop` -> detiene la horda y limpia enemigos de la sesion.
- `/hordapve status` -> muestra estado actual.
- `/hordapve logs` -> muestra ruta de logs.
- `/hordapve setspawn` -> guarda el centro de la horda en tu posicion.
- `/hordapve enemy <tipo>` -> cambia categoria de enemigos.
- `/hordapve tipos` -> diagnostico de categorias y roles detectados.
- `/hordapve role <rolNpc|auto>` -> fuerza rol NPC o vuelve a auto.
- `/hordapve roles` -> lista roles NPC disponibles.
- `/hordapve reward <rondas>` -> cada cuantas rondas se entrega recompensa.
- `/hordapve spectator <on|off>` -> marca preferencia espectador/jugador para el bloqueo al inicio.
- `/hordapve player` -> vuelve a modo jugador.
- `/hordapve arearadius <bloques>` -> radio de captura de participantes de arena.
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

Nota: la disponibilidad real depende de los roles NPC de tu modpack. Usa `/hordapve tipos`.

## Flujo rapido

1. Ejecuta `/hordapve`.
2. Pulsa `Usar mi posicion actual` o ejecuta `/hordapve setspawn`.
3. Ajusta parametros por pestanas (`General`, `Horda`, `Jugadores`, `Sonidos`, `Recompensas`).
4. En `Jugadores`, define `arearadius` y el modo de cada jugador (jugador/espectador/salir).
5. Guarda con `Guardar config`.
6. Inicia con `/hordapve start`.
7. Deten con `/hordapve stop`.

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
