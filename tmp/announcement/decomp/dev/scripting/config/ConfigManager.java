/*
 * Decompiled with CFR 0.152.
 */
package dev.scripting.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class ConfigManager {
    private static final Properties CONFIG = new Properties();
    private static File configFile;

    public static void load(File pluginDir) {
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        if (!(configFile = new File(pluginDir, "config.properties")).exists()) {
            ConfigManager.createDefault();
        }
        try (FileInputStream in = new FileInputStream(configFile);){
            CONFIG.load(new InputStreamReader((InputStream)in, StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new RuntimeException("Error loading config.properties", e);
        }
    }

    private static void createDefault() {
        CONFIG.setProperty("title", "Welcome to the server!");
        CONFIG.setProperty("subtitle", "Your Server");
        try (FileOutputStream out = new FileOutputStream(configFile);){
            CONFIG.store(new OutputStreamWriter((OutputStream)out, StandardCharsets.UTF_8), "Announcement plugin configuration - Scripting");
        }
        catch (IOException e) {
            throw new RuntimeException("Error creating config.properties", e);
        }
    }

    public static Properties get() {
        return CONFIG;
    }
}

