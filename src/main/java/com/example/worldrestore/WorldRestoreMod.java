package com.example.worldrestore;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

@Mod(WorldRestoreMod.MOD_ID)
public class WorldRestoreMod {
    public static final String MOD_ID = "worldrestore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldRestoreMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorldRestoreConfig.SPEC, "worldrestore-common.toml");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        WorldRestoreService.restoreWorld(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WorldRestoreCommands.register(event.getDispatcher());
    }
}
