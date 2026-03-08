# HyHorde Arena PVE

Mod de hordas PVE por rondas para Hytale.

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

- `auto`
- `random`
- `bandit`
- `goblin`
- `skeleton`
- `zombie`
- `spider`
- `wolf`
- `slime`
- `beetle`
- `trork`
- `outlander`
- `scarak`

Nota: los roles reales dependen de tu modpack. Verificalo con `/hordapve tipos`.

## Recompensas

- La UI permite configurar `RewardItemId` y `RewardItemQuantity`.
- Si `RewardItemId` esta vacio o invalido, el sistema elige automaticamente un item de test validado para tu modpack.
- Si no encuentra ningun item valido, avisa en chat y en logs.

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
