# HyHorde Arena PVE

Mod de Hytale para crear hordas PVE por rondas con configuracion en UI.

## Que incluye

- Sistema de rondas con dificultad progresiva.
- Centro de horda configurable (spawn de enemigos alrededor del centro).
- Seleccion de tipo de enemigo simplificada (`auto`, `random` + 10 tipos comunes).
- Recompensas como `item` dropeado en el centro de la horda.
- Escalado por jugadores (`playerMultiplier`).
- Anuncio grande en pantalla al inicio y al final de la horda.
- Cuenta atras global `3..2..1` antes de empezar la primera ronda.
- Anuncio grande al completar cada ronda con datos clave (siguiente ronda, kills, spawn, recompensa).
- Panel de estado opcional (`/hordapve hud`) con:
  - Contador `Quedan X enemigos`.
  - Tabla de jugadores con kills/deaths durante la horda.
  - Resumen personal (tus kills/deaths).
- Selector de idioma `Espanol/English` con botones `< >` en la UI de configuracion.
- Validacion reforzada de `rewardItemId` para evitar drops de items corruptos/Unknown.

## Guia rapida (paso a paso)

1. Ejecuta `/hordahelp` para ver ayuda rapida.
2. Ejecuta `/hordapve` para abrir **Horda PVE Config**.
3. Define el centro con `Usar mi posicion actual` o `/hordapve setspawn`.
4. Ajusta lo basico:
   - `Rondas`, `Base ronda`, `Inc. por ronda`, `Delay (s)`, `Jugadores (x)`.
   - `Tipo enemigo` con botones `< >` (la UI prioriza tipos compatibles detectados).
   - `Idioma` con botones `< >` (`Espanol` / `English`).
   - `Recompensa cada`, `Item recompensa` (tambien con `< >`) y `Cant.`.
5. Pulsa `Guardar config`.
6. Inicia con `Iniciar horda` o `/hordapve start`.
7. Consulta estado con `/hordapve status` o `/hordapve hud`.
8. Deten con `/hordapve stop`.

## Comandos reales

- `/horda help` muestra ayuda de comandos en chat.
- `/horda` comando rapido de pruebas para spawnear una horda simple alrededor del jugador.
- `/hordahelp` abre ayuda en UI.
- `/hordahelp chat` envia ayuda en chat.
- `/hordapve` abre UI de configuracion.
- `/hordepve` alias de `/hordapve` (mismos subcomandos).
- `/hordapve help` abre ayuda.
- `/hordapve start`
- `/hordapve stop`
- `/hordapve status`
- `/hordapve logs` muestra ruta de logs.
- `/hordapve hud` abre panel de estado.
- `/hordapve setspawn`
- `/hordapve enemy <tipo>` cambia tipo simplificado.
- `/hordapve tipos` muestra tipos y su mapeo real a roles NPC detectados.
- `/hordapve role <rolNpc|auto>` fuerza rol NPC exacto (modo avanzado).
- `/hordapve roles` lista roles NPC disponibles.
- `/hordapve reward <rondas>` cambia frecuencia de recompensa.
- `/hordareload config` recarga `horde-config.json`.
- `/hordareload mod` intenta recargar el plugin completo.

No existen comandos `/cerrar` ni `/holi`.

## Tipos de enemigo simplificados

- `auto`
- `random`
- `bandit`
- `goblin`
- `skeleton`
- `zombie`
- `spider`
- `wolf`
- `wraith`
- `void`
- `demon`
- `beast`

Importante: algunos tipos pueden no existir en todos los modpacks.  
Usa `/hordapve tipos` para verificar en tu instalacion que rol real se detecta para cada tipo.

Nota: los roles tipo gato/mascota se filtran para que no salgan como enemigos de horda.

## Recompensas

- Son items (no comandos).
- Se configuran con:
  - `rewardEveryRounds`
  - `rewardItemId`
  - `rewardItemQuantity`
- El plugin normaliza y valida el ID antes de dropear para evitar `Unknown/corrupt`.
- Al completar la ronda objetivo, el item valido se dropea en el centro de horda.

## Logs

Ruta esperada en Windows (save `Mod-Test`):

- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs`

Tambien puedes usar `/hordapve logs` para ver la ruta detectada.

## Configuracion en disco

Archivo: `horde-config.json` en la carpeta de datos del plugin.

Campos principales:

- `spawnConfigured`, `worldName`, `spawnX`, `spawnY`, `spawnZ`
- `minSpawnRadius`, `maxSpawnRadius`
- `rounds`, `baseEnemiesPerRound`, `enemiesPerRoundIncrement`, `waveDelaySeconds`
- `playerMultiplier`
- `enemyType`
- `npcRole` (override opcional)
- `rewardEveryRounds`, `rewardItemId`, `rewardItemQuantity`

## Build

```powershell
.\gradlew.bat clean jar
```

Jar esperado:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`
