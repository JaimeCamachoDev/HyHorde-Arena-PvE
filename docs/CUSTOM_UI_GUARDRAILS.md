# Custom UI Guardrails (HyHorde Arena PVE)

Este documento recoge errores reales vistos en logs y las reglas para evitarlos al tocar `HordeConfigPage.ui`.

## Error actual (2026-03-29)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-29_12-08-22_client.log`):

- `Failed to load CustomUI documents`
- `Failed to parse file Pages/HordeConfigPage.ui (1024/1025:3)`
- `Unknown property Min on node of type NumberField`

Causa:

- Se uso `Min/Max/Step` sobre un `NumberField` (`$C.@NumberField #SoundsEditorVolumeInput`).
- En este runtime, `NumberField` no acepta esas propiedades directas. Esas props son de `Slider`/`SliderNumberField`.

Solucion aplicada:

- Quitar `Min/Max/Step` del `NumberField`.
- Validar rango en Java (clamp en `HordeConfigPage`/`HordeService`) al leer/guardar.

## Error actual (2026-03-29 - sonidos al abrir categoria)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-29_12-08-22_client.log`):

- `CustomUI Set command couldn't set value. Selector: #SoundsEditorVolumeInput.Value`
- `Failed to convert JSON value (String) to specified type (Decimal)`

Causa:

- Se estaba enviando `String` desde Java a `#SoundsEditorVolumeInput.Value`.
- Ese control es `NumberField` y en este runtime exige valor numerico JSON (no string).

Solucion aplicada:

- Cambiar `set("#SoundsEditorVolumeInput.Value", HordeConfigPage.formatDouble(volumePercent))`
  por `set("#SoundsEditorVolumeInput.Value", volumePercent)`.
- Mantener `draftValues` en string para persistencia interna, pero enviar al UI como `double`.

## Error actual (2026-03-29 - slider de volumen)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-29_12-08-22_client.log`):

- `CustomUI Set command couldn't set value. Selector: #SoundsEditorVolumeSlider.Value`
- `An element of type 'Number' cannot be converted to a 'System.Int32'`

Causa:

- Se estaba enviando `double` a `Slider.Value`.
- En este runtime, `Slider.Value` se valida como `Int32`.

Solucion aplicada:

- Redondear/castear y enviar entero:
  - `set("#SoundsEditorVolumeSlider.Value", (int)Math.round(volumePercent))`.
- Persistir el draft de volumen como string entero (`"0"`..`"100"`).

## Error adicional (2026-03-29)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-29_12-08-22_client.log`):

- `Failed to apply CustomUI event bindings`
- `Target element in CustomUI event binding has no compatible Activating event. Selector: #HordeCloseButton`

Causa:

- Se reemplazo `TextButton` por `$C.@BackButton` (template de `Common.ui`) y se hizo binding `Activating` sobre el id externo.
- Ese template no expuso el evento en ese selector concreto durante ese intento.

Solucion aplicada:

- Usar elemento nativo `BackButton #Id { ... }` (no template `$C.@BackButton`) en cada cierre.
- Mantener binding `CustomUIEventBindingType.Activating` sobre ese `#Id` (documentacion oficial de `BackButton` lo soporta).

## Error actual (2026-03-28)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-28_22-32-18_client.log`):

- `Failed to load CustomUI documents`
- `Failed to parse file Pages/HordeConfigPage.ui (2251:226)`
- `Could not resolve expression for property HorizontalAlignment to type LabelAlignment`

Causa:

- En `Label #ArenaHeaderCoords` se anadio `HorizontalAlignment: Center` dentro del estilo inline y este runtime fallo al resolver ese valor en ese punto del documento.

Solucion aplicada:

- Quitar `HorizontalAlignment` de `#ArenaHeaderCoords` y dejar solo `VerticalAlignment: Center`.
- Mantener alineaciones horizontales en headers criticos usando estilos ya probados, o validar en cliente tras cada cambio de enum/alineacion.

## Error recurrente (2026-03-29)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-19_10-17-18_client.log`, `2026-03-19_19-33-38_client.log`):

- `Crash - Selected element in CustomUI command was not found. Selector: #BossSelected.Value`
- `Crash - Selected element in CustomUI command was not found. Selector: #PlayerSelected.Value`

Causa:

- Desfase entre IDs/propiedades usados en `HordeConfigPage.java` y los IDs reales del `.ui` tras refactors de layout.

Solucion aplicada:

- Revisar/limpiar selectors legacy antes de compilar.
- Mantener este chequeo rapido de paridad Java/UI antes de publicar:
  - comparar `#Id` usados en Java vs `#Id` declarados en `HordeConfigPage.ui` para detectar selectors inexistentes.

## Error actual (2026-03-27)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-27_18-57-04_client.log`):

- `Failed to load CustomUI documents`
- `Failed to parse file Pages/HordeConfigPage.ui (49:12)`
- `Could not resolve spread expression to type TextButtonStyleState`

Causa:

- En `@ListRowHitboxButtonStyle` se intento heredar estados con spread:
  - `...$C.@SmallSecondaryTextButtonStyle.Default`
  - `...$C.@SmallSecondaryTextButtonStyle.Hovered`
  - `...$C.@SmallSecondaryTextButtonStyle.Pressed`
- En este runtime, ese spread no se resuelve para `TextButtonStyleState`.

Solucion aplicada:

- Definir `Default`, `Hovered`, `Pressed` de forma explicita sin spread en ese bloque.

## Error previo de Custom UI (2026-03-27)

Firma en cliente:

- `Failed to load CustomUI documents`
- `Failed to parse file Pages/HordeConfigPage.ui (22:160)`
- `Could not resolve expression for property VerticalAlignment to type LabelAlignment`

Causa:

- Se uso `VerticalAlignment: Top` en `LabelStyle` de pestanas.

Solucion:

- Volver a `VerticalAlignment: Center`.
- Si hay que subir texto, usar `Padding` en vez de `VerticalAlignment: Top`.

## Errores frecuentes ya vistos

1. Documento no encontrado
- Firma: `Could not find document ... HordeConfigPage.ui for Custom UI Append command`.
- Causa: path incorrecto del layout.
- Regla: mantener `LAYOUT = "Pages/HordeConfigPage.ui"` en Java y archivo real en `Common/UI/Custom/Pages/HordeConfigPage.ui`.

2. Set command sobre selector no compatible
- Firma: `CustomUI Set command couldn't set value. Selector: #MinRadius.Value` (y similares).
- Causa: setear desde Java `.Value` de ciertos controles durante build/rebuild.
- Regla: no hacer `set` de esos `.Value` en `build()`. Solo leer payload al guardar/iniciar.

3. Selector inexistente
- Firma: `CustomUI Set command selector doesn't match a markup property`.
- Causa: selector cambiado en `.ui` sin sincronizar Java.
- Regla: cuando se renombre un `#Id` en UI, actualizar todos los selectors en `HordeConfigPage.java`.

3.b. Template sin propiedad compatible
- Firma: `Unknown property <X> on node of type <Y>`.
- Causa: asumir propiedades por intuicion (ej. `Min` en `NumberField`) sin validar el tipo exacto.
- Regla: verificar propiedades en Type Documentation antes de usar un template o elemento nuevo.

3.d. Tipo JSON no compatible con la propiedad
- Firma: `Failed to convert JSON value (String) to specified type (Decimal)`.
- Causa: enviar texto a propiedades numericas (`NumberField.Value`).
- Regla: al hacer `set` sobre propiedades numericas, enviar `int/double`, no `String`.

3.c. Elemento sin evento compatible
- Firma: `Target element in CustomUI event binding has no compatible Activating event`.
- Causa: binding `Activating` sobre tipo de elemento que no lo expone (ej. `BackButton`).
- Regla: comprobar compatibilidad de eventos por elemento antes de cambiar tipo (`TextButton` -> `BackButton`).

4. Texturas de tabs con cruces rojas
- Causa: rutas de textura incorrectas.
- Regla: usar rutas relativas `../Common/...` para recursos compartidos de tabs.

5. Listas `TopScrolling` con filas desalineadas
- Firma visual: mas padding a derecha que a izquierda, filas "finas/largas", boton `X` deformado.
- Causa: filas append con `Width` fijo dentro de un contenedor con scrollbar/padding.
- Regla: en filas dinamicas (`Pages/HordeArenaRow.ui`), usar `Anchor: (Left: 0, Right: 0, ...)` y definir tamanos de icono/boton en px cuadrados.

6. Nuevo campo en editor no persiste
- Firma: el valor aparece en UI pero se pierde al guardar/reabrir.
- Causa: agregar `#Campo.Value` en `.ui` sin sincronizar Java/catalogo.
- Regla: para cada campo nuevo de editor, sincronizar siempre:
- `SNAPSHOT_FIELDS` + `shouldCaptureFieldFromPayload(...)`
- `build().set(...)` + `extract...ValuesForSave()`
- `ensure...DraftDefaults(...)` + `apply...DraftFromSnapshot(...)`
- `...Definition`/`...Snapshot` del catalogo y guardado JSON

## No confundir con errores de servidor no-CustomUI

En `server.log` de hoy tambien aparece:

- `Failed to load manifest for pack at mods\VZ.HytaleMod_Test`
- `Failed to decode`
- `Unexpected character: 22, '"' expected '{'!`

Y tambien:

- `Skipping pack at <pack>: missing or invalid manifest.json`

Esto es un problema de `manifest.json` de otro pack/mod, no del parser de Custom UI.

## Reglas de referencia (docs oficiales + repo guia)

Fuentes revisadas:

- https://hytalemodding.dev/en/docs/official-documentation/custom-ui
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/common-styling
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/layout
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/markup
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/tabnavigation
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/tabbutton
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/slider
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/floatslider
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/numberfield
- https://hytalemodding.dev/en/docs/official-documentation/npc/1-know-your-enemy
- https://hytalemodding.dev/en/docs/official-documentation/npc/2-getting-started-with-templates
- https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation/elements/backbutton
- https://github.com/ScarForges/Base-Mods-Hytale-CustomUI

Aplicado en este mod:

1. Cierres de modal con componente oficial
- Regla: usar elemento `BackButton #Id { ... }` para cerrar ventanas en vez de botones rojos custom.
- Regla: bindear `Activating` contra ese `#Id` del `BackButton` (segun type docs).
- Nota: `BackButton` no usa `Text` como `TextButton`; evitar `set("#...Text")`.

2. Props segun tipo
- Regla: no mezclar propiedades de `Slider`/`SliderNumberField` con `NumberField`.
- Regla: para rangos en `NumberField`, validar en Java y/o usar formato soportado por tipo.

3. Estilo oficial de texto
- Regla: priorizar tokens de color de `Common.ui` (`$C.@ColorDefault`, `$C.@ColorDefaultLabel`) sobre colores hardcode cuando se quiera look oficial.
- Regla: para titulos destacados, usar `MaskTexturePath: $C.@TextHighlightGradientMask` donde sea estable.

4. Scrolls estables
- Regla: contenedor con `Height` fijo + `LayoutMode: TopScrolling` + `ScrollbarStyle`.

5. Flujo de eventos
- Regla: para `DropdownBox`, usar `ValueChanged` (no `Activating`) para evitar crashes de bindings.

6. Tabs nativas (`TabNavigation`)
- Regla: usar un unico sistema de tabs activo (nativo o custom), no mezclar comportamiento de `TabNavigation` con overlays legacy de seleccion.
- Regla: `TabButton` usa `Activating`; el tab activo debe sincronizarse por `#CategoryTabs.SelectedTab = <id>`.
- Regla: mantener los `Id` de `TabButton` estables (`general`, `players`, `arenas`, etc.) para evitar desajustes de navegacion.

7. Volumen/sistemas numericos de Sonidos
- Regla: para control de volumen en UI, preferir `Slider`/`FloatSlider` (mas usable) y no `NumberField` suelto.
- Regla: `Slider`/`FloatSlider` soportan `Min`, `Max`, `Step`, `Value` y callback `ValueChanged` (segun Type Documentation).
- Regla: `NumberField` no debe recibir `Min/Max/Step` (ya causo crash de parser en este proyecto).
- Regla: al hacer `set` desde Java sobre propiedades numericas, enviar tipos numericos JSON (`int/double`), nunca string.

8. Referencia NPC para pickers de enemigos
- Regla: para listas de enemigos y validacion semantica, usar los docs NPC oficiales como referencia funcional (roles/templates), sin depender de fuentes no oficiales para IDs criticos.

## Checklist rapido antes de compilar

1. Revisar que no hay `VerticalAlignment: Top` en estilos de label de tabs.
2. Revisar que no hay spreads de estados no compatibles en `TextButtonStyle` (evitar `...Style.Default` dentro de `Default/Hovered/Pressed` si falla parseo).
3. Revisar que selectors Java (`#...`) existen en el `.ui`.
4. Revisar que no se setean `.Value` problematicos en `build()`.
5. Compilar y probar apertura de `/hordeconfig` una vez.
6. Si hay crash de carga, mirar primero:
- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\*_client.log` (errores de parser Custom UI).
- `C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Saves\Mod-Test\logs\*_server.log` (errores de plugin/manifest/arranque).
7. Si se toca Sonidos, verificar:
- abrir tab Sonidos sin crash.
- abrir editor de sonido.
- mover slider de volumen y guardar.
- reabrir editor y confirmar persistencia del valor.
