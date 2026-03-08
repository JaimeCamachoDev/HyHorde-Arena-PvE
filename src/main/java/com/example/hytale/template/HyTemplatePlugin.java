package com.example.hytale.template;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class HyTemplatePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public HyTemplatePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Loading " + getName() + " v" + getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new HolaCommand());
        LOGGER.atInfo().log("Registered /hola command for " + getName());
    }
}
