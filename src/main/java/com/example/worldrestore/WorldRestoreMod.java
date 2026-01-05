package com.example.worldrestore;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod(WorldRestoreMod.MOD_ID)
public class WorldRestoreMod {
    public static final String MOD_ID = "worldrestore";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean PENDING_RESTORE = new AtomicBoolean(false);

    public WorldRestoreMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorldRestoreConfig.SPEC, "worldrestore-common.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onDedicatedServerSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onDedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        event.enqueueWork(() -> {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                PENDING_RESTORE.set(true);
                LOGGER.info("Dedicated server not available during setup; deferring world restore to ServerAboutToStartEvent.");
                return;
            }
            WorldRestoreService.restoreWorld(server);
        });
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!PENDING_RESTORE.compareAndSet(true, false)) {
            return;
        }
        WorldRestoreService.restoreWorld(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WorldRestoreCommands.register(event.getDispatcher());
    }
}
