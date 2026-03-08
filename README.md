# HyHorde Arena PVE

Mod de Hytale centrado en un sistema de hordas PVE configurable, con interfaz dentro del juego, panel de estado en vivo y recompensas por rondas.

## Caracteristicas

- Sistema de rondas con escalado de enemigos.
- Spawn radial alrededor de un centro configurable.
- Seleccion de enemigos simplificada a una lista de 10 tipos comunes (o modo `auto`).
- Configuracion desde UI (`HordeConfigPage`) y estado en vivo (`HordeStatusPage`).
- Recompensas por rondas mediante comandos configurables.
- Recarga de configuracion en caliente (`/hordareload config`).

## Estructura principal

- Plugin principal: `src/main/java/com/hyhorde/arenapve/HyHordeArenaPvePlugin.java`
- Servicio de hordas: `src/main/java/com/hyhorde/arenapve/horde/HordeService.java`
- Comandos: `src/main/java/com/hyhorde/arenapve/commands/`
- UI: `src/main/resources/Common/UI/Custom/Pages/`
- Manifest estatico: `src/main/resources/manifest.json`
- Manifest para build: `src/main/resources/manifest.template.json`
- Parametros del mod: `gradle.properties`

## Comandos del mod

- `/hordapve` abre la UI de configuracion.
- `/hordapve start|stop|status`
- `/hordapve hud` abre panel de estado.
- `/hordapve setspawn` guarda tu posicion como centro.
- `/hordapve enemy <tipo>` fija tipo de enemigo (`auto`, `bandit`, `goblin`, `skeleton`, `zombie`, `spider`, `wolf`, `wraith`, `void`, `demon`, `beast`).
- `/hordapve tipos` muestra la lista disponible.
- `/hordapve reward <rondas>` define frecuencia de recompensas.
- `/hordareload config` recarga `horde-config.json`.
- `/hordareload mod` intenta recargar el plugin completo.
- `/horda` genera una horda rapida alrededor del jugador.

La configuracion de recompensas (frecuencia y comandos) se puede editar directamente en la ventana de `/hordapve`.

## Configuracion

`HordeService` guarda su configuracion en `horde-config.json` dentro del directorio de datos del plugin. Valores clave:

- `rounds`
- `baseEnemiesPerRound`
- `enemiesPerRoundIncrement`
- `waveDelaySeconds`
- `minSpawnRadius` / `maxSpawnRadius`
- `enemyType`
- `rewardEveryRounds`
- `rewardCommands`

## Build

1. Configura `gradle.properties`:
   - `version`
   - `maven_group`
   - `mod_group`
   - `mod_name`
   - `mod_description`
   - `mod_author`
   - `mod_website`
   - `mod_main_class`
2. Asegura acceso a `HytaleServer.jar` con `hytale_home` o `hytale_server_jar`.
3. Compila:

```powershell
.\gradlew.bat clean jar
```

Salida esperada:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`

## Despliegue local

```powershell
.\gradlew.bat deployToLocalMods
```

El task copia el JAR a `%APPDATA%\Hytale\UserData\Mods`.

## Notas

- `manifest.template.json` se usa para generar el `manifest.json` final durante el build.
- `src/main/resources/manifest.json` es valido para carga como carpeta/mod descomprimido.
