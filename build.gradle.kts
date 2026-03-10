import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.os.OperatingSystem

plugins {
    java
}

group = (findProperty("maven_group") as String? ?: "com.hyhorde.arenapve")
version = (findProperty("version") as String? ?: "1.1.0")

val javaVersion = (findProperty("java_version") as String? ?: "25").toInt()
val patchline = (findProperty("patchline") as String? ?: "release")
val includesPack = (findProperty("includes_pack") as String? ?: "false").toBoolean()
val skipHytaleChecks = (findProperty("skip_hytale_checks") as String? ?: "false").toBoolean()

val modGroup = (findProperty("mod_group") as String? ?: "HyHorde")
val modName = (findProperty("mod_name") as String? ?: "ArenaPVE")
val modDescription = (findProperty("mod_description") as String? ?: "Sistema de hordas PVE para Hytale.")
val modAuthor = (findProperty("mod_author") as String? ?: "Jaime")
val modWebsite = (findProperty("mod_website") as String? ?: "https://github.com/Jaime/HyHorde-Arena-PVE")
val modMainClass = (findProperty("mod_main_class") as String? ?: "com.hyhorde.arenapve.HyHordeArenaPvePlugin")

val configuredHytaleHome = findProperty("hytale_home") as String?
val configuredServerJarPath = findProperty("hytale_server_jar") as String?

val detectedHytaleHome = configuredHytaleHome ?: run {
    val os = OperatingSystem.current()
    when {
        os.isWindows -> "${System.getProperty("user.home")}/AppData/Roaming/Hytale"
        os.isMacOsX -> "${System.getProperty("user.home")}/Library/Application Support/Hytale"
        os.isLinux -> {
            val flatpakPath = file("${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale")
            if (flatpakPath.exists()) flatpakPath.absolutePath else "${System.getProperty("user.home")}/.local/share/Hytale"
        }
        else -> null
    }
}

val resolvedServerJar = when {
    configuredServerJarPath != null -> file(configuredServerJarPath)
    detectedHytaleHome != null -> file("$detectedHytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar")
    else -> null
}

val resolvedHytaleHome = when {
    configuredHytaleHome != null -> configuredHytaleHome
    configuredServerJarPath != null && configuredServerJarPath.replace('\\', '/').contains("/install/") ->
        configuredServerJarPath.replace('\\', '/').substringBefore("/install/")
    else -> detectedHytaleHome
}

if (!skipHytaleChecks && (resolvedServerJar == null || !resolvedServerJar.exists())) {
    throw GradleException(
        "No se encontro HytaleServer.jar. Configura hytale_home o hytale_server_jar en gradle.properties."
    )
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    if (resolvedServerJar != null && resolvedServerJar.exists()) {
        compileOnly(files(resolvedServerJar))
    }
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val resourceProps = mapOf(
        "modVersion" to project.version.toString(),
        "includesPack" to includesPack.toString(),
        "modGroup" to modGroup,
        "modName" to modName,
        "modDescription" to modDescription,
        "modAuthor" to modAuthor,
        "modWebsite" to modWebsite,
        "modMainClass" to modMainClass
    )

    resourceProps.forEach { (key, value) -> inputs.property(key, value) }
    // Keep src/main/resources/manifest.json as a valid static file for folder-based loading.
    // Build outputs are generated from manifest.template.json so placeholders are always resolved.
    exclude("manifest.json")
    filesMatching("manifest.template.json") {
        expand(resourceProps)
        name = "manifest.json"
    }
}

tasks.register<Copy>("deployToLocalMods") {
    group = "distribution"
    description = "Compila y copia el jar a UserData/Mods local."
    dependsOn(tasks.jar)
    from(tasks.jar)
    doFirst {
        if (resolvedHytaleHome == null) {
            throw GradleException("No se pudo resolver hytale_home para desplegar localmente.")
        }
    }
    into(file("${resolvedHytaleHome ?: "."}/UserData/Mods"))
}

tasks.test {
    useJUnitPlatform()
}
