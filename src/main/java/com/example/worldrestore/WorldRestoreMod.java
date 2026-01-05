package com.example.worldrestore;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import org.slf4j.Logger;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WorldRestoreMod.MOD_ID)
public class WorldRestoreMod {
    public static final String MOD_ID = "worldrestore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldRestoreMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorldRestoreConfig.SPEC, "worldrestore-common.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onDedicatedServerSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onDedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        WorldRestoreService.restoreWorld(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WorldRestoreCommands.register(event.getDispatcher());
    }
}
