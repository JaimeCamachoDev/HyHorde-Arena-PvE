# Custom UI Guardrails (HyHorde Arena PVE)

Este documento recoge errores reales vistos en logs y las reglas para evitarlos al tocar `HordeConfigPage.ui`.

## Error actual (2026-04-08 - picker de enemigos NPC, cruces rojas + refresco lento)

Firma en cliente:

- iconos NPC con cruz roja (missing texture)
- refresco por lotes al navegar (no carga inmediata de todas las caras)

Causa:

- Se mezclaron dos enfoques:
  - `AssetImage.AssetPath` con ruta incorrecta (`Common/UI/...`).
  - carga dinamica de muchos assets NPC (stream + rebuild), que en listas grandes provoca refresco por lotes.

Solucion aplicada:

- Mantener `AssetImage` en las celdas del picker (`#EnemyPickFace1..4`).
- Usar rutas estaticas del mod en `AssetPath` con formato:
  - `UI/Custom/Icons/Npcs/<Nombre>.png`
- No usar prefijo `Common/` en `AssetPath`.
- Filtrar opciones del picker para mostrar solo enemigos con icono NPC resoluble.

Regla:

- Para iconos NPC de pickers masivos, priorizar assets estaticos empaquetados en el mod.
- Reservar streaming de assets para casos puntuales (ej. caras de jugador), no para cientos de NPCs.

## Error actual (2026-04-08 - crash al seleccionar enemigo en picker)

Firma en cliente:

- `CustomUI Set command couldn't set value. Selector: #EnemyCatEnemyPickerGrid[0] #EnemyPickFace1.Visible`

Causa:

- En esta build, usar `Sprite` para `#EnemyPickFaceX` en filas append dinamicas del picker causaba fallo al aplicar comandos de UI.

Solucion aplicada:

- Revertir `#EnemyPickFaceX` a `AssetImage`.
- Actualizar desde Java con `AssetPath` (no `TexturePath`).

Regla:

- En listas append del picker de enemigos, usar `AssetImage` para miniaturas NPC.
- Evitar `Sprite` en ese punto concreto aunque compile.

## Error actual (2026-04-08 - buscador de Enemy ID se borra al escribir)

Firma:

- al teclear en `#EnemyCatEnemyPickerSearch #SearchInput` / `#BossEnemyPickerSearch #SearchInput`, el texto vuelve atras (parece que solo deja 1 letra o vacia el campo).

Causa:

- El evento `ValueChanged` disparaba rebuild, pero `shouldCaptureFieldFromPayload(...)` no permitia capturar:
  - `enemycatEnemyPickerSearch`
  - `bossEnemyPickerSearch`

Solucion aplicada:

- Incluir ambas claves en captura para `TAB_ENEMIES` y `TAB_BOSSES`.

Regla:

- Todo `SearchInput` con `ValueChanged` debe tener:
  - `UiFieldBinding` definido.
  - clave permitida en `shouldCaptureFieldFromPayload(...)` para su tab.
  - reconstruccion que lea desde `draftValues`.

## Error actual (2026-04-08 - buscador de Enemy ID pierde foco en cada letra)

Firma:

- En `#EnemyCatEnemyPickerSearch #SearchInput` y `#BossEnemyPickerSearch #SearchInput`, al escribir una letra el foco se pierde y obliga a hacer click de nuevo.

Causa:

- El binding en `ValueChanged` disparaba rebuild completo de la UI en cada tecla.

Solucion aplicada:

- Cambiar el binding de buscador de enemigos/bosses a:
  - `Validating`
  - `FocusLost`
- Mantener `draftValues` + lectura de payload para que el filtro siga funcionando al confirmar.

Regla:

- Para `TextInput` con listas pesadas (picker de enemigos con miniaturas), evitar `ValueChanged` si causa rebuild/focus thrash.
- Usar `Validating` + `FocusLost` para permitir escritura fluida y refrescar al confirmar.

## Error actual (2026-04-09 - buscador de Enemy ID casi no deja hacer click/escribir)

Firma:

- En la ventana `Selecciona Enemy IDs`, el `SearchInput` responde mal al click.
- Hay que insistir varias veces para enfocar el campo o empezar a escribir.
- Sensacion de refresco continuo de fondo que bloquea la interaccion.

Causa:

- El binding `FocusLost` del `SearchInput` disparaba `enemycat_enemy_search_change` / `boss_enemy_search_change`.
- Ese evento terminaba en rebuild de la UI, lo que podia volver a provocar perdida de foco y otro ciclo.
- Resultado: thrash de foco/interaccion (el campo parece “pelearse” con el refresco).

Solucion final aplicada:

- Quitar `FocusLost` en:
  - `#EnemyCatEnemyPickerSearch #SearchInput`
  - `#BossEnemyPickerSearch #SearchInput`
- Dejar solo `Validating` para aplicar filtro al confirmar (Enter).

Estado validado:

- El buscador vuelve a ser usable: click estable + escritura fluida.
- Se evita el bucle de refresco por perdida de foco.

Regla:

- En buscadores de pickers pesados, usar `Validating` como trigger principal.
- No usar `FocusLost` si el handler provoca rebuild completo de pantalla.

## Error actual (2026-04-09 - crash tras tocar ancho de DropdownBox)

Firma:

- Tras el cambio de ancho en dropdowns de editor (`#HordeMode`, `#BossEditTier`), el cliente se cae al entrar (`Crash - Crash`) y el servidor termina con `Exception when adding player to world` / `CancellationException`.

Causa:

- Se forzó `Anchor` dentro de `DropdownBoxStyle(...)`:
  - `Style: DropdownBoxStyle(..., Anchor: (Width: 960, Height: 34), PanelWidth: 960)`
- Ese patrón no es estable en este runtime para estos controles.

Solucion aplicada:

- Quitar `Anchor` del `Style`.
- Mantener:
  - `@Anchor = (Left: ..., Top: ..., Width: 960)` en el control.
  - `Style: DropdownBoxStyle(..., PanelWidth: 960)` para el panel desplegable.

Regla:

- No definir `Anchor` dentro de `DropdownBoxStyle`.
- Definir geometría del botón en `@Anchor` del `DropdownBox` y usar `PanelWidth` solo para ancho del desplegable.
- Si se cambia layout de dropdown, validar en juego los dos estados:
  - botón cerrado (ancho correcto)
  - panel abierto (ancho correcto)

## Error actual (2026-04-09 - panel ancho correcto pero boton del dropdown corto)

Firma visual:

- En `Tier`/`Modo de horda`, el panel desplegado tiene el ancho correcto.
- El botón que abre el dropdown queda más corto que el resto de campos.

Causa:

- Dependencia del template `$C.@DropdownBox` (de `Common.ui`) para el botón renderizado.
- Si el template/base cambia o se comporta distinto, puede respetar `PanelWidth` en el panel pero no el ancho visual del botón.

Solucion aplicada (estable):

- Para dropdowns críticos de editor, usar `DropdownBox` nativo en vez de `$C.@DropdownBox`.
- Definir `Anchor` directo en el control:
  - `Anchor: (Left: ..., Top: ..., Width: 960, Height: 34)`
- Mantener estilo:
  - `Style: DropdownBoxStyle(...@OfficialDropdownBoxStyle, PanelWidth: 960)`

Patron recomendado:

- `DropdownBox #ControlId { Anchor: (..., Width: X, Height: 34); Style: DropdownBoxStyle(..., PanelWidth: X); }`
- Evitar mezclar:
  - `Anchor` dentro de `DropdownBoxStyle`
  - dependencia innecesaria de template (`$C.@DropdownBox`) en campos donde el ancho del botón debe ser exacto.

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

## Error actual (2026-03-29 - apertura del mod con `hordeconfig`)

Firma en cliente (`C:\Users\Jaime\AppData\Roaming\Hytale\UserData\Logs\2026-03-29_12-08-22_client.log`):

- `CustomUI Set command couldn't set value. Selector: #ArenaJoinRadius.Value`
- `Failed to convert JSON value (String) to specified type (Int32)`

Causa:

- `#ArenaJoinRadius` es `$C.@SliderNumberField` y en esta build valida `Value` como `Int32`.
- Se estaba enviando string desde Java (`"32.00"`) en `build()`.

Solucion aplicada:

- Enviar entero en el `set`:
  - `set("#ArenaJoinRadius.Value", arenaJoinRadiusUiValue)`
- `arenaJoinRadiusUiValue` se calcula con `getDraftInt(...)` + `clamp(4..512)`.

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
- Regla: en `UIPath`, las rutas son relativas al `.ui` donde se declaran.
- Regla: para `Pages/HordeConfigPage.ui`, usar:
  - recursos de `Common/UI/Custom/Common/*`: `../Common/...`
  - iconos para `TabButton.Icon`: `../Icons/...` dentro del propio asset pack del mod.
- Error real visto (2026-03-29 16:59:40):
  - `Failed to parse file Pages/HordeConfigPage.ui (54:13) - Could not resolve relative path: ../../../Icons/ItemCategories/Items.png`
- Regla: no apuntar `TabButton.Icon` a `../../../Icons/...` ni a `Common/Icons/...` desde `Pages/HordeConfigPage.ui`.
- Solucion estable:
  - copiar los iconos necesarios a `src/main/resources/Common/UI/Custom/Icons/...`
  - referenciar con `../Icons/...` desde `Pages/HordeConfigPage.ui`.

4.b Iconos de estilo inconsistente (tabs mas oscuros)
- Causa: mezclar iconos 88x88 (`Items.png`, `Natural.png`) con el set de categorias 48x48.
- Firma visual: algunos tabs se ven mas oscuros o con distinto estilo.
- Regla: para tabs/top-tabs/header-tabs usar solo iconos 48x48 del set `ItemCategories` (ej: `Builder Tools.png`, `Build-Roofs.png`, `Items-Armor.png`, `Natural-Fire.png`).

4.c `Trash@2x` mostrando cruz roja en listas
- Causa real: en esta build, la ruta con `@` en `TexturePath` provoca fallback de textura faltante en algunos contextos de Custom UI.
- Firma visual: boton de borrar muestra una `X` roja sobre fondo blanco (placeholder de textura missing).
- Regla: usar alias local sin `@` (`../Icons/AssetNotifications/Trash.png`) para `@TrashDeleteButtonStyle`.

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

Tambien visto (2026-03-29):

- `HyHorde:ArenaPVE: java.lang.StackOverflowError` al abrir el mod.
- Traza repetida en:
  - `HordeService.resolveDefaultRewardCategoryIconItemId(...)`
  - `HordeService.normalizeRewardCategoryIconItemId(...)`

Causa:

- Fallback recursivo circular de iconos de recompensa (si icono invalido -> default -> vuelve a normalize -> default ...).

Regla:

- Evitar fallback circular entre helpers `resolveDefault...` y `normalize...`.
- `resolveDefault...` debe devolver un valor final validado (sin llamar a `normalize...` del mismo dominio).

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

9. Eventos de alta frecuencia (sliders)
- Regla: en `ValueChanged` de `Slider`/`SliderNumberField`, evitar handlers pesados como `applyUiConfig(...)`.
- Regla: usar un setter ligero y especifico (ej. `setArenaJoinRadius(...)`) para no bloquear `GamePacketHandler` ni dejar la UI en loading.
- Regla: tras procesar el evento, enviar una actualizacion UI minima para cerrar correctamente el estado de carga del cliente.

## Validacion build-11 (2026-03-29)

Comprobado contra:
- `C:\Users\Jaime\AppData\Roaming\Hytale\install\release\package\game\build-11\Assets.zip`
- `Common/UI/Custom/Common.ui`

Resultado:
- Estan definidos y disponibles: `@TopTabsStyle`, `@TopTabStyle`, `@HeaderTabsStyle`, `@HeaderTabStyle`, `@HeaderSearch`.
- En esta build no hay evidencia de crash por usar esos estilos nativos de tabs/search; los crashes vistos fueron por:
  - propiedades incompatibles por tipo (ej. `Min` en `NumberField`)
  - selectores/event bindings invalidos (elemento no encontrado o sin evento compatible).

Decision:
- Priorizar patron nativo oficial para navegacion y pickers:
  - tabs principales: `Style: $C.@TopTabsStyle` con `TabButton.Icon` + `TooltipText`.
  - tabs de cabecera en pickers: `Style: $C.@HeaderTabsStyle`.
  - buscador de cabecera en pickers: `$C.@HeaderSearch #<Id> { ... }` y leer/escribir `#<Id> #SearchInput.Value`.

Inventario de iconos (misma build):
- `Common/Icons/*`: `3746` iconos.
- Carpetas mas utiles para este mod:
  - `Common/Icons/ItemCategories/*`
  - `Common/Icons/CraftingCategories/*`
  - `Common/Icons/ItemsGenerated/*`

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
