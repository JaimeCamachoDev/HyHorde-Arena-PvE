# HyHorde Arena PVE

Sistema de hordas PVE para Hytale con configuracion en UI, lista simple de enemigos y recompensas en forma de items.

## Que hace este mod

- Crea hordas por rondas con dificultad creciente.
- Spawnea enemigos alrededor de un centro configurable.
- Permite elegir tipo de enemigo desde lista simplificada:
  `auto, random, bandit, goblin, skeleton, zombie, spider, wolf, wraith, void, demon, beast`
- Da recompensas como item dropeado en el centro de la horda.
- Escala cantidad de enemigos por ronda con un multiplicador de jugadores.
- Muestra anuncios grandes en pantalla al iniciar y al terminar la horda.
- Incluye panel de estado en vivo opcional (`/hordapve hud`).

## Guia rapida para usuario nuevo

1. Ejecuta `/hordahelp` para abrir la ayuda rapida en ventana.
2. Ejecuta `/hordapve` para abrir el menu principal.
3. Define el centro de la horda:
   - Boton `Usar mi posicion actual`, o
   - comando `/hordapve setspawn`.
4. Ajusta parametros basicos en la UI:
   - `Rounds`, `BaseEnemies`, `Incremento por ronda`, `Delay entre rondas`
   - `Jugadores (x)` para escalar la horda segun cuantas personas jugaran
   - `Tipo enemigo` (con botones `<` y `>` para cambiar rapido)
   - `Recompensa cada`, `Item recompensa`, `Cantidad`
5. Pulsa `Guardar config`.
6. Inicia con `Iniciar horda` o `/hordapve start`.
7. Para consultar estado:
   - `/hordapve status` (chat)
   - `/hordapve hud` (ventana de estado)
8. Para detenerla: `/hordapve stop`.

## Comandos

- `/hordahelp` abre la ventana de ayuda (o `/hordahelp chat` para version texto).
- `/hordapve` abre la UI de configuracion.
- `/hordapve help` abre la ayuda.
- `/hordapve start|stop|status`
- `/hordapve logs` muestra la ruta de logs del save/mod.
- `/hordapve hud` abre el panel de estado.
- `/hordapve setspawn` guarda tu posicion como centro.
- `/hordapve enemy <tipo>` fija tipo de enemigo.
- `/hordapve tipos` lista tipos disponibles.
- `/hordapve reward <rondas>` cambia frecuencia de recompensa.
- `/hordareload config` recarga `horde-config.json`.
- `/hordareload mod` intenta recargar el plugin completo.
- `/horda` comando rapido de pruebas para spawnear una horda alrededor del jugador.

Ruta tipica de logs en Windows:

- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs`

## Recompensas (simple)

- Las recompensas son items, no comandos.
- Se configuran desde la UI de `/hordapve`:
  - `RewardEveryRounds`
  - `RewardItemId`
  - `RewardItemQuantity`
- Boton `Ver rewards` en la UI muestra sugerencias rapidas de IDs de item.
- Cuando se cumple la ronda objetivo, el item se dropea en el centro de la horda.

## Anuncios en pantalla

- Inicio de horda: anuncio grande (title) para jugadores.
- Fin de horda: anuncio grande (title) para jugadores.
- Adicionalmente se mantiene mensaje de chat con detalle.

## Configuracion en disco

El mod guarda config en `horde-config.json` dentro de la carpeta de datos del plugin.

Campos principales:

- `spawnConfigured`, `worldName`, `spawnX`, `spawnY`, `spawnZ`
- `minSpawnRadius`, `maxSpawnRadius`
- `rounds`, `baseEnemiesPerRound`, `enemiesPerRoundIncrement`, `waveDelaySeconds`
- `playerMultiplier`
- `enemyType`
- `rewardEveryRounds`, `rewardItemId`, `rewardItemQuantity`

## Build

1. Revisa `gradle.properties` (`version`, `mod_group`, `mod_name`, etc.).
2. Configura acceso a `HytaleServer.jar` con `hytale_home` o `hytale_server_jar`.
3. Compila:

```powershell
.\gradlew.bat clean jar
```

Jar generado:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`

## Deploy local

```powershell
.\gradlew.bat deployToLocalMods
```

Copia el jar a `%APPDATA%\Hytale\UserData\Mods`.

## Estructura del proyecto

- Plugin: `src/main/java/com/hyhorde/arenapve/HyHordeArenaPvePlugin.java`
- Servicio principal: `src/main/java/com/hyhorde/arenapve/horde/HordeService.java`
- Comandos: `src/main/java/com/hyhorde/arenapve/commands/`
- UI: `src/main/resources/Common/UI/Custom/Pages/`
- Manifest build-time: `src/main/resources/manifest.template.json`
- Manifest estatico: `src/main/resources/manifest.json`
