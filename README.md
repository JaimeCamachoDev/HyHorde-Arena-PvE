# HyHorde Arena PVE

Mod de hordas PVE por rondas para Hytale.
Version actual: `1.0.4`.

## Comando de ayuda

- `/hordahelp` -> muestra ayuda de comandos en chat.

## Comandos disponibles

- `/hordapve` -> abre la UI de configuracion.
- `/hordepve` -> alias de `/hordapve`.
- `/hordapve start` -> inicia la horda.
- `/hordapve stop` -> detiene la horda y limpia enemigos spawneados por la sesion.
- `/hordapve status` -> muestra estado actual.
- `/hordapve logs` -> muestra la ruta de logs.
- `/hordapve setspawn` -> guarda el centro de horda en tu posicion.
- `/hordapve enemy <tipo>` -> cambia el tipo de enemigo.
- `/hordapve tipos` -> muestra diagnostico tipo -> rol detectado.
- `/hordapve role <rolNpc|auto>` -> fuerza un rol NPC o vuelve a auto.
- `/hordapve roles` -> lista roles NPC disponibles.
- `/hordapve reward <rondas>` -> configura cada cuantas rondas hay recompensa.
- `/hordapve spectator <on|off>` -> te marca como espectador/jugador para el siguiente inicio de horda.
- `/hordapve player` -> atajo para volver a modo jugador.
- `/hordapve arearadius <bloques>` -> radio de la arena para bloquear jugadores/espectadores al inicio.
- `/hordareload config` -> recarga `horde-config.json`.

## Tipos de enemigo soportados

- `random`
- `random-all`
- `undead`
- `goblins`
- `scarak`
- `void`
- `wild`
- `elementals`

Nota: los roles reales dependen de tu modpack. Verificalo con `/hordapve tipos`.

## Recompensas

- La UI permite configurar `RewardCategory`, `RewardItemId` y `RewardItemQuantity`.
- Usa `< >` en categoria e item para recorrer rapidamente el pool de recompensas.
- `random`: item aleatorio dentro de la categoria elegida.
- `random_all`: item aleatorio del pool completo.

## HUD de horda

- En `1.0.4` el HUD emergente automatico de horda queda desactivado temporalmente por estabilidad.

## Flujo rapido

1. Ejecuta `/hordapve`.
2. Pulsa `Usar mi posicion actual` (o usa `/hordapve setspawn`).
3. Configura por pestanas: `General`, `Jugadores`, `Recompensas`.
4. En `Jugadores`, ajusta `arearadius` y tu modo `jugador/espectador` para el bloqueo al inicio.
5. Configura rondas, enemigos, idioma y recompensas.
6. Guarda configuracion.
7. Inicia con `/hordapve start`.
8. Deten y limpia con `/hordapve stop`.

## Archivo de configuracion

- `horde-config.json` en la carpeta de datos del plugin.
- `arenaJoinRadius` define el radio alrededor del centro para bloquear participantes al iniciar (jugadores/espectadores).

## Build

```powershell
.\gradlew.bat clean jar
```

Genera:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`
