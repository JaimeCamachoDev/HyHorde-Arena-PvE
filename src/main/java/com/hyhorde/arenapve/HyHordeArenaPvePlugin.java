package com.hyhorde.arenapve;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hyhorde.arenapve.commands.HordeCommand;
import com.hyhorde.arenapve.commands.HordeHelpCommand;
import com.hyhorde.arenapve.commands.HordePveCommand;
import com.hyhorde.arenapve.commands.HordeReloadCommand;
import com.hyhorde.arenapve.horde.HordeDamageTrackerSystem;
import com.hyhorde.arenapve.horde.HordeService;
import javax.annotation.Nonnull;

public class HyHordeArenaPvePlugin
extends JavaPlugin {
    private HordeService hordeService;

    public HyHordeArenaPvePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        super.setup();
        this.hordeService = new HordeService((PluginBase)this);
        this.getEntityStoreRegistry().registerSystem(new HordeDamageTrackerSystem(this.hordeService));
        this.getCommandRegistry().registerCommand((AbstractCommand)new HordeCommand("horda", "crea una horda de enemigos alrededor de ti", this.hordeService));
        this.getCommandRegistry().registerCommand((AbstractCommand)new HordePveCommand("hordapve", "controla el sistema de hordas PVE", this.hordeService));
        this.getCommandRegistry().registerCommand((AbstractCommand)new HordePveCommand("hordepve", "alias de hordapve", this.hordeService));
        this.getCommandRegistry().registerCommand((AbstractCommand)new HordeHelpCommand("hordahelp", "abre guia rapida del sistema de hordas", this.hordeService));
        this.getCommandRegistry().registerCommand((AbstractCommand)new HordeReloadCommand("hordareload", "recarga config/mod de horda", (PluginBase)this, this.hordeService));
    }

    protected void shutdown() {
        if (this.hordeService != null) {
            this.hordeService.shutdown();
        }
        super.shutdown();
    }
}


