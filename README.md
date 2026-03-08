# Hytale Mod Template

Template base para crear mods de Hytale en Java con un comando unico: `/hola`.

## Estructura

- Entrada del plugin: `src/main/java/com/example/hytale/template/HyTemplatePlugin.java`
- Comando ejemplo: `src/main/java/com/example/hytale/template/HolaCommand.java`
- Manifest para carga desde carpeta: `src/main/resources/manifest.json`
- Manifest template para build: `src/main/resources/manifest.template.json`
- Parametros iniciales: `gradle.properties`

## Paso 1: Crear tu repo en GitHub

1. En GitHub, pulsa `Use this template` sobre este repositorio.
2. Crea tu nuevo repo (publico o privado).
3. Clona tu repo nuevo:

```powershell
git clone https://github.com/<tu-usuario>/<tu-repo>.git
cd <tu-repo>
```

## Paso 2: Configurar parametros iniciales limpios

Edita `gradle.properties` y cambia como minimo:

- `version`: version inicial de tu mod (ejemplo `0.1.0`)
- `maven_group`: package base (ejemplo `com.tuusuario.tumod`)
- `mod_group`: grupo visible en manifest
- `mod_name`: nombre del mod
- `mod_description`: descripcion corta
- `mod_author`: autor
- `mod_website`: URL de GitHub del repo
- `mod_main_class`: clase principal completa (FQCN)
- `patchline`: `release` o `pre-release`

Nota: el JAR usa `manifest.template.json` (con placeholders) y lo resuelve en build.
Si cargas el mod como carpeta, se usa `manifest.json` (valores estaticos validos).

Si usas una instalacion custom de Hytale:

- Descomenta `hytale_home=...`
  o
- Descomenta `hytale_server_jar=...`

## Paso 3: Ajustar package/clases (recomendado)

Por defecto el template usa:

- package: `com.example.hytale.template`
- clase principal: `HyTemplatePlugin`

Si cambias package o nombre de clase:

1. Renombra los archivos en `src/main/java/...`
2. Actualiza `mod_main_class` en `gradle.properties`

## Paso 4: Compilar el JAR

En Windows:

```powershell
.\gradlew.bat clean jar
```

Si te sale `JAVA_HOME is not set`, puedes usar el runtime de Hytale:

```powershell
$env:JAVA_HOME="$env:APPDATA\Hytale\install\release\package\jre\latest"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean jar
```

Salida esperada:

- `build/libs/<nombre-del-proyecto>-<version>.jar`

## Paso 5: Probar en servidor local de Hytale

Opcion automatica:

```powershell
.\gradlew.bat deployToLocalMods
```

Opcion manual:

1. Copia el JAR de `build/libs/`
2. Pegalo en `%APPDATA%\\Hytale\\UserData\\Mods`
3. Arranca tu servidor y ejecuta `/hola`

Respuesta esperada:

- `Hola mundo desde HyTemplate!`

## Paso 6: Publicar limpio en GitHub

1. Verifica cambios:

```powershell
git status
```

2. Commit inicial:

```powershell
git add .
git commit -m "chore: init clean hytale mod template"
```

3. Push a `main`:

```powershell
git push origin main
```

## CI en GitHub Actions

Este repo incluye un workflow de verificacion minima en:

- `.github/workflows/ci.yml`

Ejecuta `gradlew help` en CI sin requerir instalacion de Hytale local (`-Pskip_hytale_checks=true`).
