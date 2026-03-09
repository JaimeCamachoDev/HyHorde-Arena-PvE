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
3. Configura rondas, enemigos, idioma y recompensas.
4. Guarda configuracion.
5. Inicia con `/hordapve start`.
6. Deten y limpia con `/hordapve stop`.

## Archivo de configuracion

- `horde-config.json` en la carpeta de datos del plugin.

## Build

```powershell
.\gradlew.bat clean jar
```

Genera:

- `build/libs/HyHorde-Arena-PVE-<version>.jar`
