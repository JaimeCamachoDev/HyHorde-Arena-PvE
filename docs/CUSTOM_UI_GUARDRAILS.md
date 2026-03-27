# Custom UI Guardrails (HyHorde Arena PVE)

Este documento recoge errores reales vistos en logs y las reglas para evitarlos al tocar `HordeConfigPage.ui`.

## Error de hoy (2026-03-27)

Firma en cliente:

- `Failed to load CustomUI documents`
- `Failed to parse file Pages/HordeConfigPage.ui (22:160)`
- `Could not resolve expression for property VerticalAlignment to type LabelAlignment`

Causa:

- Se uso `VerticalAlignment: Top` en `LabelStyle` de pestañas.
- En este runtime de Hytale, ese valor no se resuelve para `LabelAlignment` en ese contexto.

Solucion aplicada:

- Volver a `VerticalAlignment: Center`.
- Para subir el texto visualmente, usar `Padding` (no `VerticalAlignment: Top`).

## Errores Custom UI frecuentes ya vistos

1. Documento no encontrado
- Firma: `Could not find document ... HordeConfigPage.ui for Custom UI Append command`.
- Causa: path incorrecto del layout.
- Regla: mantener `LAYOUT = "Pages/HordeConfigPage.ui"` en Java y archivo real en `Common/UI/Custom/Pages/HordeConfigPage.ui`.

2. Set command sobre selector no compatible
- Firma: `CustomUI Set command couldn't set value. Selector: #MinRadius.Value` (y similares).
- Causa: setear desde Java `.Value` de ciertos controles (especialmente sliders/slider-number fields) durante build/rebuild.
- Regla: no hacer `set` de esos `.Value` en `build()`. Solo leer payload al guardar/iniciar.

3. Selector inexistente
- Firma: `CustomUI Set command selector doesn't match a markup property`.
- Causa: selector cambiado en `.ui` sin sincronizar Java.
- Regla: cuando se renombre un `#Id` en UI, actualizar todos los selectors en `HordeConfigPage.java`.

4. Texturas de tabs con cruces rojas
- Causa: rutas de textura incorrectas.
- Regla: usar rutas relativas `../Common/...` para recursos compartidos de tabs.

## Checklist rapido antes de compilar

1. Revisar que no hay `VerticalAlignment: Top` en estilos de label de tabs.
2. Revisar que selectors Java (`#...`) existen en el `.ui`.
3. Revisar que no se setean `.Value` problemáticos en `build()`.
4. Compilar y probar apertura de `/hordeconfig` una vez.
5. Si hay crash de carga, mirar primero:
   - `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\*_client.log`
   - `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs\*_server.log`

